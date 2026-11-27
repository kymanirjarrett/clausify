package com.clausify.user;

import com.clausify.TestcontainersConfiguration;
import com.clausify.analysis.Analysis;
import com.clausify.analysis.AnalysisRepository;
import com.clausify.analysis.RiskLevel;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-12: GET/PATCH /api/users/me and GET /api/users/me/stats. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private AnalysisRepository analyses;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TransactionTemplate transactions;

    private User maria;

    @BeforeEach
    void createUser() {
        maria = newUser("Maria Lopez");
    }

    @Test
    void getReturnsDetailsWithoutThePasswordAndNoPhoneYet() throws Exception {
        as(maria, get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maria.getId()))
                .andExpect(jsonPath("$.name").value("Maria Lopez"))
                .andExpect(jsonPath("$.email").value(maria.getEmail()))
                .andExpect(jsonPath("$.phoneNumber").value(nullValue()))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("hash"))));
    }

    /** FR-12 example 2: the change is saved and shown immediately. */
    @Test
    void patchUpdatesNameAndPhoneAndTheChangeIsSaved() throws Exception {
        update(maria, """
                {"name": "  Maria L. Lopez ", "phoneNumber": "+1 513-555-0142"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria L. Lopez"))
                .andExpect(jsonPath("$.phoneNumber").value("+1 513-555-0142"));

        as(maria, get("/api/users/me"))
                .andExpect(jsonPath("$.name").value("Maria L. Lopez"))
                .andExpect(jsonPath("$.phoneNumber").value("+1 513-555-0142"));
        String storedPhone = transactions.execute(s -> users.findById(maria.getId()).orElseThrow().getProfile().getPhoneNumber());
        assertThat(storedPhone).isEqualTo("+1 513-555-0142");
    }

    @Test
    void fieldsLeftOutStayTheSameAndAnEmptyPhoneClearsIt() throws Exception {
        update(maria, """
                {"phoneNumber": "5135550142"}""").andExpect(jsonPath("$.name").value("Maria Lopez"));

        update(maria, """
                {"name": "Maria"}""").andExpect(jsonPath("$.phoneNumber").value("5135550142"));

        update(maria, """
                {"phoneNumber": ""}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria"))
                .andExpect(jsonPath("$.phoneNumber").value(nullValue()));
    }

    @Test
    void invalidValuesAre400AndChangeNothing() throws Exception {
        update(maria, """
                {"name": "   "}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name cannot be blank"));
        update(maria, """
                {"phoneNumber": "call me maybe"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phoneNumber").exists());
        update(maria, """
                {"phoneNumber": "1234567890123456"}""")
                .andExpect(status().isBadRequest());
        update(maria, "{\"name\": \"" + "a".repeat(256) + "\"}")
                .andExpect(status().isBadRequest());

        as(maria, get("/api/users/me"))
                .andExpect(jsonPath("$.name").value("Maria Lopez"))
                .andExpect(jsonPath("$.phoneNumber").value(nullValue()));
    }

    /** FR-12 example 1. 80 and 55 average to 67.5, which rounds to 68; only Maria's contracts count. */
    @Test
    void statsCountOnlyMyContractsAndAverageMyAnalyzedScores() throws Exception {
        analyzed(maria, 80);
        analyzed(maria, 55);
        contracts.save(Contract.uploaded(maria, "pending.pdf", 1000, 1, "text", T0));
        User bob = newUser("Bob");
        analyzed(bob, 10);

        as(maria, get("/api/users/me/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractsUploaded").value(3))
                .andExpect(jsonPath("$.contractsAnalyzed").value(2))
                .andExpect(jsonPath("$.averageRiskScore").value(68));
    }

    @Test
    void statsBeforeTheFirstAnalysisHaveNoAverage() throws Exception {
        as(maria, get("/api/users/me/stats"))
                .andExpect(jsonPath("$.contractsUploaded").value(0))
                .andExpect(jsonPath("$.contractsAnalyzed").value(0))
                .andExpect(jsonPath("$.averageRiskScore").value(nullValue()));
    }

    @Test
    void everyRouteNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void analyzed(User owner, int score) {
        Contract contract = Contract.uploaded(owner, "nda.pdf", 1000, 1, "text", T0);
        contract.markAnalyzing();
        contract.markAnalyzed(T0.plusSeconds(30));
        contracts.save(contract);
        analyses.save(new Analysis(null, contract, score, RiskLevel.fromScore(score), "NDA", "Summary.", "test-model",
                1000L, new ArrayList<>(), null));
    }

    private ResultActions update(User user, String body) throws Exception {
        return as(user, patch("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions as(User user, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    private User newUser(String name) {
        return users.save(User.builder().name(name).email("acct." + UUID.randomUUID() + "@example.com")
                .password("hash").build());
    }
}
