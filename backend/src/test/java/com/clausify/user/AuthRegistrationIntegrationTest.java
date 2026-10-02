package com.clausify.user;

import com.clausify.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full register flow through security, validation, service, and real MySQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersUserWithHashedPasswordAndLowercasedEmail() throws Exception {
        register("Register.Flow@Example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("register.flow@example.com"));

        User stored = userRepository.findByEmail("register.flow@example.com").orElseThrow();
        assertThat(stored.getPassword()).startsWith("$2a$10$").isNotEqualTo("Str0ng!Passw0rd");
        assertThat(passwordEncoder.matches("Str0ng!Passw0rd", stored.getPassword())).isTrue();
        assertThat(stored.getCreatedAt()).isNotNull();
    }

    @Test
    void secondRegistrationWithSameEmailIs409() throws Exception {
        register("duplicate@example.com").andExpect(status().isCreated());

        register("DUPLICATE@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with this email already exists."));
    }

    private ResultActions register(String email) throws Exception {
        String body = """
                {"name": "Maria Lopez", "email": "%s", "password": "Str0ng!Passw0rd"}""".formatted(email);
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
