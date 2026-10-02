package com.clausify.config;

import com.clausify.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityConfigTest {

    private static final String ANGULAR_DEV = "http://localhost:4200";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void protectedEndpointWithoutTokenIs401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/contracts"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication is required."));
    }

    @Test
    void registerAndLoginArePublic() throws Exception {
        mockMvc.perform(post("/api/auth/register")).andExpect(status().is(not(401)));
        mockMvc.perform(post("/api/auth/login")).andExpect(status().is(not(401)));
    }

    @Test
    void noSessionCookieIsCreated() throws Exception {
        mockMvc.perform(get("/api/contracts")).andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void corsPreflightFromAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, ANGULAR_DEV)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_DEV));
    }

    @Test
    void corsPreflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void passwordsAreHashedWithBcrypt() {
        String hash = passwordEncoder.encode("Str0ng!Passw0rd");

        assertThat(hash).startsWith("$2a$10$");
        assertThat(passwordEncoder.matches("Str0ng!Passw0rd", hash)).isTrue();
        assertThat(passwordEncoder.matches("wrong", hash)).isFalse();
    }
}
