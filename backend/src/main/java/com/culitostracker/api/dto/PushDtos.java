package com.culitostracker.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PushDtos {

    private PushDtos() {
    }

    /** Mirrors PushSubscription.toJSON() from the browser. */
    public record SubscribeRequest(
            @NotBlank @Size(max = 2000) String endpoint,
            @NotBlank @Size(max = 255) String p256dh,
            @NotBlank @Size(max = 255) String auth,
            @Size(max = 60) String timezone) {
    }

    public record UnsubscribeRequest(@NotBlank @Size(max = 2000) String endpoint) {
    }

    public record PushConfigResponse(boolean enabled, String publicKey, boolean subscribed) {
    }
}
