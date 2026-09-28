package com.culitostracker.infrastructure.security;

/** Mapped to 429 by the API error handler. */
public class RateLimitedException extends RuntimeException {

    private final String code;

    public RateLimitedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
