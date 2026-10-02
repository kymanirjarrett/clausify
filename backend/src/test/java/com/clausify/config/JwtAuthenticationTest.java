package com.clausify.config;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends real bearer tokens through the security filter chain. Until FR-3 adds the contracts controller,
 * an authenticated GET /api/contracts reaches Spring MVC and returns 404, which proves the token was
 * accepted; a rejected token stops at the filter with 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JwtAuthenticationTest {

    private static final String PROTECTED = "/api/contracts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    private final User user = User.builder().id(42L).name("Maria").email("maria@example.com").password("hash").build();

    @Test
    void validTokenIsAccepted() throws Exception {
        withToken(jwtService.issueToken(user)).andExpect(status().isNotFound());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        Instant past = Instant.now().minus(Duration.ofHours(25));
        expect401(withToken(sign(JwtService.ISSUER, past, past.plus(Duration.ofHours(24)))));
    }

    @Test
    void tamperedTokenIs401() throws Exception {
        String token = jwtService.issueToken(user);
        // Change the payload (the middle part) without re-signing it.
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "AA." + parts[2];
        expect401(withToken(tampered));
    }

    @Test
    void tokenFromAnotherIssuerIs401() throws Exception {
        Instant now = Instant.now();
        expect401(withToken(sign("someone-else", now, now.plus(Duration.ofHours(1)))));
    }

    @Test
    void tokenSignedWithAnotherKeyIs401() throws Exception {
        JwtEncoder otherEncoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(
                "an-attackers-secret-that-is-32-bytes-plus".getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256).build();
        Instant now = Instant.now();
        String forged = otherEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims(JwtService.ISSUER, now, now.plus(Duration.ofHours(1))))).getTokenValue();
        expect401(withToken(forged));
    }

    @Test
    void garbageTokenIs401() throws Exception {
        expect401(withToken("not-a-jwt"));
    }

    private ResultActions withToken(String token) throws Exception {
        return mockMvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static void expect401(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    private String sign(String issuer, Instant issuedAt, Instant expiresAt) {
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims(issuer, issuedAt, expiresAt))).getTokenValue();
    }

    private static JwtClaimsSet claims(String issuer, Instant issuedAt, Instant expiresAt) {
        return JwtClaimsSet.builder().issuer(issuer).subject("42").claim("email", "maria@example.com")
                .issuedAt(issuedAt).expiresAt(expiresAt).build();
    }
}
