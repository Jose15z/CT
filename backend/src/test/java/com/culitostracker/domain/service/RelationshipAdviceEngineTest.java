package com.culitostracker.domain.service;

import com.culitostracker.domain.model.AdviceCategory;
import com.culitostracker.domain.model.AdviceSource;
import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.DailyTip;
import com.culitostracker.domain.model.Mood;
import com.culitostracker.domain.model.ObservationType;
import com.culitostracker.domain.model.RelationshipStage;
import com.culitostracker.domain.model.RelationshipType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
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
        return new AdviceContext(partnerId, "Laura", RelationshipType.SERIOUS_RELATIONSHIP,
                RelationshipStage.ESTABLISHED, 30L, daysUntilAnniversary, anniversaryYears,
                phase, myMood, myStress, myEnergy, partnerMood, partnerStress, observed,
                List.of(), List.of(), List.of(tip("tips.general.listen")), today);
    }

    private DailyTip tip(String key) {
        DailyTip t = new DailyTip();
        t.setMessageKey(key);
        t.setCategory(AdviceCategory.GENERAL);
        return t;
    }

    @Test
    void anniversaryTodayWinsOverEverything() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.SAD, 5, 1, Mood.SAD, 5,
                ObservationType.NEEDS_SPACE, 0, 2, CyclePhase.MENSTRUATION));
        assertThat(advice.get(0).messageKey()).isEqualTo("advice.anniversary.today");
        assertThat(advice.get(0).category()).isEqualTo(AdviceCategory.ANNIVERSARY);
    }

    @Test
    void upcomingAnniversaryWithinTwoWeeks() {
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                null, 5, 3, null));
        assertThat(advice.get(0).messageKey()).isEqualTo("advice.anniversary.upcoming");
        assertThat(advice.get(0).params()).containsEntry("days", 5).containsEntry("years", 3);
    }

    @Test
    void partnerSelfReportOutranksMyObservation() {
        // Laura herself said she is sad; the user also observed she seems upset.
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, Mood.SAD, null,
                ObservationType.UPSET, null, null, null));
        assertThat(advice.get(0).messageKey()).isEqualTo("advice.partnerReport.sad");
        assertThat(advice.get(0).source()).isEqualTo(AdviceSource.SELF_REPORT);
    }

    @Test
    void observationIsAlwaysLabeledAsObservation() {
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                ObservationType.NEEDS_SPACE, null, null, null));
        AdviceCandidate top = advice.get(0);
        assertThat(top.messageKey()).isEqualTo("advice.observation.needsSpace");
        assertThat(top.source()).isEqualTo(AdviceSource.OBSERVATION);
        assertThat(top.category()).isEqualTo(AdviceCategory.SPACE);
    }

    @Test
    void bothStressedSuggestsCalm() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.STRESSED, 4, 3, null, null,
                ObservationType.STRESSED, null, null, null));
        assertThat(advice.get(0).messageKey()).isEqualTo("advice.both.stressed");
    }

    @Test
    void tiredUserAndStressedPartnerSuggestsQuietPlan() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.TIRED, 2, 2, null, null,
                ObservationType.STRESSED, null, null, null));
        assertThat(advice).extracting(AdviceCandidate::messageKey)
                .contains("advice.quietPlan");
    }

    @Test
    void cyclePhaseAdviceIsMarkedAsCycleSource() {
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                null, null, null, CyclePhase.MENSTRUATION));
        AdviceCandidate cycleAdvice = advice.stream()
                .filter(a -> a.messageKey().equals("advice.phase.menstruation"))
                .findFirst().orElseThrow();
        assertThat(cycleAdvice.source()).isEqualTo(AdviceSource.CYCLE);
    }

    @Test
    void fallsBackToGeneralTipWhenNoSignals() {
        List<AdviceCandidate> advice = engine.advise(base(null, null, null, null, null,
                null, null, null, null));
        assertThat(advice).hasSize(1);
        assertThat(advice.get(0).messageKey()).isEqualTo("tips.general.listen");
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
    void returnsAtMostThreeItems() {
        List<AdviceCandidate> advice = engine.advise(base(Mood.STRESSED, 5, 1, Mood.SAD, 5,
                ObservationType.NEEDS_SPACE, 3, 2, CyclePhase.MENSTRUATION));
        assertThat(advice).hasSizeLessThanOrEqualTo(3);
    }
}
