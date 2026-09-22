package com.culitostracker.domain.service;

import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.DailyTip;
import com.culitostracker.domain.model.Mood;
import com.culitostracker.domain.model.ObservationType;
import com.culitostracker.domain.model.RelationshipStage;
import com.culitostracker.domain.model.RelationshipType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Everything the advice engine may look at. Signals are kept strictly
 * separated by origin so the engine never turns a perception into a fact:
 * - myMood / myStress / myEnergy: the user's own check-in (today or yesterday)
 * - partnerMood / partnerStress: the partner's OWN check-in, present only when
 *   the partner is a linked account that shared check-ins
 * - observed: what the user registered about the partner (a perception)
 */
public record AdviceContext(UUID partnerId,
                            String partnerName,
                            RelationshipType type,
                            RelationshipStage stage,          // null without a start date
                            Long monthsTogether,              // null without a start date
                            Integer daysUntilAnniversary,     // null when not applicable
                            Integer anniversaryYears,
                            CyclePhase estimatedPhase,        // null without cycle data
                            Mood myMood,                      // null without a recent check-in
                            Integer myStress,
                            Integer myEnergy,
                            Mood partnerMood,                 // self-reported by the partner
                            Integer partnerStress,
                            ObservationType observed,         // my recent perception
                            List<DailyTip> stageTips,
                            List<DailyTip> phaseTips,
                            List<DailyTip> generalTips,
                            LocalDate today) {
}
