package com.culitostracker.api;

import org.springframework.security.core.Authentication;

import java.util.UUID;

/** The JWT filter stores the authenticated user's UUID as the principal. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
