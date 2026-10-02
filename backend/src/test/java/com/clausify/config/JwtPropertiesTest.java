package com.clausify.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JwtPropertiesTest {

    @Test
    void rejectsMissingSecret() {
        assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(null, null))
                .withMessageContaining("at least 32 bytes");
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {
        assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties("x".repeat(31), null));
    }

    @Test
    void accepts32ByteSecretAndDefaultsExpiryTo24Hours() {
        JwtProperties properties = new JwtProperties("x".repeat(32), null);

        assertThat(properties.expiry()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void toStringHidesTheSecret() {
        assertThat(new JwtProperties("s".repeat(40), null).toString()).doesNotContain("ssss");
    }
}
