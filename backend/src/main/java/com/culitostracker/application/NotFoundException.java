package com.culitostracker.application;

/**
 * Resource missing OR not accessible by the authenticated user. Both cases
 * map to 404 so object IDs cannot be probed (no 403 oracle).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
