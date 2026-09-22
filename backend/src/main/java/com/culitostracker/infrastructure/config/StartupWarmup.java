package com.culitostracker.infrastructure.config;

import com.culitostracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Exercises the hottest request paths once at boot (BCrypt hashing and a JDBC
 * round trip) so the JIT compiles them before the first real user arrives.
 * On tiny CPU shares this cuts first-login latency from many seconds to ~1s.
 */
@Component
public class StartupWarmup {

    private static final Logger log = LoggerFactory.getLogger(StartupWarmup.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public StartupWarmup(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        long start = System.currentTimeMillis();
        String hash = passwordEncoder.encode("warmup-only");
        passwordEncoder.matches("warmup-only", hash);
        userRepository.count();
        log.info("Warmup completed in {} ms", System.currentTimeMillis() - start);
    }
}
