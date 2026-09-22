package com.culitostracker.application;

import com.culitostracker.domain.model.PasswordResetToken;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.infrastructure.mail.ResetLinkSender;
import com.culitostracker.repository.PasswordResetTokenRepository;
import com.culitostracker.repository.RefreshTokenRepository;
import com.culitostracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    /** A fresh token per address at most this often: keeps mail/logs unspammable. */
    static final Duration REQUEST_THROTTLE = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResetLinkSender resetLinkSender;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                PasswordEncoder passwordEncoder,
                                ResetLinkSender resetLinkSender,
                                @Value("${app.frontend-url:}") String frontendUrl,
                                @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.resetLinkSender = resetLinkSender;
        // The reset link must point at the SPA; the first CORS origin already is it.
        this.frontendUrl = !frontendUrl.isBlank()
                ? frontendUrl
                : allowedOrigins.split(",")[0].trim();
    }

    /**
     * Always succeeds from the caller's point of view (the API returns 204
     * regardless), so this endpoint can't be used to enumerate accounts.
     * Returns the raw token for tests; production callers ignore it.
     */
    @Transactional
    public Optional<String> requestReset(String email) {
        User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        if (tokenRepository.existsByUserIdAndCreatedAtAfter(
                user.getId(), Instant.now().minus(REQUEST_THROTTLE))) {
            return Optional.empty();
        }
        // One live token per account: a new request invalidates older links.
        tokenRepository.deleteByUserId(user.getId());

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(AuthService.sha256(rawToken));
        token.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        tokenRepository.save(token);

        resetLinkSender.send(user.getEmail(), user.getPreferredLanguage(),
                frontendUrl + "/reset-password?token=" + rawToken);
        return Optional.of(rawToken);
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(AuthService.sha256(rawToken))
                .orElseThrow(PasswordResetService::invalidToken);
        if (token.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw invalidToken();
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(PasswordResetService::invalidToken);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId()); // single use
        refreshTokenRepository.deleteByUserId(user.getId()); // log out every session
    }

    private static DomainRuleException invalidToken() {
        return new DomainRuleException("auth.invalidResetToken",
                "Invalid or expired reset token");
    }
}
