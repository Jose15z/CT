package com.culitostracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cycle_profiles")
public class CycleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private UUID partnerId;

    @Column(name = "average_cycle_length", nullable = false)
    private int averageCycleLength = 28;

    @Column(name = "average_period_length", nullable = false)
    private int averagePeriodLength = 5;

    @Column(name = "last_period_start_date")
    private LocalDate lastPeriodStartDate;

    @Column(name = "tracking_enabled", nullable = false)
    private boolean trackingEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public int getAverageCycleLength() { return averageCycleLength; }
    public void setAverageCycleLength(int averageCycleLength) { this.averageCycleLength = averageCycleLength; }
    public int getAveragePeriodLength() { return averagePeriodLength; }
    public void setAveragePeriodLength(int averagePeriodLength) { this.averagePeriodLength = averagePeriodLength; }
    public LocalDate getLastPeriodStartDate() { return lastPeriodStartDate; }
    public void setLastPeriodStartDate(LocalDate lastPeriodStartDate) { this.lastPeriodStartDate = lastPeriodStartDate; }
    public boolean isTrackingEnabled() { return trackingEnabled; }
    public void setTrackingEnabled(boolean trackingEnabled) { this.trackingEnabled = trackingEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
