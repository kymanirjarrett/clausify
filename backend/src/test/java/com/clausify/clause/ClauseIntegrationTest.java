package com.clausify.clause;

import com.clausify.TestcontainersConfiguration;
import com.clausify.analysis.Analysis;
import com.clausify.analysis.AnalysisRepository;
import com.clausify.analysis.ClauseFinding;
import com.clausify.analysis.RiskCategory;
import com.clausify.analysis.RiskLevel;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-9: GET /api/clauses/{findingId}/similar and GET /api/standard-clauses, with the hashing embedder. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClauseIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");

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

    @Autowired
    private StandardClauseLoader loader;

    @Autowired
    private StandardClauseRepository clauses;

    private User maria;

    @BeforeEach
    void createUser() {
        maria = newUser();
    }

    /** FR-9 example 1: a worldwide non-compete is matched with the library's worldwide non-compete first. */
    @Test
    void similarReturnsTheThreeClosestStandardClausesInTheSameCategory() throws Exception {
        Long findingId = finding(maria, RiskCategory.NON_COMPETE,
                "For three years after termination, Contractor shall not provide services to any business anywhere "
                        + "in the world that competes with Client.");

        as(maria, "/api/clauses/" + findingId + "/similar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finding.id").value(findingId))
                .andExpect(jsonPath("$.finding.category").value("NON_COMPETE"))
                .andExpect(jsonPath("$.matches", hasSize(3)))
                .andExpect(jsonPath("$.matches[*].clause.category", everyItem(is("NON_COMPETE"))))
                .andExpect(jsonPath("$.matches[0].clause.title").value("Worldwide multi-year non-compete"))
                .andExpect(jsonPath("$.matches[0].clause.favorable").value(false))
                .andExpect(content().string(not(containsString("embedding"))))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    List<Double> scores = com.jayway.jsonpath.JsonPath.read(body, "$.matches[*].similarity");
                    assertThat(scores).isSortedAccordingTo((a, b) -> Double.compare(b, a))
                            .allSatisfy(s -> assertThat(s).isBetween(0.0, 1.0));
                });
    }

    @Test
    void anotherUsersFindingIs404LikeAMissingOne() throws Exception {
        Long bobsFinding = finding(newUser(), RiskCategory.LIABILITY, "Contractor shall be liable for all damages.");

        as(maria, "/api/clauses/" + bobsFinding + "/similar").andExpect(status().isNotFound());
        as(maria, "/api/clauses/" + Long.MAX_VALUE + "/similar").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/clauses/" + bobsFinding + "/similar")).andExpect(status().isUnauthorized());
    }

    /** FR-9 example 2: filtering by category shows only that category, with fair and unfair examples. */
    @Test
    void libraryFiltersByCategory() throws Exception {
        as(maria, "/api/standard-clauses?category=IP_RIGHTS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(13)))
                .andExpect(jsonPath("$[*].category", everyItem(is("IP_RIGHTS"))))
                .andExpect(jsonPath("$[*].favorable", hasItem(true)))
                .andExpect(jsonPath("$[*].favorable", hasItem(false)))
                .andExpect(content().string(not(containsString("embedding"))));
        as(maria, "/api/standard-clauses").andExpect(jsonPath("$", hasSize(104)));
        as(maria, "/api/standard-clauses?category=NOT_A_CATEGORY").andExpect(status().isBadRequest());
    }

    /** A restart re-embeds nothing; an edited clause or a different model is embedded again. */
    @Test
    void syncEmbedsOnlyWhatChanged() {
        assertThat(loader.sync()).isZero();

        StandardClause clause = clauses.findAll().getFirst();
        clause.setEmbeddingModel("an-older-model");
        clauses.save(clause);

        assertThat(loader.sync()).isEqualTo(1);
        assertThat(clauses.findAll()).allSatisfy(c -> {
            assertThat(c.getEmbedding()).isNotNull();
            assertThat(c.getEmbeddingModel()).isEqualTo("hashing-384");
        });
    }

    @Test
    void cosineIsOneForTheSameDirectionAndZeroForOrthogonalOrEmpty() {
        assertThat(ClauseEmbedder.cosine(new float[] {1, 2, 3}, new float[] {2, 4, 6})).isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(ClauseEmbedder.cosine(new float[] {1, 0}, new float[] {0, 1})).isZero();
        assertThat(ClauseEmbedder.cosine(new float[] {0, 0}, new float[] {1, 1})).isZero();
    }

    private Long finding(User owner, RiskCategory category, String text) {
        Contract contract = Contract.uploaded(owner, "nda.pdf", 1000, 1, "text", T0);
        contract.markAnalyzing();
        contract.markAnalyzed(T0.plusSeconds(30));
        contracts.save(contract);
        Analysis analysis = new Analysis(null, contract, 85, RiskLevel.HIGH, "NDA", "Summary.", "test-model", 1000L,
                new ArrayList<>(), null);
        analysis.addFinding(new ClauseFinding(null, null, "Clause", "6.1", category, 85, RiskLevel.HIGH, false, text,
                "Why.", "Fairer.", null));
        return analyses.save(analysis).getFindings().getFirst().getId();
    }

    private ResultActions as(User user, String url) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    private User newUser() {
        return users.save(User.builder().name("Clause").email("cl." + UUID.randomUUID() + "@example.com")
                .password("hash").build());
    }
}
