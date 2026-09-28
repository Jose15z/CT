package com.culitostracker.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window brute-force guard for login, keyed by the identifier the
 * caller typed. In-memory on purpose: the app runs as a single instance and
 * a restart simply forgives everyone. Successful logins clear the window.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 10;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public void check(String identifier) {
        Deque<Instant> window = failures.get(key(identifier));
        if (window == null) {
            return;
        }
        synchronized (window) {
            prune(window, Instant.now());
            if (window.size() >= MAX_FAILURES) {
                throw new RateLimitedException("auth.tooManyAttempts",
                        "Too many failed logins; try again later");
            }
        }
    }

    public void recordFailure(String identifier) {
        Deque<Instant> window = failures.computeIfAbsent(key(identifier), k -> new ArrayDeque<>());
        synchronized (window) {
            Instant now = Instant.now();
            prune(window, now);
            window.addLast(now);
        }
    }

    public void recordSuccess(String identifier) {
        failures.remove(key(identifier));
    }

    private static void prune(Deque<Instant> window, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
            window.pollFirst();
        }
    }

    private static String key(String identifier) {
        return identifier.trim().toLowerCase(Locale.ROOT);
    }
}
