package com.culitostracker.infrastructure.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** VAPID keys; without them push is simply off and the UI hides reminders. */
@ConfigurationProperties(prefix = "app.push")
public record PushProperties(String publicKey, String privateKey, String subject) {

    public boolean enabled() {
        return publicKey != null && !publicKey.isBlank()
                && privateKey != null && !privateKey.isBlank();
    }
}
