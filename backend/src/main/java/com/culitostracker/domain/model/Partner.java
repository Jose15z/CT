package com.culitostracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "partners")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    /** Set when the partner is a real linked account (consent-based sharing). */
    @Column(name = "linked_user_id")
    private UUID linkedUserId;

    @Column(nullable = false, length = 60)
    private String name;

    /** Lowercased, accent-stripped name. Feeds dedupe and the leaderboard metric. */
    @Column(name = "normalized_name", nullable = false, length = 60)
    private String normalizedName;

    @Column(length = 60)
    private String nickname;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "avatar_emoji", length = 16)
    private String avatarEmoji;

    /** Optional, owner-entered. Must correspond to an adult (service-enforced). */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Optional, owner-entered approximation in kilograms. */
    @Column(name = "weight_kg", precision = 5, scale = 1)
    private BigDecimal weightKg;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static String normalizeName(String name) {
        String stripped = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return stripped.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public void rename(String newName) {
        this.name = newName;
        this.normalizedName = normalizeName(newName);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Age in whole years at the given date, or null when the birth date is unknown. */
    public Integer ageAt(LocalDate date) {
        return birthDate == null ? null : Period.between(birthDate, date).getYears();
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public UUID getLinkedUserId() { return linkedUserId; }
    public void setLinkedUserId(UUID linkedUserId) { this.linkedUserId = linkedUserId; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAvatarEmoji() { return avatarEmoji; }
    public void setAvatarEmoji(String avatarEmoji) { this.avatarEmoji = avatarEmoji; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
