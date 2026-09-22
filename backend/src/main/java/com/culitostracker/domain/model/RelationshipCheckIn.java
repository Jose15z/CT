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
@Table(name = "relationship_check_ins")
public class RelationshipCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "relationship_id", nullable = false)
    private UUID relationshipId;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mood mood;

    @Column(name = "energy_level", nullable = false)
    private int energyLevel;

    @Column(name = "stress_level", nullable = false)
    private int stressLevel;

    @Column(name = "affection_level")
    private Integer affectionLevel;

    @Column(name = "relationship_satisfaction")
    private Integer relationshipSatisfaction;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getRelationshipId() { return relationshipId; }
    public void setRelationshipId(UUID relationshipId) { this.relationshipId = relationshipId; }
    public UUID getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(UUID authorUserId) { this.authorUserId = authorUserId; }
    public Mood getMood() { return mood; }
    public void setMood(Mood mood) { this.mood = mood; }
    public int getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(int energyLevel) { this.energyLevel = energyLevel; }
    public int getStressLevel() { return stressLevel; }
    public void setStressLevel(int stressLevel) { this.stressLevel = stressLevel; }
    public Integer getAffectionLevel() { return affectionLevel; }
    public void setAffectionLevel(Integer affectionLevel) { this.affectionLevel = affectionLevel; }
    public Integer getRelationshipSatisfaction() { return relationshipSatisfaction; }
    public void setRelationshipSatisfaction(Integer relationshipSatisfaction) { this.relationshipSatisfaction = relationshipSatisfaction; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public Instant getCreatedAt() { return createdAt; }
}
