package com.clausify.analysis;

import com.clausify.TestcontainersConfiguration;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import com.clausify.contract.ContractStatus;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-6 at the API level: GET /api/contracts/{id}/analysis. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AnalysisResultIntegrationTest {

    private static final Instant UPLOADED_AT = Instant.parse("2026-10-01T09:00:00Z");
    private static final Instant ANALYZED_AT = Instant.parse("2026-10-01T09:00:42Z");

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
    private User bob;

    @BeforeEach
    void createUsers() {
        maria = newUser("maria");
        bob = newUser("bob");
    }

    /** DESIGN.md 7.6 example: 85, 78, 55 risky and 20 favorable give 80, High. */
    @Test
    void analyzedContractReturnsScoreCategoriesDocumentAndFindingsRiskiestFirst() throws Exception {
        Contract contract = analyzed(maria,
                finding(RiskCategory.LIABILITY, 55, false),
                finding(RiskCategory.PAYMENT_TERMS, 78, false),
                finding(RiskCategory.LIABILITY, 85, false),
                finding(RiskCategory.CONFIDENTIALITY, 20, true));

        as(maria, contract.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractId").value(contract.getId()))
                .andExpect(jsonPath("$.overallScore").value(80))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.contractType").value("Consulting Agreement"))
                .andExpect(jsonPath("$.summary").value("Risk concentrated in liability."))
                .andExpect(jsonPath("$.document.filename").value("consulting.pdf"))
                .andExpect(jsonPath("$.document.pageCount").value(2))
                .andExpect(jsonPath("$.document.sizeBytes").value(4096))
                .andExpect(jsonPath("$.document.uploadedAt").value("2026-10-01T09:00:00Z"))
                .andExpect(jsonPath("$.document.analyzedAt").value("2026-10-01T09:00:42Z"))
                .andExpect(jsonPath("$.document.analysisMs").value(19400))
                .andExpect(jsonPath("$.categories[*].category").value(contains("LIABILITY", "PAYMENT_TERMS")))
                .andExpect(jsonPath("$.categories[0].highestScore").value(85))
                .andExpect(jsonPath("$.categories[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.categories[0].findingCount").value(2))
                .andExpect(jsonPath("$.categories[1].findingCount").value(1))
                .andExpect(jsonPath("$.findings[*].riskScore").value(contains(85, 78, 55)))
                .andExpect(jsonPath("$.findings[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.findings[2].riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.findings[0].title").value("LIABILITY clause"))
                .andExpect(jsonPath("$.findings[0].sectionRef").value("7.2"))
                .andExpect(jsonPath("$.findings[0].originalText").value("Original clause."))
                .andExpect(jsonPath("$.findings[0].explanation").value("Why it matters."))
                .andExpect(jsonPath("$.findings[0].suggestedRevision").value("Fairer wording."))
                .andExpect(jsonPath("$.favorable", hasSize(1)))
                .andExpect(jsonPath("$.favorable[0].category").value("CONFIDENTIALITY"))
                .andExpect(content().string(not(containsString("embedding"))))
                .andExpect(content().string(not(containsString("extractedText"))))
                .andExpect(content().string(not(containsString("Full contract text"))));
    }

    @Test
    void anAnalysisWithNoFindingsHasEmptyLists() throws Exception {
        Contract contract = analyzed(maria);

        as(maria, contract.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(0)))
                .andExpect(jsonPath("$.findings", hasSize(0)))
                .andExpect(jsonPath("$.favorable", hasSize(0)));
    }

    /** FR-6 example 4: the client keeps showing progress instead of an error. */
    @ParameterizedTest
    @EnumSource(value = ContractStatus.class, names = {"UPLOADED", "ANALYZING", "FAILED"})
    void notAnalyzedYetIs409WithTheCurrentStatus(ContractStatus state) throws Exception {
        Contract contract = Contract.uploaded(maria, "nda.pdf", 1000, 1, "text", UPLOADED_AT);
        if (state != ContractStatus.UPLOADED) {
            contract.markAnalyzing();
        }
        if (state == ContractStatus.FAILED) {
            contract.markFailed("The AI service is unavailable right now. Please retry later.");
        }
        contracts.save(contract);

        as(maria, contract.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.contractStatus").value(state.name()))
                .andExpect(jsonPath("$.detail").value("The analysis is not ready. The contract is " + state + "."));
    }

    @Test
    void anotherUsersContractIs404LikeAMissingOne() throws Exception {
        Contract marias = analyzed(maria, finding(RiskCategory.LIABILITY, 85, false));

        as(bob, marias.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Contract not found."))
                .andExpect(content().string(not(containsString("LIABILITY"))));
        as(bob, Long.MAX_VALUE).andExpect(status().isNotFound());
    }

    @Test
    void requiresATokenAndAPositiveId() throws Exception {
        mockMvc.perform(get("/api/contracts/1/analysis")).andExpect(status().isUnauthorized());
        as(maria, 0L).andExpect(status().isBadRequest());
    }

    private Contract analyzed(User owner, ClauseFinding... findings) {
        Contract contract = Contract.uploaded(owner, "consulting.pdf", 4096, 2, "Full contract text", UPLOADED_AT);
        contract.markAnalyzing();
        contract.markAnalyzed(ANALYZED_AT);
        contracts.save(contract);
        Analysis analysis = new Analysis(null, contract, 80, RiskLevel.HIGH, "Consulting Agreement",
                "Risk concentrated in liability.", "test-model", 19400L, new ArrayList<>(), null);
        for (ClauseFinding finding : findings) {
            analysis.addFinding(finding);
        }
        analyses.save(analysis);
        return contract;
    }

    private static ClauseFinding finding(RiskCategory category, int score, boolean favorable) {
        return new ClauseFinding(null, null, category + " clause", "7.2", category, score, RiskLevel.fromScore(score),
                favorable, "Original clause.", "Why it matters.", "Fairer wording.", null);
    }

    private ResultActions as(User user, Long contractId) throws Exception {
        return mockMvc.perform(get("/api/contracts/" + contractId + "/analysis")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    private User newUser(String name) {
        return users.save(User.builder().name(name).email(name + "." + UUID.randomUUID() + "@example.com")
                .password("hash").build());
    }
}
