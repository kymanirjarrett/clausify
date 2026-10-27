package com.clausify.config;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** #80: the configured limits are enforced on the real endpoints, per IP, per user, and per email. */
@SpringBootTest(properties = {
        "clausify.rate-limit.enabled=true",
        "clausify.rate-limit.login.capacity=3",
        "clausify.rate-limit.login.period=1h",
        "clausify.rate-limit.register.capacity=2",
        "clausify.rate-limit.register.period=1h",
        "clausify.rate-limit.login-failures-per-email.capacity=2",
        "clausify.rate-limit.login-failures-per-email.period=1h",
        "clausify.rate-limit.analysis.capacity=2",
        "clausify.rate-limit.analysis.period=1h"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RateLimitIntegrationTest {

    private static final String PASSWORD = "correct-horse-9";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void loginIsLimitedPerIpWith429ProblemDetailAndRetryAfter() throws Exception {
        String ip = uniqueIp();
        for (int i = 0; i < 3; i++) {
            login(ip, "nobody-" + i + "@example.com", "wrong").andExpect(status().isUnauthorized());
        }

        login(ip, "nobody-4@example.com", "wrong")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").isNotEmpty());

        login(uniqueIp(), "nobody-5@example.com", "wrong").andExpect(status().isUnauthorized());
    }

    /** Rotating IPs doesn't help guess one account's password. */
    @Test
    void failedLoginsAreLimitedPerEmailFromAnyIp() throws Exception {
        String email = newUser().getEmail();
        login(uniqueIp(), email, "wrong-1").andExpect(status().isUnauthorized());
        login(uniqueIp(), email, "wrong-2").andExpect(status().isUnauthorized());

        login(uniqueIp(), email, PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void successfulLoginsDoNotUseTheFailureBudget() throws Exception {
        String email = newUser().getEmail();
        for (int i = 0; i < 3; i++) {
            login(uniqueIp(), email, PASSWORD).andExpect(status().isOk());
        }
    }

    @Test
    void registerIsLimitedPerIp() throws Exception {
        String ip = uniqueIp();
        register(ip).andExpect(status().isCreated());
        register(ip).andExpect(status().isCreated());

        register(ip).andExpect(status().isTooManyRequests());
    }

    @Test
    void uploadsAreLimitedPerUserNotShared() throws Exception {
        User maria = newUser();
        User bob = newUser();
        upload(maria).andExpect(status().is(not(429)));
        upload(maria).andExpect(status().is(not(429)));

        upload(maria).andExpect(status().isTooManyRequests());
        upload(bob).andExpect(status().is(not(429)));
    }

    private ResultActions login(String ip, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions register(String ip) throws Exception {
        String email = "rate-" + UUID.randomUUID() + "@example.com";
        return mockMvc.perform(post("/api/auth/register")
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Rate Test\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"));
    }

    /** A non-PDF still counts: the limit is checked before the upload is processed. */
    private ResultActions upload(User user) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "not a pdf".getBytes());
        return mockMvc.perform(multipart("/api/contracts").file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    private User newUser() {
        String email = "rate-" + UUID.randomUUID() + "@example.com";
        return users.save(User.builder().name("Rate Test").email(email).password(passwordEncoder.encode(PASSWORD)).build());
    }

    private static String uniqueIp() {
        UUID id = UUID.randomUUID();
        return "10." + (id.hashCode() & 0xff) + "." + ((id.hashCode() >> 8) & 0xff) + "." + ((id.hashCode() >> 16) & 0xff);
    }
}
