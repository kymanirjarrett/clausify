package com.clausify.user;

import com.clausify.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";
    private static final SecretKey KEY = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final JwtService jwtService = new JwtService(
            NimbusJwtEncoder.withSecretKey(KEY).algorithm(MacAlgorithm.HS256).build(),
            new JwtProperties(SECRET, null),
            Clock.fixed(now, ZoneOffset.UTC));

    private final User user = User.builder().id(7L).name("Maria Lopez").email("maria@example.com").password("hash").build();

    @Test
    void issuesHs256TokenWithUserClaimsAndTwentyFourHourExpiry() {
        Jwt jwt = decoder(KEY).decode(jwtService.issueToken(user));

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("maria@example.com");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("clausify");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(24)));
    }

    @Test
    void tokenSignedWithOneKeyIsRejectedByAnother() {
        String token = jwtService.issueToken(user);
        SecretKey otherKey = new SecretKeySpec("a-different-secret-also-32-bytes-long!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        assertThatThrownBy(() -> decoder(otherKey).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenNeverContainsThePassword() {
        Jwt jwt = decoder(KEY).decode(jwtService.issueToken(user));

        assertThat(jwt.getClaims()).doesNotContainKey("password");
        assertThat(jwt.getClaims().values()).doesNotContain("hash");
    }

    private static NimbusJwtDecoder decoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
