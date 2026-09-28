package com.culitostracker.api.dto;

import com.culitostracker.domain.model.Plan;

import java.time.Instant;

public final class BillingDtos {

    private BillingDtos() {
    }

    public record BillingMe(boolean billingEnabled,
                            Plan plan,
                            String status,
                            Instant currentPeriodEnd,
                            int freeTrendWeeks,
                            int freeWishlistItems) {
    }

    /** A URL the browser must navigate to (Stripe Checkout or the Customer Portal). */
    public record RedirectResponse(String url) {
    }
}
