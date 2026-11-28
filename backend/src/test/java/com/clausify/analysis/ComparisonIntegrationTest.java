package com.clausify.analysis;

import com.clausify.TestcontainersConfiguration;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-10 at the API level: POST /api/contracts/{id}/compare/{otherId}. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ComparisonIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");
    private static final String V1 = """
            Payment. Client shall pay each invoice within sixty (60) days of receipt.
            Liability. Contractor is liable for all damages without limit.
            Governing law. This Agreement is governed by the laws of Ohio.
            """;
    private static final String V2 = """
            Payment. Client shall pay each invoice within fifteen (15) days of receipt.
            Liability. Contractor's liability is capped at the fees paid.
            Governing law. This Agreement is governed by the laws of Ohio.
            Non-compete. Contractor shall not work for any competitor for three years.
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private AnalysisRepository analyses;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    private User maria;

    @BeforeEach
    void createUser() {
        maria = newUser();
    }

    /** FR-10 example 1: changes, the overall risk change (72 to 48), and which categories moved. */
    @Test
    void comparesTextAndRiskOfTwoVersions() throws Exception {
        Contract v1 = analyzed(maria, "nda-v1.pdf", V1, 72,
                finding(RiskCategory.PAYMENT_TERMS, 65), finding(RiskCategory.LIABILITY, 85));
        Contract v2 = analyzed(maria, "nda-v2.pdf", V2, 48,
                finding(RiskCategory.PAYMENT_TERMS, 20), finding(RiskCategory.NON_COMPETE, 55));

        compare(maria, v1.getId(), v2.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.original.id").value(v1.getId()))
                .andExpect(jsonPath("$.original.filename").value("nda-v1.pdf"))
                .andExpect(jsonPath("$.original.overallScore").value(72))
                .andExpect(jsonPath("$.revised.overallScore").value(48))
                .andExpect(jsonPath("$.revised.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.scoreChange").value(-24))
                .andExpect(jsonPath("$.unrelated").value(false))
                // Biggest move first: liability 85 -> none (85), non-compete none -> 55 (55), payment 65 -> 20 (45).
                .andExpect(jsonPath("$.categoryChanges[0].category").value("LIABILITY"))
                .andExpect(jsonPath("$.categoryChanges[0].before").value(85))
                .andExpect(jsonPath("$.categoryChanges[0].after").value(nullValue()))
                .andExpect(jsonPath("$.categoryChanges[1].category").value("NON_COMPETE"))
                .andExpect(jsonPath("$.categoryChanges[1].before").value(nullValue()))
                .andExpect(jsonPath("$.categoryChanges[1].after").value(55))
                .andExpect(jsonPath("$.categoryChanges[2].category").value("PAYMENT_TERMS"))
                .andExpect(jsonPath("$.counts.added").value(1))
                .andExpect(jsonPath("$.rows[*].type").value(hasItem("CHANGED")))
                .andExpect(jsonPath("$.rows[?(@.type == 'CHANGED')].revised[?(@.changed == true)].text")
                        .value(hasItem("fifteen (15)")));
    }

    /** FR-10 example 2. */
    @Test
    void unrelatedDocumentsAreFlagged() throws Exception {
        Contract nda = analyzed(maria, "nda.pdf", V1, 72);
        Contract lease = analyzed(maria, "lease.pdf", """
                This lease is made between the landlord and the tenant for the premises described below.
                The tenant shall keep the premises clean and shall not keep pets without written consent.
                """, 30);

        compare(maria, nda.getId(), lease.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unrelated").value(true));
    }

    @Test
    void bothContractsMustBeAnalyzed() throws Exception {
        Contract v1 = analyzed(maria, "nda-v1.pdf", V1, 72);
        Contract pending = contracts.save(Contract.uploaded(maria, "nda-v2.pdf", 1000, 1, V2, T0));

        compare(maria, v1.getId(), pending.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.contractId").value(pending.getId()))
                .andExpect(jsonPath("$.contractStatus").value("UPLOADED"));
    }

    @Test
    void anotherUsersContractIs404AndLeaksNothing() throws Exception {
        Contract mine = analyzed(maria, "nda-v1.pdf", V1, 72);
        Contract bobs = analyzed(newUser(), "secret.pdf", V2, 48);

        compare(maria, mine.getId(), bobs.getId())
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("Non-compete"))));
        compare(maria, bobs.getId(), mine.getId()).andExpect(status().isNotFound());
    }

    @Test
    void comparingAContractWithItselfIs400() throws Exception {
        Contract v1 = analyzed(maria, "nda-v1.pdf", V1, 72);

        compare(maria, v1.getId(), v1.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(ComparisonService.SAME_CONTRACT));
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(post("/api/contracts/1/compare/2")).andExpect(status().isUnauthorized());
    }

    private Contract analyzed(User owner, String filename, String text, int score, ClauseFinding... findings) {
        Contract contract = Contract.uploaded(owner, filename, 1000, 1, text, T0);
        contract.markAnalyzing();
        contract.markAnalyzed(T0.plusSeconds(30));
        contracts.save(contract);
        Analysis analysis = new Analysis(null, contract, score, RiskLevel.fromScore(score), "NDA", "Summary.",
                "test-model", 1000L, new ArrayList<>(), null);
        for (ClauseFinding finding : findings) {
            analysis.addFinding(finding);
        }
        analyses.save(analysis);
        return contract;
    }

    private static ClauseFinding finding(RiskCategory category, int score) {
        return new ClauseFinding(null, null, "Title", "1", category, score, RiskLevel.fromScore(score), false,
                "Original.", "Why.", "Fairer.", null);
    }

    private ResultActions compare(User user, Long id, Long otherId) throws Exception {
        return mockMvc.perform(post("/api/contracts/" + id + "/compare/" + otherId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    private User newUser() {
        return users.save(User.builder().name("Compare").email("cmp." + UUID.randomUUID() + "@example.com")
                .password("hash").build());
    }
}
