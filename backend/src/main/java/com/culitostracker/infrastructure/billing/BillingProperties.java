package com.culitostracker.infrastructure.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Stripe wiring; with the secret key or price unset, billing is off and everyone is PRO. */
@ConfigurationProperties(prefix = "app.billing")
public record BillingProperties(String stripeSecretKey,
                                String stripeWebhookSecret,
                                String stripePriceId) {

    public boolean enabled() {
        return notBlank(stripeSecretKey) && notBlank(stripePriceId);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
