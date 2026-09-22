package com.culitostracker.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic XP engine. XP is never stored: it is recomputed from the
 * encounter log plus owner-entered partner attributes, so edits and deletions
 * are always reflected and there is nothing to migrate or drift.
 *
 * Per encounter:
 *   base 10
 *   + age bonus: 1 XP per year the partner is over 18 at that date (max +40)
 *   + weight bonus: 1 XP per 2 kg over 50 (max +25)
 *   x 1.5 when the relationship is exclusive (MONOGAMOUS / ENGAGED / MARRIED)
 *   x loyalty: +5% per consecutive same-partner encounter, capped at +50%
 *
 * The two multipliers are the "small advantage" for people in a serious
 * relationship farming XP with a single person: a loyal exclusive couple earns
 * up to 2.25x what partner-hopping yields per encounter.
 */
@Service
public class XpEngine {

    static final int BASE_XP = 10;
    static final int AGE_BONUS_CAP = 40;
    static final int WEIGHT_BONUS_CAP = 25;
    static final double EXCLUSIVE_MULTIPLIER = 1.5;
    static final double LOYALTY_STEP = 0.05;
    static final int LOYALTY_STEP_CAP = 10;

    /** One encounter as the engine sees it. Attributes are null when unknown. */
    public record EncounterFact(UUID partnerId,
                                LocalDate date,
                                Integer partnerAgeAtDate,
                                BigDecimal partnerWeightKg,
                                boolean exclusiveRelationship) {
    }

    public record PartnerXp(long xp, int encounters) {
    }

    public record XpTotals(long totalXp,
                           int level,
                           long xpIntoLevel,
                           long xpForNextLevel,
                           int loyaltyStreak,
                           UUID loyaltyPartnerId,
                           Map<UUID, PartnerXp> byPartner) {
    }

    /** Facts must arrive in stable chronological order (date, then created). */
    public XpTotals compute(List<EncounterFact> facts) {
        long total = 0;
        int streak = 0;
        UUID streakPartner = null;
        Map<UUID, PartnerXp> byPartner = new LinkedHashMap<>();

        for (EncounterFact fact : facts) {
            streak = fact.partnerId().equals(streakPartner) ? streak + 1 : 1;
            streakPartner = fact.partnerId();

            long earned = xpFor(fact, streak);
            total += earned;
            byPartner.merge(fact.partnerId(), new PartnerXp(earned, 1),
                    (a, b) -> new PartnerXp(a.xp() + b.xp(), a.encounters() + b.encounters()));
        }

        int level = levelFor(total);
        return new XpTotals(total, level, total - levelFloor(level), 100L * level,
                streak, streakPartner, byPartner);
    }

    long xpFor(EncounterFact fact, int loyaltyStreak) {
        int points = BASE_XP + ageBonus(fact) + weightBonus(fact);
        double multiplier = (fact.exclusiveRelationship() ? EXCLUSIVE_MULTIPLIER : 1.0)
                * (1.0 + LOYALTY_STEP * Math.min(loyaltyStreak - 1, LOYALTY_STEP_CAP));
        return Math.round(points * multiplier);
    }

    private int ageBonus(EncounterFact fact) {
        if (fact.partnerAgeAtDate() == null) {
            return 0;
        }
        return clamp(fact.partnerAgeAtDate() - 18, AGE_BONUS_CAP);
    }

    private int weightBonus(EncounterFact fact) {
        if (fact.partnerWeightKg() == null) {
            return 0;
        }
        return clamp(fact.partnerWeightKg().subtract(BigDecimal.valueOf(50))
                .intValue() / 2, WEIGHT_BONUS_CAP);
    }

    private static int clamp(int value, int cap) {
        return Math.max(0, Math.min(value, cap));
    }

    /** Reaching level n takes 50 * n * (n - 1) XP; each level costs 100 * n more. */
    public static long levelFloor(int level) {
        return 50L * level * (level - 1);
    }

    public static int levelFor(long xp) {
        int level = 1;
        while (levelFloor(level + 1) <= xp) {
            level++;
        }
        return level;
    }
}
