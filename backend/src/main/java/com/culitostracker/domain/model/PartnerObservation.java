package com.culitostracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "partner_observations")
public class PartnerObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "observer_user_id", nullable = false)
    private UUID observerUserId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_type", nullable = false, length = 30)
    private ObservationType observationType;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "observation_date", nullable = false)
    private LocalDate observationDate = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getObserverUserId() { return observerUserId; }
    public void setObserverUserId(UUID observerUserId) { this.observerUserId = observerUserId; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public ObservationType getObservationType() { return observationType; }
    public void setObservationType(ObservationType observationType) { this.observationType = observationType; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDate getObservationDate() { return observationDate; }
    public void setObservationDate(LocalDate observationDate) { this.observationDate = observationDate; }
    public Instant getCreatedAt() { return createdAt; }
}
