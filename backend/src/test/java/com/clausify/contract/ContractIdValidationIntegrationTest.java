package com.clausify.contract;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A contract id of zero or less is rejected with 400 at the boundary, before any lookup. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ContractIdValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void getWithNonPositiveIdIs400(String id) throws Exception {
        expectInvalidId(perform(newUser(), get("/api/contracts/" + id)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void retryWithNonPositiveIdIs400(String id) throws Exception {
        expectInvalidId(perform(newUser(), post("/api/contracts/" + id + "/retry")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void deleteWithNonPositiveIdIs400AndDeletesNothing(String id) throws Exception {
        User maria = newUser();
        Contract kept = contracts.save(Contract.uploaded(maria, "kept.pdf", 1000L, 1, "text", Instant.now()));

        expectInvalidId(perform(maria, delete("/api/contracts/" + id)));

        assertThat(contracts.findById(kept.getId())).isPresent();
    }

    private static void expectInvalidId(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.id").exists())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    private ResultActions perform(User user, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    private User newUser() {
        return users.save(User.builder().name("maria").email("maria." + UUID.randomUUID() + "@example.com").password("hash").build());
    }
}
