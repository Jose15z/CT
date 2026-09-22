package com.culitostracker.domain.service;

import com.culitostracker.domain.model.AdviceCategory;
import com.culitostracker.domain.model.AdviceSource;
import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.DailyTip;
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
 * Tie-breaking uses a Random seeded by (partnerId, date) so the advice of the
 * day is stable throughout the day but varies day to day.
 */
@Service
public class RelationshipAdviceEngine {

    private static final Set<Mood> LOW_MOODS =
            Set.of(Mood.SAD, Mood.ANXIOUS, Mood.OVERWHELMED, Mood.STRESSED, Mood.ANGRY);
    private static final Set<Mood> GOOD_MOODS =
            Set.of(Mood.VERY_HAPPY, Mood.HAPPY, Mood.CALM, Mood.AFFECTIONATE);
    private static final int MAX_ADVICE = 3;

    public List<AdviceCandidate> advise(AdviceContext ctx) {
        List<AdviceCandidate> candidates = new ArrayList<>();
        Map<String, Object> name = Map.of("name", ctx.partnerName());
        Random seeded = new Random(Objects.hash(ctx.partnerId(), ctx.today()));

        // --- Anniversary ---
        if (ctx.daysUntilAnniversary() != null && ctx.anniversaryYears() != null
                && ctx.anniversaryYears() >= 1) {
            int days = ctx.daysUntilAnniversary();
            if (days == 0) {
                candidates.add(new AdviceCandidate(AdviceCategory.ANNIVERSARY,
                        "advice.anniversary.today",
                        Map.of("name", ctx.partnerName(), "years", ctx.anniversaryYears()),
                        100, AdviceSource.RELATIONSHIP));
            } else if (days <= 14) {
                candidates.add(new AdviceCandidate(AdviceCategory.ANNIVERSARY,
                        "advice.anniversary.upcoming",
                        Map.of("name", ctx.partnerName(), "days", days, "years", ctx.anniversaryYears()),
                        90, AdviceSource.RELATIONSHIP));
            } else if (days <= 30) {
                candidates.add(new AdviceCandidate(AdviceCategory.ANNIVERSARY,
                        "advice.anniversary.approaching",
                        Map.of("name", ctx.partnerName(), "days", days),
                        35, AdviceSource.RELATIONSHIP));
            }
        }

        // --- Partner self-reports (highest-trust signal about the partner) ---
        if (ctx.partnerMood() != null && LOW_MOODS.contains(ctx.partnerMood())) {
            String key = ctx.partnerMood() == Mood.SAD
                    ? "advice.partnerReport.sad" : "advice.partnerReport.low";
            candidates.add(new AdviceCandidate(AdviceCategory.SUPPORT, key, name,
                    85, AdviceSource.SELF_REPORT));
        }

        // --- Observations (a perception, phrased as such by the UI) ---
        if (ctx.observed() == ObservationType.NEEDS_SPACE) {
            candidates.add(new AdviceCandidate(AdviceCategory.SPACE,
                    "advice.observation.needsSpace", name, 80, AdviceSource.OBSERVATION));
        }

        // --- Both having a rough day ---
        boolean iAmStressed = (ctx.myStress() != null && ctx.myStress() >= 4)
                || ctx.myMood() == Mood.STRESSED || ctx.myMood() == Mood.OVERWHELMED;
        boolean partnerSeemsStressed = ctx.observed() == ObservationType.STRESSED
                || (ctx.partnerStress() != null && ctx.partnerStress() >= 4)
                || ctx.partnerMood() == Mood.STRESSED;
        if (iAmStressed && partnerSeemsStressed) {
            candidates.add(new AdviceCandidate(AdviceCategory.SUPPORT,
                    "advice.both.stressed", name, 75, AdviceSource.SELF_REPORT));
        }

        if (ctx.observed() == ObservationType.SAD) {
            candidates.add(new AdviceCandidate(AdviceCategory.SUPPORT,
                    "advice.observation.sad", name, 70, AdviceSource.OBSERVATION));
        } else if (ctx.observed() == ObservationType.UPSET) {
            candidates.add(new AdviceCandidate(AdviceCategory.CONFLICT,
                    "advice.observation.upset", name, 70, AdviceSource.OBSERVATION));
        } else if (ctx.observed() == ObservationType.DISTANT) {
            candidates.add(new AdviceCandidate(AdviceCategory.COMMUNICATION,
                    "advice.observation.distant", name, 65, AdviceSource.OBSERVATION));
        }

        // --- Tired user + stressed partner: suggest a quiet plan ---
        boolean iAmTired = ctx.myMood() == Mood.TIRED
                || (ctx.myEnergy() != null && ctx.myEnergy() <= 2);
        if (iAmTired && partnerSeemsStressed) {
            candidates.add(new AdviceCandidate(AdviceCategory.GENERAL,
                    "advice.quietPlan", name, 60, AdviceSource.OBSERVATION));
        }

        // --- Both in a good mood ---
        if (ctx.partnerMood() != null && GOOD_MOODS.contains(ctx.partnerMood())
                && ctx.myMood() != null && GOOD_MOODS.contains(ctx.myMood())) {
            candidates.add(new AdviceCandidate(AdviceCategory.DATE_IDEA,
                    "advice.both.good", name, 40, AdviceSource.SELF_REPORT));
        }

        // --- Cycle-based, always neutral and clearly an estimate ---
        if (ctx.estimatedPhase() == CyclePhase.MENSTRUATION) {
            candidates.add(new AdviceCandidate(AdviceCategory.SUPPORT,
                    "advice.phase.menstruation", name, 30, AdviceSource.CYCLE));
        }

        // --- Catalog fallbacks: stage, phase, general ---
        pickTip(ctx.stageTips(), seeded).ifPresent(tip ->
                candidates.add(new AdviceCandidate(tip.getCategory(), tip.getMessageKey(),
                        name, 20, AdviceSource.RELATIONSHIP)));
        pickTip(ctx.phaseTips(), seeded).ifPresent(tip ->
                candidates.add(new AdviceCandidate(tip.getCategory(), tip.getMessageKey(),
                        name, 15, AdviceSource.CYCLE)));
        if (candidates.isEmpty()) {
            pickTip(ctx.generalTips(), seeded).ifPresent(tip ->
                    candidates.add(new AdviceCandidate(tip.getCategory(), tip.getMessageKey(),
                            name, 10, AdviceSource.RELATIONSHIP)));
        }

        return candidates.stream()
                .sorted(Comparator.comparingInt(AdviceCandidate::priority).reversed())
                .limit(MAX_ADVICE)
                .toList();
    }

    private java.util.Optional<DailyTip> pickTip(List<DailyTip> tips, Random seeded) {
        List<DailyTip> active = tips == null ? List.of()
                : tips.stream().filter(DailyTip::isActive).toList();
        if (active.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(active.get(seeded.nextInt(active.size())));
    }
}
