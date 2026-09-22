package com.culitostracker.application;

import com.culitostracker.api.dto.AuthDtos.AuthResponse;
import com.culitostracker.api.dto.AuthDtos.LoginRequest;
import com.culitostracker.api.dto.AuthDtos.RegisterRequest;
import com.culitostracker.api.dto.UserDtos.UserResponse;
import com.culitostracker.domain.model.RefreshToken;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.infrastructure.security.JwtProperties;
import com.culitostracker.infrastructure.security.JwtService;
import com.culitostracker.repository.RefreshTokenRepository;
import com.culitostracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTtl = Duration.ofDays(jwtProperties.refreshDays());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().toLowerCase(Locale.ROOT);
        String email = request.email().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new DomainRuleException("auth.usernameTaken", "Username already in use");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DomainRuleException("auth.emailTaken", "Email already in use");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        if (request.preferredLanguage() != null) {
            user.setPreferredLanguage(normalizeLanguage(request.preferredLanguage()));
        }
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.usernameOrEmail().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElse(null);
        // Same error for unknown user and wrong password: no account enumeration.
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new DomainRuleException("auth.invalidCredentials", "Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new DomainRuleException("auth.invalidRefreshToken", "Unknown refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new DomainRuleException("auth.invalidRefreshToken", "Expired refresh token");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new DomainRuleException("auth.invalidRefreshToken", "User no longer exists"));
        // Rotation: the used token is burned and a fresh one issued.
        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.deleteByTokenHash(sha256(rawRefreshToken));
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.issueAccessToken(user.getId());
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(sha256(rawRefresh));
        refreshToken.setExpiresAt(Instant.now().plus(refreshTtl));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(access, rawRefresh, UserResponse.from(user));
    }

    private String normalizeLanguage(String language) {
        String lang = language.toLowerCase(Locale.ROOT);
        return lang.startsWith("en") ? "en" : "es";
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
