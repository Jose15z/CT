package com.culitostracker.domain.service;

import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.CycleProfile;
import com.culitostracker.domain.model.PeriodRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CyclePredictionServiceTest {

    private final CyclePredictionService service = new CyclePredictionService();

    private CycleProfile profile(int cycleLength, int periodLength, LocalDate lastStart) {
        CycleProfile p = new CycleProfile();
        p.setAverageCycleLength(cycleLength);
        p.setAveragePeriodLength(periodLength);
        p.setLastPeriodStartDate(lastStart);
        p.setTrackingEnabled(true);
        return p;
    }

    private PeriodRecord record(LocalDate start) {
        PeriodRecord r = new PeriodRecord();
        r.setStartDate(start);
        return r;
    }

    @Test
    void usesConfiguredAverageWithThinHistory() {
        assertThat(service.effectiveCycleLength(
                List.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)), 31))
                .isEqualTo(31);
    }

    @Test
    void averagesRecordedIntervals() {
        // Intervals: 29, 28, 30 → average 29
        List<LocalDate> starts = List.of(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 30),
                LocalDate.of(2026, 2, 27),
                LocalDate.of(2026, 3, 29));
        assertThat(service.effectiveCycleLength(starts, 28)).isEqualTo(29);
    }

    @Test
    void discardsImplausibleIntervals() {
        // A 90-day gap (e.g. unlogged months) must not skew the average.
        List<LocalDate> starts = List.of(
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 12, 30),   // 90-day gap, discarded
                LocalDate.of(2026, 1, 27),    // 28
                LocalDate.of(2026, 2, 24));   // 28
        assertThat(service.effectiveCycleLength(starts, 30)).isEqualTo(28);
    }

    @Test
    void predictsNextPeriodAndOvulation() {
        LocalDate lastStart = LocalDate.of(2026, 9, 9);
        CycleProfile profile = profile(28, 5, lastStart);
        Optional<CyclePrediction> result = service.predict(profile,
                List.of(record(lastStart)), LocalDate.of(2026, 9, 21));

        assertThat(result).isPresent();
        CyclePrediction p = result.get();
        assertThat(p.nextPeriodStart()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(p.ovulationDate()).isEqualTo(LocalDate.of(2026, 9, 23));
        assertThat(p.fertileWindowStart()).isEqualTo(LocalDate.of(2026, 9, 18));
        assertThat(p.fertileWindowEnd()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(p.currentCycleDay()).isEqualTo(13);
    }

    @Test
    void rollsForwardWhenLastRecordIsOld() {
        // Last logged period is ~4 cycles ago; projections land on Jun 29,
        // Jul 27, Aug 24, Sep 21 (today) and Oct 19.
        LocalDate lastStart = LocalDate.of(2026, 6, 1);
        CycleProfile profile = profile(28, 5, lastStart);
        CyclePrediction p = service.predict(profile, List.of(record(lastStart)),
                LocalDate.of(2026, 9, 21)).orElseThrow();
        // Today itself is a projected start day → phase is (predicted) menstruation
        // and the NEXT strictly-future start is one cycle later.
        assertThat(p.currentPhase()).isEqualTo(CyclePhase.MENSTRUATION);
        assertThat(p.currentCycleDay()).isEqualTo(1);
        assertThat(p.nextPeriodStart()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void noDataMeansNoPrediction() {
        CycleProfile profile = profile(28, 5, null);
        assertThat(service.predict(profile, List.of(), LocalDate.now())).isEmpty();
    }

    @Test
    void trackingDisabledMeansNoPrediction() {
        CycleProfile profile = profile(28, 5, LocalDate.of(2026, 9, 1));
        profile.setTrackingEnabled(false);
        assertThat(service.predict(profile, List.of(), LocalDate.now())).isEmpty();
    }

    @Test
    void phasesAcrossTheCycle() {
        LocalDate lastStart = LocalDate.of(2026, 9, 1);
        CycleProfile profile = profile(28, 5, lastStart);
        List<PeriodRecord> records = List.of(record(lastStart));

        assertThat(service.predict(profile, records, LocalDate.of(2026, 9, 3))
                .orElseThrow().currentPhase()).isEqualTo(CyclePhase.MENSTRUATION);
        assertThat(service.predict(profile, records, LocalDate.of(2026, 9, 9))
                .orElseThrow().currentPhase()).isEqualTo(CyclePhase.FOLLICULAR);
        // Ovulation estimated on day 15 (Sep 15 = 29 - 14)
        assertThat(service.predict(profile, records, LocalDate.of(2026, 9, 15))
                .orElseThrow().currentPhase()).isEqualTo(CyclePhase.OVULATION);
        assertThat(service.predict(profile, records, LocalDate.of(2026, 9, 25))
                .orElseThrow().currentPhase()).isEqualTo(CyclePhase.LUTEAL);
    }

    @Test
    void classifyRangeMarksActualAndPredictedPeriods() {
        LocalDate lastStart = LocalDate.of(2026, 9, 1);
        PeriodRecord actual = record(lastStart);
        actual.setEndDate(LocalDate.of(2026, 9, 4));
        CycleProfile profile = profile(28, 5, lastStart);

        List<CycleDayInfo> days = service.classifyRange(profile, List.of(actual),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 5));

        CycleDayInfo sep2 = days.get(1);
        assertThat(sep2.actualPeriod()).isTrue();
        assertThat(sep2.phase()).isEqualTo(CyclePhase.MENSTRUATION);

        // Next projected period starts Sep 29 (28-day cycle).
        CycleDayInfo sep29 = days.stream()
                .filter(d -> d.date().equals(LocalDate.of(2026, 9, 29))).findFirst().orElseThrow();
        assertThat(sep29.predictedPeriod()).isTrue();
        assertThat(sep29.actualPeriod()).isFalse();
        assertThat(sep29.cycleDay()).isEqualTo(1);

        // Ovulation Sep 15 flagged and inside the fertile window.
        CycleDayInfo sep15 = days.stream()
                .filter(d -> d.date().equals(LocalDate.of(2026, 9, 15))).findFirst().orElseThrow();
        assertThat(sep15.ovulation()).isTrue();
        assertThat(sep15.fertile()).isTrue();
    }

    @Test
    void daysBeforeAnyRecordAreUnclassified() {
        LocalDate lastStart = LocalDate.of(2026, 9, 10);
        CycleProfile profile = profile(28, 5, lastStart);
        List<CycleDayInfo> days = service.classifyRange(profile, List.of(record(lastStart)),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5));
        assertThat(days).allSatisfy(d -> {
            assertThat(d.phase()).isNull();
            assertThat(d.cycleDay()).isNull();
        });
    }
}
