package com.culitostracker.domain.service;

/**
 * A business rule violation. The code is a stable identifier the API maps to
 * HTTP 422 and the frontend translates.
 */
public class DomainRuleException extends RuntimeException {

    private final String code;

    public DomainRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
