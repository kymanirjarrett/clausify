package com.clausify.contract;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-8: deleting contracts, and that users can only ever delete their own. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ContractDeleteIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    private User maria;
    private User bob;

    @BeforeEach
    void createUsers() {
        maria = newUser("maria");
        bob = newUser("bob");
    }

    @Test
    void deletesOwnContract() throws Exception {
        Long id = contracts.save(Contract.uploaded(maria, "nda.pdf", 1, 1, "text", T0)).getId();

        deleteAs(maria, "/api/contracts/" + id)
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(contracts.existsById(id)).isFalse();
        mockMvc.perform(get("/api/contracts/" + id).header(HttpHeaders.AUTHORIZATION, bearer(maria)))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForAnotherUsersContract() throws Exception {
        Long mariasContract = contracts.save(Contract.uploaded(maria, "private.pdf", 1, 1, "text", T0)).getId();

        deleteAs(bob, "/api/contracts/" + mariasContract)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Contract not found."));

        assertThat(contracts.existsById(mariasContract)).isTrue();
    }

    @Test
    void returnsNotFoundForMissingContract() throws Exception {
        Long id = contracts.save(Contract.uploaded(maria, "nda.pdf", 1, 1, "text", T0)).getId();

        deleteAs(maria, "/api/contracts/" + (id + 1_000_000))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Contract not found."));
    }

    @Test
    void requiresAToken() throws Exception {
        Long id = contracts.save(Contract.uploaded(maria, "nda.pdf", 1, 1, "text", T0)).getId();

        mockMvc.perform(delete("/api/contracts/" + id)).andExpect(status().isUnauthorized());

        assertThat(contracts.existsById(id)).isTrue();
    }

    /** FR-8 example 1: the analysis and its findings go with the contract. Columns follow the V6 spec in #34. */
    @Disabled("Enable once V6 (#34) adds analyses.contract_id -> contracts(id) ON DELETE CASCADE")
    @Test
    void deletingAContractAlsoDeletesItsAnalysisAndFindings() throws Exception {
        Long id = contracts.save(Contract.uploaded(maria, "nda.pdf", 1, 1, "text", T0)).getId();
        jdbc.update("insert into analyses (contract_id, overall_score, risk_level, contract_type, summary, model_name, "
                + "duration_ms, created_at) values (?, 72, 'HIGH', 'NDA', 'Summary', 'test-model', 1000, ?)", id, Timestamp.from(T0));
        Long analysisId = jdbc.queryForObject("select id from analyses where contract_id = ?", Long.class, id);
        jdbc.update("insert into clause_findings (analysis_id, category, risk_score, risk_level, favorable, title, "
                + "section_ref, original_text, explanation, suggested_revision) values (?, 'PAYMENT_TERMS', 80, 'HIGH', "
                + "false, 'Late payment', '4.2', 'Net 120', 'Payment is very late', 'Net 30')", analysisId);

        deleteAs(maria, "/api/contracts/" + id).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from analyses where contract_id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from clause_findings where analysis_id = ?", Integer.class, analysisId)).isZero();
    }

    private ResultActions deleteAs(User user, String url) throws Exception {
        return mockMvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, bearer(user)));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.issueToken(user);
    }

    private User newUser(String name) {
        return users.save(User.builder().name(name).email(name + "." + UUID.randomUUID() + "@example.com").password("hash").build());
    }
}
