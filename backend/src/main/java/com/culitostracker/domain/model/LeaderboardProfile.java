package com.culitostracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Opt-in leaderboard participation. A user appears publicly only when
 * enabled is true AND publicAlias is set. Nothing about individual partners
 * is ever exposed, only the aggregated score.
 */
@Entity
@Table(name = "leaderboard_profiles")
public class LeaderboardProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "public_alias", length = 30)
    private String publicAlias;

    @Column(name = "show_avatar", nullable = false)
    private boolean showAvatar = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isPubliclyVisible() {
        return enabled && publicAlias != null && !publicAlias.isBlank();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPublicAlias() { return publicAlias; }
    public void setPublicAlias(String publicAlias) { this.publicAlias = publicAlias; }
    public boolean isShowAvatar() { return showAvatar; }
    public void setShowAvatar(boolean showAvatar) { this.showAvatar = showAvatar; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
