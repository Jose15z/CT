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
import java.util.UUID;

/**
 * A consent grant: the person the data belongs to (grantedBy) allows another
 * user (grantedTo) to see a scope of information about a partner record.
 * Revocable at any time by flipping status to REVOKED.
 */
@Entity
@Table(name = "partner_access")
public class PartnerAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "granted_by_user_id", nullable = false)
    private UUID grantedByUserId;

    @Column(name = "granted_to_user_id", nullable = false)
    private UUID grantedToUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccessScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessStatus status = AccessStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public void setPartnerId(UUID partnerId) { this.partnerId = partnerId; }
    public UUID getGrantedByUserId() { return grantedByUserId; }
    public void setGrantedByUserId(UUID grantedByUserId) { this.grantedByUserId = grantedByUserId; }
    public UUID getGrantedToUserId() { return grantedToUserId; }
    public void setGrantedToUserId(UUID grantedToUserId) { this.grantedToUserId = grantedToUserId; }
    public AccessScope getScope() { return scope; }
    public void setScope(AccessScope scope) { this.scope = scope; }
    public AccessStatus getStatus() { return status; }
    public void setStatus(AccessStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
