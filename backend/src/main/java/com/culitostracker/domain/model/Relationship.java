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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "relationships")
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RelationshipType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelationshipStatus status = RelationshipStatus.ACTIVE;

    @Column(name = "dating_start_date")
    private LocalDate datingStartDate;

    @Column(name = "relationship_start_date")
    private LocalDate relationshipStartDate;

    @Column(name = "engagement_date")
    private LocalDate engagementDate;

    @Column(name = "marriage_date")
    private LocalDate marriageDate;

    @Column(name = "relationship_end_date")
    private LocalDate relationshipEndDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** The date the couple counts "together since": relationship start, falling back to dating start. */
    public LocalDate togetherSince() {
        return relationshipStartDate != null ? relationshipStartDate : datingStartDate;
    }

    public boolean isActiveRomantic() {
        return status == RelationshipStatus.ACTIVE && type.isRomantic();
    }

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public RelationshipType getType() { return type; }
    public void setType(RelationshipType type) { this.type = type; }
    public RelationshipStatus getStatus() { return status; }
    public void setStatus(RelationshipStatus status) { this.status = status; }
    public LocalDate getDatingStartDate() { return datingStartDate; }
    public void setDatingStartDate(LocalDate datingStartDate) { this.datingStartDate = datingStartDate; }
    public LocalDate getRelationshipStartDate() { return relationshipStartDate; }
    public void setRelationshipStartDate(LocalDate relationshipStartDate) { this.relationshipStartDate = relationshipStartDate; }
    public LocalDate getEngagementDate() { return engagementDate; }
    public void setEngagementDate(LocalDate engagementDate) { this.engagementDate = engagementDate; }
    public LocalDate getMarriageDate() { return marriageDate; }
    public void setMarriageDate(LocalDate marriageDate) { this.marriageDate = marriageDate; }
    public LocalDate getRelationshipEndDate() { return relationshipEndDate; }
    public void setRelationshipEndDate(LocalDate relationshipEndDate) { this.relationshipEndDate = relationshipEndDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
