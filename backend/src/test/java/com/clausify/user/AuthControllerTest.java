package com.clausify.user;

import com.clausify.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web layer only: request validation and status codes. Security rules are covered by SecurityConfigTest. */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String WEAK_PASSWORD_MESSAGE = "Password must be at least 8 characters and include a letter and a number";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerReturns201WithTokenAndUser() throws Exception {
        when(authService.register(any())).thenReturn(
                new AuthResponse("jwt-token", new UserResponse(1L, "Maria Lopez", "maria@example.com")));

        register("Maria Lopez", "maria@example.com", "Str0ng!Passw0rd")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/auth/me"))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("maria@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void weakPasswordIs400WithFieldMessage() throws Exception {
        register("Maria Lopez", "maria@example.com", "abc123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(WEAK_PASSWORD_MESSAGE));
        verifyNoInteractions(authService);
    }

    @Test
    void passwordWithoutNumberIs400() throws Exception {
        register("Maria Lopez", "maria@example.com", "OnlyLettersHere")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(WEAK_PASSWORD_MESSAGE));
    }

    @Test
    void passwordLongerThanBcryptLimitIs400() throws Exception {
        register("Maria Lopez", "maria@example.com", "a1".repeat(37))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void invalidEmailAndMissingNameAre400() throws Exception {
        register("", "not-an-email", "Str0ng!Passw0rd")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email must be a valid address"));
        verifyNoInteractions(authService);
    }

    @Test
    void duplicateEmailIs409() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("An account with this email already exists."));

        register("Maria Lopez", "maria@example.com", "Str0ng!Passw0rd")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with this email already exists."));
    }

    private ResultActions register(String name, String email, String password) throws Exception {
        String body = """
                {"name": "%s", "email": "%s", "password": "%s"}""".formatted(name, email, password);
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
