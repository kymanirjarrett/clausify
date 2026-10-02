package com.clausify.user;

import com.clausify.common.ConflictException;
import com.clausify.common.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    static final String DUPLICATE_EMAIL = "An account with this email already exists.";
    static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Checked against when the email is unknown, so every failed login costs one BCrypt comparison. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    /** Creates the account (FR-1) and signs the user in by returning a token. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(DUPLICATE_EMAIL);
        }
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .build();
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two registrations for the same email at once: the unique constraint (V4) catches the second.
            throw new ConflictException(DUPLICATE_EMAIL);
        }
        return new AuthResponse(jwtService.issueToken(user), UserResponse.from(user));
    }

    /**
     * Signs a user in (FR-2). Unknown email and wrong password fail the same way, with the same message
     * and the same BCrypt cost, so neither the response nor its timing reveals which emails are registered.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(request.email()));
        boolean passwordMatches = passwordMatches(request.password(), user.map(User::getPassword).orElse(dummyHash));
        if (user.isEmpty() || !passwordMatches) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        return new AuthResponse(jwtService.issueToken(user.get()), UserResponse.from(user.get()));
    }

    /** The signed-in user (GET /api/auth/me). 404 if the account was deleted after the token was issued. */
    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private boolean passwordMatches(String rawPassword, String hash) {
        try {
            return passwordEncoder.matches(rawPassword, hash);
        } catch (IllegalArgumentException ex) {
            // BCrypt rejects input over 72 bytes; such a password can never match a stored one.
            return false;
        }
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
