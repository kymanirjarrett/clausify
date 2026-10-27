package com.clausify.config;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E-2: the health check Render polls is public and minimal, and nothing else from actuator leaks. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActuatorHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthContributorRegistry healthContributors;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    @Test
    void healthIsPublicAndUpWithoutDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    /** A dead database connection must turn the health check DOWN, so Render restarts or reports it. */
    @Test
    void healthIncludesTheDatabase() {
        assertThat(healthContributors.getContributor("db")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator/info", "/actuator"})
    void otherActuatorEndpointsAreNotExposedEvenWhenSignedIn(String path) throws Exception {
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenForNewUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherActuatorEndpointsRequireAToken() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    /** Vercel may cache proxied responses that allow it; a cached authenticated response could reach another user. */
    @Test
    void authenticatedApiResponsesAreNotCacheable() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenForNewUser()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    private String tokenForNewUser() {
        String email = "actuator-" + UUID.randomUUID() + "@example.com";
        return jwtService.issueToken(users.save(User.builder().name("Health Check").email(email).password("hash").build()));
    }
}
