package com.culitostracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Catalog entry for a generic tip. Carries an i18n key; translations live in
 * the frontend bundles so language switching works instantly.
 */
@Entity
@Table(name = "daily_tips")
public class DailyTip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_key", nullable = false, length = 120, unique = true)
    private String messageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdviceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_phase", length = 20)
    private CyclePhase cyclePhase;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_stage", length = 20)
    private RelationshipStage relationshipStage;

    @Column(nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public String getMessageKey() { return messageKey; }
    public void setMessageKey(String messageKey) { this.messageKey = messageKey; }
    public AdviceCategory getCategory() { return category; }
    public void setCategory(AdviceCategory category) { this.category = category; }
    public CyclePhase getCyclePhase() { return cyclePhase; }
    public void setCyclePhase(CyclePhase cyclePhase) { this.cyclePhase = cyclePhase; }
    public RelationshipStage getRelationshipStage() { return relationshipStage; }
    public void setRelationshipStage(RelationshipStage relationshipStage) { this.relationshipStage = relationshipStage; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
