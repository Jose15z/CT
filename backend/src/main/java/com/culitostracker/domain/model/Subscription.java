package com.culitostracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of the provider's subscription; one per user. */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String provider = "STRIPE";

    @Column(name = "provider_customer_id", length = 120)
    private String providerCustomerId;

    @Column(name = "provider_subscription_id", length = 120, unique = true)
    private String providerSubscriptionId;

    /** Stripe status verbatim: active, trialing, past_due, canceled, unpaid, ... */
    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Statuses that still grant access (Stripe keeps past_due briefly during retries). */
    public boolean grantsAccess() {
        return "active".equals(status) || "trialing".equals(status) || "past_due".equals(status);
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderCustomerId() { return providerCustomerId; }
    public void setProviderCustomerId(String providerCustomerId) { this.providerCustomerId = providerCustomerId; }
    public String getProviderSubscriptionId() { return providerSubscriptionId; }
    public void setProviderSubscriptionId(String providerSubscriptionId) { this.providerSubscriptionId = providerSubscriptionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(Instant currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    public Instant getUpdatedAt() { return updatedAt; }
}
