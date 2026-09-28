package com.culitostracker.domain.service;

import com.culitostracker.domain.model.AdviceCategory;
import com.culitostracker.domain.model.AdviceSource;
import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.Mood;
import com.culitostracker.domain.model.ObservationType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic rule engine. No ML, no invented feelings: every rule reads a
 * signal that a user explicitly recorded (or a cycle estimate, always labeled
 * as an estimate) and produces advice as an i18n key + params.
 *
 * Every rule has {@link #VARIANTS} phrasings ({@code advice.<rule>.<n>}) and
 * the generic fallback is composed from {@link TipCatalog}, so the same
 * situation reads differently from one day to the next. Selection uses a
 * Random seeded by (partnerId, date): stable within a day, new the next.
 */
@Service
public class RelationshipAdviceEngine {

    public static final int VARIANTS = 3;

    /** Every rule id the engine can emit; the i18n contract test mirrors it. */
    public static final List<String> RULES = List.of(
            "anniversary.today", "anniversary.upcoming", "anniversary.approaching",
            "partnerReport.sad", "partnerReport.low", "partnerReport.good",
            "observation.needsSpace", "observation.sad", "observation.upset",
            "observation.distant", "observation.happy", "observation.affectionate",
            "observation.tired", "observation.stressed", "observation.notSure",
            "both.stressed", "both.good", "quietPlan",
            "self.low", "self.stressed", "self.tired", "self.lowSatisfaction",
            "self.affectionate", "self.good",
            "phase.menstruation");

    private static final Set<Mood> LOW_MOODS =
            Set.of(Mood.SAD, Mood.ANXIOUS, Mood.OVERWHELMED, Mood.STRESSED, Mood.ANGRY);
    private static final Set<Mood> GOOD_MOODS =
            Set.of(Mood.VERY_HAPPY, Mood.HAPPY, Mood.CALM, Mood.AFFECTIONATE);
    private static final int MAX_ADVICE = 3;
    private static final int COMPOSED_PRIORITY = 20;

    public List<AdviceCandidate> advise(AdviceContext ctx) {
        List<AdviceCandidate> out = new ArrayList<>();
        Random seeded = new Random(Objects.hash(ctx.partnerId(), ctx.today()));
        Map<String, Object> name = Map.of("name", ctx.partnerName());

        // --- Anniversary ---
        if (ctx.daysUntilAnniversary() != null && ctx.anniversaryYears() != null
                && ctx.anniversaryYears() >= 1) {
            int days = ctx.daysUntilAnniversary();
            Map<String, Object> params = Map.of("name", ctx.partnerName(),
                    "days", days, "years", ctx.anniversaryYears());
            if (days == 0) {
                add(out, seeded, "anniversary.today", params, 100, AdviceCategory.ANNIVERSARY, AdviceSource.RELATIONSHIP);
            } else if (days <= 14) {
                add(out, seeded, "anniversary.upcoming", params, 90, AdviceCategory.ANNIVERSARY, AdviceSource.RELATIONSHIP);
            } else if (days <= 30) {
                add(out, seeded, "anniversary.approaching", params, 35, AdviceCategory.ANNIVERSARY, AdviceSource.RELATIONSHIP);
            }
        }

        // --- Partner self-reports (highest-trust signal about the partner) ---
        boolean partnerLow = ctx.partnerMood() != null && LOW_MOODS.contains(ctx.partnerMood());
        boolean partnerGood = ctx.partnerMood() != null && GOOD_MOODS.contains(ctx.partnerMood());
        if (partnerLow) {
            String rule = ctx.partnerMood() == Mood.SAD ? "partnerReport.sad" : "partnerReport.low";
            add(out, seeded, rule, name, 85, AdviceCategory.SUPPORT, AdviceSource.SELF_REPORT);
        }

        // --- Observations (a perception, phrased as such by the copy) ---
        ObservationType observed = ctx.observed();
        if (observed == ObservationType.NEEDS_SPACE) {
            add(out, seeded, "observation.needsSpace", name, 80, AdviceCategory.SPACE, AdviceSource.OBSERVATION);
        }

        // --- Both having a rough day ---
        boolean iAmStressed = (ctx.myStress() != null && ctx.myStress() >= 4)
                || ctx.myMood() == Mood.STRESSED || ctx.myMood() == Mood.OVERWHELMED;
        boolean partnerSeemsStressed = observed == ObservationType.STRESSED
                || (ctx.partnerStress() != null && ctx.partnerStress() >= 4)
                || ctx.partnerMood() == Mood.STRESSED;
        if (iAmStressed && partnerSeemsStressed) {
            add(out, seeded, "both.stressed", name, 75, AdviceCategory.SUPPORT, AdviceSource.SELF_REPORT);
        }

        if (observed == ObservationType.SAD) {
            add(out, seeded, "observation.sad", name, 70, AdviceCategory.SUPPORT, AdviceSource.OBSERVATION);
        } else if (observed == ObservationType.UPSET) {
            add(out, seeded, "observation.upset", name, 70, AdviceCategory.CONFLICT, AdviceSource.OBSERVATION);
        } else if (observed == ObservationType.DISTANT) {
            add(out, seeded, "observation.distant", name, 65, AdviceCategory.COMMUNICATION, AdviceSource.OBSERVATION);
        } else if (observed == ObservationType.STRESSED && !iAmStressed) {
            add(out, seeded, "observation.stressed", name, 60, AdviceCategory.SUPPORT, AdviceSource.OBSERVATION);
        } else if (observed == ObservationType.TIRED) {
            add(out, seeded, "observation.tired", name, 55, AdviceCategory.SUPPORT, AdviceSource.OBSERVATION);
        } else if (observed == ObservationType.NOT_SURE) {
            add(out, seeded, "observation.notSure", name, 50, AdviceCategory.COMMUNICATION, AdviceSource.OBSERVATION);
        } else if (observed == ObservationType.HAPPY || observed == ObservationType.VERY_HAPPY) {
            add(out, seeded, "observation.happy", name, 45, AdviceCategory.DATE_IDEA, AdviceSource.OBSERVATION);
        } else if (observed == ObservationType.AFFECTIONATE) {
            add(out, seeded, "observation.affectionate", name, 45, AdviceCategory.AFFECTION, AdviceSource.OBSERVATION);
        }

        // --- My own check-in: what I recorded about myself ---
        if (ctx.mySatisfaction() != null && ctx.mySatisfaction() <= 2) {
            add(out, seeded, "self.lowSatisfaction", name, 65, AdviceCategory.COMMUNICATION, AdviceSource.SELF_REPORT);
        }
        boolean iAmTired = ctx.myMood() == Mood.TIRED
                || (ctx.myEnergy() != null && ctx.myEnergy() <= 2);
        if (iAmTired && partnerSeemsStressed) {
            add(out, seeded, "quietPlan", name, 60, AdviceCategory.GENERAL, AdviceSource.OBSERVATION);
        }
        boolean iAmLow = ctx.myMood() == Mood.SAD || ctx.myMood() == Mood.ANXIOUS
                || ctx.myMood() == Mood.OVERWHELMED || ctx.myMood() == Mood.ANGRY;
        if (iAmLow) {
            add(out, seeded, "self.low", name, 55, AdviceCategory.SELF_CARE, AdviceSource.SELF_REPORT);
        } else if (iAmStressed && !partnerSeemsStressed) {
            add(out, seeded, "self.stressed", name, 50, AdviceCategory.SELF_CARE, AdviceSource.SELF_REPORT);
        } else if (iAmTired && !partnerSeemsStressed) {
            add(out, seeded, "self.tired", name, 40, AdviceCategory.SELF_CARE, AdviceSource.SELF_REPORT);
        }

        // --- Good signals deserve advice too: that is what makes it feel alive ---
        if (partnerGood && ctx.myMood() != null && GOOD_MOODS.contains(ctx.myMood())) {
            add(out, seeded, "both.good", name, 40, AdviceCategory.DATE_IDEA, AdviceSource.SELF_REPORT);
        } else if (partnerGood) {
            add(out, seeded, "partnerReport.good", name, 45, AdviceCategory.AFFECTION, AdviceSource.SELF_REPORT);
        }
        boolean iAmAffectionate = ctx.myMood() == Mood.AFFECTIONATE
                || (ctx.myAffection() != null && ctx.myAffection() >= 4);
        if (iAmAffectionate) {
            add(out, seeded, "self.affectionate", name, 42, AdviceCategory.AFFECTION, AdviceSource.SELF_REPORT);
        } else if (ctx.myMood() != null && GOOD_MOODS.contains(ctx.myMood()) && !partnerGood) {
            add(out, seeded, "self.good", name, 38, AdviceCategory.DATE_IDEA, AdviceSource.SELF_REPORT);
        }

        // --- Cycle-based, always neutral and clearly an estimate ---
        if (ctx.estimatedPhase() == CyclePhase.MENSTRUATION) {
            add(out, seeded, "phase.menstruation", name, 30, AdviceCategory.SUPPORT, AdviceSource.CYCLE);
        }

        // --- Composed generic tip: always present, so there is never silence ---
        out.add(TipCatalog.compose(ctx.stage(), ctx.estimatedPhase(), ctx.partnerName(),
                COMPOSED_PRIORITY, seeded));

        return out.stream()
                .sorted(Comparator.comparingInt(AdviceCandidate::priority).reversed())
                .limit(MAX_ADVICE)
                .toList();
    }

    private static void add(List<AdviceCandidate> out, Random seeded, String rule,
                            Map<String, Object> params, int priority,
                            AdviceCategory category, AdviceSource source) {
        String key = "advice." + rule + "." + (1 + seeded.nextInt(VARIANTS));
        out.add(new AdviceCandidate(category, key, params, priority, source));
    }
}
