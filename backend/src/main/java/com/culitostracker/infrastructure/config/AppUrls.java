package com.culitostracker.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Where the SPA lives, for links the backend hands out (invites, resets). */
@Component
public class AppUrls {

    private final String frontendUrl;

    public AppUrls(@Value("${app.frontend-url:}") String frontendUrl,
                   @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        // The first CORS origin is the SPA unless a URL is configured explicitly.
        this.frontendUrl = !frontendUrl.isBlank()
                ? frontendUrl.replaceAll("/+$", "")
                : allowedOrigins.split(",")[0].trim().replaceAll("/+$", "");
    }

    public String frontend() {
        return frontendUrl;
    }

    public String invite(String rawToken) {
        return frontendUrl + "/invite/" + rawToken;
    }
}
