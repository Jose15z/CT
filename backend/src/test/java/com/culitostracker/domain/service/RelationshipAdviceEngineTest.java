package com.culitostracker.domain.service;

import com.culitostracker.domain.model.AdviceCategory;
import com.culitostracker.domain.model.AdviceSource;
import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.Mood;
import com.culitostracker.domain.model.ObservationType;
import com.culitostracker.domain.model.RelationshipStage;
import com.culitostracker.domain.model.RelationshipType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RelationshipAdviceEngineTest {

    private final RelationshipAdviceEngine engine = new RelationshipAdviceEngine();
    private final UUID partnerId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 9, 21);

    private AdviceContext base(Mood myMood, Integer myStress, Integer myEnergy,
                               Mood partnerMood, Integer partnerStress,
                               ObservationType observed,
                               Integer daysUntilAnniversary, Integer anniversaryYears,
                               CyclePhase phase) {
        return base(myMood, myStress, myEnergy, null, null, partnerMood, partnerStress,
                observed, daysUntilAnniversary, anniversaryYears, phase, today);
    }

    private AdviceContext base(Mood myMood, Integer myStress, Integer myEnergy,
                               Integer myAffection, Integer mySatisfaction,
                               Mood partnerMood, Integer partnerStress,
                               ObservationType observed,
                               Integer daysUntilAnniversary, Integer anniversaryYears,
                               CyclePhase phase, LocalDate date) {
        return new AdviceContext(partnerId, "Laura", RelationshipType.SERIOUS_RELATIONSHIP,
                RelationshipStage.ESTABLISHED, 30L, daysUntilAnniversary, anniversaryYears,
                phase, myMood, myStress, myEnergy, myAffection, mySatisfaction,
                partnerMood, partnerStress, observed, null, null, 0, date);
    }

    @Test
    void birthdayOutranksObservationsAndCarriesGiftIdeas() {
        AdviceContext ctx = new AdviceContext(partnerId, "Laura", RelationshipType.DATING,
                RelationshipStage.NEW, 2L, null, null, null, null, null, null, null, null,
                null, null, ObservationType.SAD, 0, 30, 2, today);
        List<AdviceCandidate> advice = engine.advise(ctx);
        assertThat(ruleOf(advice.get(0))).isEqualTo("birthday.today");
        assertThat(advice.get(0).params()).containsEntry("age", 30).containsEntry("wishes", 2);

        AdviceContext soon = new AdviceContext(partnerId, "Laura", RelationshipType.DATING,
                RelationshipStage.NEW, 2L, null, null, null, null, null, null, null, null,
                null, null, null, 10, 30, 0, today);
        assertThat(ruleOf(engine.advise(soon).get(0))).isEqualTo("birthday.upcoming");
    }

    /** Rule keys carry a variant suffix: "advice.rule.N" → "rule". */
    private static String ruleOf(AdviceCandidate c) {
        String key = c.messageKey();
        if (!key.startsWith("advice.")) {
            return key; // composed tip
        }
        return key.substring("advice.".length(), key.lastIndexOf('.'));
    }

    @Test
    void anniversaryTodayWinsOverEverything() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.SAD, 5, 1, Mood.SAD, 5,
                ObservationType.NEEDS_SPACE, 0, 2, CyclePhase.MENSTRUATION));
        assertThat(ruleOf(advice.get(0))).isEqualTo("anniversary.today");
        assertThat(advice.get(0).category()).isEqualTo(AdviceCategory.ANNIVERSARY);
    }

    @Test
    void upcomingAnniversaryWithinTwoWeeks() {
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                null, 5, 3, null));
        assertThat(ruleOf(advice.get(0))).isEqualTo("anniversary.upcoming");
        assertThat(advice.get(0).params()).containsEntry("days", 5).containsEntry("years", 3);
    }

    @Test
    void partnerSelfReportOutranksMyObservation() {
        // Laura herself said she is sad; the user also observed she seems upset.
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, Mood.SAD, null,
                ObservationType.UPSET, null, null, null));
        assertThat(ruleOf(advice.get(0))).isEqualTo("partnerReport.sad");
        assertThat(advice.get(0).source()).isEqualTo(AdviceSource.SELF_REPORT);
    }

    @Test
    void observationIsAlwaysLabeledAsObservation() {
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                ObservationType.NEEDS_SPACE, null, null, null));
        AdviceCandidate top = advice.get(0);
        assertThat(ruleOf(top)).isEqualTo("observation.needsSpace");
        assertThat(top.source()).isEqualTo(AdviceSource.OBSERVATION);
        assertThat(top.category()).isEqualTo(AdviceCategory.SPACE);
    }

    @Test
    void bothStressedSuggestsCalm() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.STRESSED, 4, 3, null, null,
                ObservationType.STRESSED, null, null, null));
        assertThat(ruleOf(advice.get(0))).isEqualTo("both.stressed");
    }

    @Test
    void tiredUserAndStressedPartnerSuggestsQuietPlan() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.TIRED, 2, 2, null, null,
                ObservationType.STRESSED, null, null, null));
        assertThat(advice).extracting(RelationshipAdviceEngineTest::ruleOf).contains("quietPlan");
    }

    @Test
    void positiveSignalsProduceAdviceToo() {
        // A happy observation used to fall through to the generic tip; now it reacts.
        List<AdviceCandidate> happy = engine.advise(base(null, null, null, null, null,
                ObservationType.HAPPY, null, null, null));
        assertThat(ruleOf(happy.get(0))).isEqualTo("observation.happy");
        assertThat(happy.get(0).source()).isEqualTo(AdviceSource.OBSERVATION);

        List<AdviceCandidate> affectionate = engine.advise(base(Mood.AFFECTIONATE, 1, 4,
                5, 5, null, null, null, null, null, null, today));
        assertThat(ruleOf(affectionate.get(0))).isEqualTo("self.affectionate");
    }

    @Test
    void lowSatisfactionOutranksAGoodMood() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.HAPPY, 1, 4, 3, 1,
                null, null, null, null, null, null, today));
        assertThat(ruleOf(advice.get(0))).isEqualTo("self.lowSatisfaction");
    }

    @Test
    void cyclePhaseAdviceIsMarkedAsCycleSource() {
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                null, null, null, CyclePhase.MENSTRUATION));
        AdviceCandidate cycleAdvice = advice.stream()
                .filter(a -> a.messageKey().startsWith("advice.phase.menstruation."))
                .findFirst().orElseThrow();
        assertThat(cycleAdvice.source()).isEqualTo(AdviceSource.CYCLE);
    }

    @Test
    void fallsBackToComposedTipWhenNoSignals() {
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                null, null, null, null));
        assertThat(advice).hasSize(1);
        AdviceCandidate tip = advice.get(0);
        assertThat(tip.messageKey()).isEqualTo(TipCatalog.COMPOSED_KEY);
        assertThat(tip.params()).containsKeys("lead", "action", "name");
        assertThat((String) tip.params().get("lead")).startsWith("tips.lead.");
        assertThat((String) tip.params().get("action")).startsWith("tips.action.");
    }

    @Test
    void neverInventsFeelingsWithoutSignals() {
        // With no check-ins and no observations, no advice may claim a mood.
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                null, null, null, null));
        assertThat(advice).noneMatch(a ->
                a.source() == AdviceSource.SELF_REPORT || a.source() == AdviceSource.OBSERVATION);
    }

    @Test
    void adviceIsStableWithinTheSameDay() {
        AdviceContext context = base(null, null, null, null, null, null, null, null, null);
        assertThat(engine.advise(context)).isEqualTo(engine.advise(context));
    }

    @Test
    void sameSituationReadsDifferentlyAcrossDays() {
        // The exact same signals over two weeks: the phrasing must rotate.
        Set<String> sadVariants = new HashSet<>();
        Set<String> composedTips = new HashSet<>();
        for (int i = 0; i < 14; i++) {
            LocalDate day = today.plusDays(i);
            sadVariants.add(engine.advise(base(null, null, null, null, null, null, null,
                    ObservationType.SAD, null, null, null, day)).get(0).messageKey());
            composedTips.add(engine.advise(base(null, null, null, null, null, null, null,
                    null, null, null, null, day)).get(0).params().toString());
        }
        assertThat(sadVariants).hasSizeGreaterThan(1);
        assertThat(composedTips).hasSizeGreaterThan(10);
    }

    @Test
    void ruleVariantKeysAreWellFormed() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.STRESSED, 5, 1, Mood.SAD, 5,
                ObservationType.NEEDS_SPACE, 3, 2, CyclePhase.MENSTRUATION));
        assertThat(advice).hasSizeLessThanOrEqualTo(3);
        for (AdviceCandidate c : advice) {
            assertThat(RelationshipAdviceEngine.RULES).contains(ruleOf(c));
            int variant = Integer.parseInt(c.messageKey().substring(c.messageKey().lastIndexOf('.') + 1));
            assertThat(variant).isBetween(1, RelationshipAdviceEngine.VARIANTS);
        }
    }

    @Test
    void theBankHoldsOverTenThousandDistinctTips() {
        assertThat(TipCatalog.distinctCombinations()).isGreaterThanOrEqualTo(10_000);
    }

    @Test
    void cycleAwareLeadsOnlyAppearWithThatPhase() {
        // Without cycle data, no composed tip may reference the cycle.
        for (int i = 0; i < 60; i++) {
            AdviceCandidate tip = engine.advise(base(null, null, null, null, null, null, null,
                    null, null, null, null, today.plusDays(i))).get(0);
            assertThat(tip.source()).isEqualTo(AdviceSource.RELATIONSHIP);
        }
    }
}
