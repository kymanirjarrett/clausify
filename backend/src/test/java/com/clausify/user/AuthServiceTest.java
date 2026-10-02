package com.clausify.user;

import com.clausify.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private final RegisterRequest request = new RegisterRequest("  Maria Lopez ", " Maria@Example.COM ", "Str0ng!Passw0rd");

    @Test
    void registerNormalizesEmailHashesPasswordAndReturnsToken() {
        when(userRepository.existsByEmail("maria@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Passw0rd")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.issueToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("maria@example.com");
        assertThat(saved.getValue().getName()).isEqualTo("Maria Lopez");
        assertThat(saved.getValue().getPassword()).isEqualTo("bcrypt-hash");
        assertThat(response).isEqualTo(new AuthResponse("jwt-token", new UserResponse(1L, "Maria Lopez", "maria@example.com")));
    }

    @Test
    void registerRejectsExistingEmailWithoutSaving() {
        when(userRepository.existsByEmail("maria@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An account with this email already exists.");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerTurnsConcurrentDuplicateIntoConflict() {
        when(userRepository.existsByEmail("maria@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uk_users_email"));

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void requestToStringHidesPassword() {
        assertThat(request.toString()).doesNotContain("Str0ng!Passw0rd");
    }
}
