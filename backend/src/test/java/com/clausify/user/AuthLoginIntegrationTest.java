package com.clausify.user;

import com.clausify.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-2 end to end: login issues a token that GET /api/auth/me accepts; bad credentials and bad tokens are 401. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthLoginIntegrationTest {

    private static final String EMAIL = "login.flow@example.com";
    private static final String PASSWORD = "Str0ng!Passw0rd";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    private User user;

    @BeforeEach
    void createUser() {
        user = userRepository.findByEmail(EMAIL).orElseGet(() -> userRepository.save(
                User.builder().name("Login Flow").email(EMAIL).password(passwordEncoder.encode(PASSWORD)).build()));
    }

    @Test
    void loginTokenWorksOnMe() throws Exception {
        String response = login(EMAIL.toUpperCase(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Login Flow"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSame401() throws Exception {
        login(EMAIL, "Wrong-passw0rd")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
        login("nobody@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void meWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meWithExpiredTokenIs401() throws Exception {
        Instant issued = Instant.now().minus(Duration.ofHours(25));
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(JwtService.ISSUER).subject(String.valueOf(user.getId()))
                .claim("email", EMAIL).issuedAt(issued).expiresAt(issued.plus(Duration.ofHours(24))).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    /** BCrypt's limit is 72 bytes: 35 two-byte letters plus "a1" is exactly 72, and must survive JSON decoding and hashing. */
    @Test
    void multibytePasswordAt72BytesRegistersAndLogsIn() throws Exception {
        String email = "bytes72." + java.util.UUID.randomUUID() + "@example.com";
        String password = "é".repeat(35) + "a1";

        fromOwnIp(post("/api/auth/register"), """
                {"name": "Byte Limit", "email": "%s", "password": "%s"}""".formatted(email, password))
                .andExpect(status().isCreated());
        fromOwnIp(post("/api/auth/login"), """
                {"email": "%s", "password": "%s"}""".formatted(email, password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        assertThat(passwordEncoder.matches(password, userRepository.findByEmail(email).orElseThrow().getPassword()))
                .isTrue();
    }

    /** The real encoder rejects input over 72 bytes; login must still answer with the generic 401, not 500. */
    @Test
    void loginWithMultibytePasswordOver72BytesIs401() throws Exception {
        fromOwnIp(post("/api/auth/login"), """
                {"email": "%s", "password": "%s"}""".formatted(EMAIL, "é".repeat(37)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."))
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    /** A fresh client IP per call, so these requests never share the per-IP login bucket with other tests. */
    private ResultActions fromOwnIp(MockHttpServletRequestBuilder request, String body) throws Exception {
        String ip = "10.120." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250);
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body)
                .with(r -> { r.setRemoteAddr(ip); return r; }));
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}""".formatted(email, password);
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
