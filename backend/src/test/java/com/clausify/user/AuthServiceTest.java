package com.clausify.user;

import com.clausify.common.ConflictException;
import com.clausify.common.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
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

    private AuthService authService;

    private final RegisterRequest request = new RegisterRequest("  Maria Lopez ", " Maria@Example.COM ", "Str0ng!Passw0rd");

    private final User maria = User.builder().id(1L).name("Maria Lopez").email("maria@example.com").password("bcrypt-hash").build();

    @BeforeEach
    void createService() {
        // The constructor hashes a dummy password once, used to equalize failed-login timing.
        when(passwordEncoder.encode(startsWith("timing-equalizer"))).thenReturn("dummy-hash");
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

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
    void loginReturnsTokenForCorrectPassword() {
        when(userRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(maria));
        when(passwordEncoder.matches("Str0ng!Passw0rd", "bcrypt-hash")).thenReturn(true);
        when(jwtService.issueToken(maria)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest(" MARIA@example.com ", "Str0ng!Passw0rd"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("maria@example.com");
    }

    @Test
    void loginWithWrongPasswordFailsWithGenericMessage() {
        when(userRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(maria));
        when(passwordEncoder.matches("wrong-password1", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@example.com", "wrong-password1")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
        verify(jwtService, never()).issueToken(any());
    }

    @Test
    void loginWithUnknownEmailStillRunsBcryptAndFailsWithSameMessage() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "Str0ng!Passw0rd")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
        verify(passwordEncoder).matches(eq("Str0ng!Passw0rd"), eq("dummy-hash"));
    }

    @Test
    void loginWithPasswordOverBcryptLimitFailsAsInvalidCredentials() {
        String overlong = "a1".repeat(40);
        when(userRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(maria));
        when(passwordEncoder.matches(overlong, "bcrypt-hash")).thenThrow(new IllegalArgumentException("password too long"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@example.com", overlong)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void currentUserThatNoLongerExistsIsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.currentUser(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void requestToStringHidesPassword() {
        assertThat(request.toString()).doesNotContain("Str0ng!Passw0rd");
        assertThat(new LoginRequest("maria@example.com", "Str0ng!Passw0rd").toString()).doesNotContain("Str0ng!Passw0rd");
    }
}
