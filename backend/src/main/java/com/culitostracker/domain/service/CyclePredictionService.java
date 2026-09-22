package com.culitostracker.domain.service;

import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.CycleProfile;
import com.culitostracker.domain.model.PeriodRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Cycle estimation. Deliberately simple for the MVP:
 * - effective cycle length = average of the last recorded intervals when there
 *   is enough history, otherwise the profile's configured average;
 * - ovulation is estimated 14 days before the next expected period (fixed
 *   luteal phase approximation);
 * - fertile window = ovulation -5 .. +1.
 *
 * Everything here is an estimate. The API layer attaches the disclaimers.
 */
@Service
public class CyclePredictionService {

    static final int LUTEAL_PHASE_DAYS = 14;
    static final int FERTILE_DAYS_BEFORE_OVULATION = 5;
    static final int FERTILE_DAYS_AFTER_OVULATION = 1;
    static final int MIN_PLAUSIBLE_CYCLE = 15;
    static final int MAX_PLAUSIBLE_CYCLE = 60;
    private static final int MAX_INTERVALS_FOR_AVERAGE = 6;
    private static final int MIN_STARTS_FOR_AVERAGE = 3;

    /**
     * Average of the most recent plausible intervals between period starts.
     * Falls back to the configured average when history is thin.
     */
    public int effectiveCycleLength(List<LocalDate> periodStarts, int configuredAverage) {
        List<LocalDate> sorted = periodStarts.stream().sorted().distinct().toList();
        if (sorted.size() < MIN_STARTS_FOR_AVERAGE) {
            return configuredAverage;
        }
        List<Long> intervals = new ArrayList<>();
        for (int i = sorted.size() - 1; i > 0 && intervals.size() < MAX_INTERVALS_FOR_AVERAGE; i--) {
            long days = ChronoUnit.DAYS.between(sorted.get(i - 1), sorted.get(i));
            if (days >= MIN_PLAUSIBLE_CYCLE && days <= MAX_PLAUSIBLE_CYCLE) {
                intervals.add(days);
            }
        }
        if (intervals.isEmpty()) {
            return configuredAverage;
        }
        double avg = intervals.stream().mapToLong(Long::longValue).average().orElse(configuredAverage);
        return (int) Math.round(avg);
    }

    public Optional<CyclePrediction> predict(CycleProfile profile, List<PeriodRecord> records, LocalDate today) {
        Optional<LocalDate> lastStartOpt = lastPeriodStart(profile, records);
        if (lastStartOpt.isEmpty() || !profile.isTrackingEnabled()) {
            return Optional.empty();
        }
        LocalDate lastStart = lastStartOpt.get();
        int cycleLength = effectiveCycleLength(startDates(records), profile.getAverageCycleLength());
        int periodLength = profile.getAveragePeriodLength();

        // Roll forward whole cycles so nextPeriodStart is always after today.
        LocalDate nextStart = lastStart.plusDays(cycleLength);
        while (!nextStart.isAfter(today)) {
            nextStart = nextStart.plusDays(cycleLength);
        }
        LocalDate ovulation = nextStart.minusDays(LUTEAL_PHASE_DAYS);
        LocalDate fertileStart = ovulation.minusDays(FERTILE_DAYS_BEFORE_OVULATION);
        LocalDate fertileEnd = ovulation.plusDays(FERTILE_DAYS_AFTER_OVULATION);

        CycleDayInfo todayInfo = classifyDay(today, profile, records, cycleLength);
        int cycleDay = todayInfo.cycleDay() != null ? todayInfo.cycleDay()
                : (int) (ChronoUnit.DAYS.between(lastStart, today) % cycleLength) + 1;
        CyclePhase phase = todayInfo.phase() != null ? todayInfo.phase() : CyclePhase.FOLLICULAR;

        return Optional.of(new CyclePrediction(lastStart, cycleLength, periodLength,
                nextStart, ovulation, fertileStart, fertileEnd, cycleDay, phase));
    }

    /** Classify every day in [from, to] for the calendar view. */
    public List<CycleDayInfo> classifyRange(CycleProfile profile, List<PeriodRecord> records,
                                            LocalDate from, LocalDate to) {
        int cycleLength = effectiveCycleLength(startDates(records), profile.getAverageCycleLength());
        List<CycleDayInfo> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.add(classifyDay(d, profile, records, cycleLength));
        }
        return days;
    }

    private CycleDayInfo classifyDay(LocalDate date, CycleProfile profile,
                                     List<PeriodRecord> records, int cycleLength) {
        int periodLength = profile.getAveragePeriodLength();
        List<PeriodRecord> sorted = records.stream()
                .sorted(Comparator.comparing(PeriodRecord::getStartDate))
                .toList();

        boolean actualPeriod = sorted.stream().anyMatch(r -> {
            LocalDate end = r.getEndDate() != null ? r.getEndDate()
                    : r.getStartDate().plusDays(periodLength - 1L);
            return !date.isBefore(r.getStartDate()) && !date.isAfter(end);
        });

        // Anchor: latest cycle start (actual or projected) on or before the date.
        LocalDate lastActual = sorted.isEmpty()
                ? profile.getLastPeriodStartDate()
                : sorted.get(sorted.size() - 1).getStartDate();
        if (lastActual == null) {
            return new CycleDayInfo(date, null, null, actualPeriod, false, false, false);
        }

        LocalDate anchor;
        LocalDate nextStart;
        boolean projectedAnchor;
        if (date.isBefore(lastActual)) {
            // Historical date: anchor on the actual start that precedes it.
            LocalDate prev = null;
            LocalDate next = lastActual;
            for (PeriodRecord r : sorted) {
                if (!r.getStartDate().isAfter(date)) {
                    prev = r.getStartDate();
                } else {
                    next = r.getStartDate();
                    break;
                }
            }
            LocalDate profileStart = profile.getLastPeriodStartDate();
            if (prev == null && profileStart != null && !profileStart.isAfter(date)) {
                prev = profileStart;
            }
            if (prev == null) {
                return new CycleDayInfo(date, null, null, actualPeriod, false, false, false);
            }
            anchor = prev;
            nextStart = next;
            projectedAnchor = false;
        } else {
            long since = ChronoUnit.DAYS.between(lastActual, date);
            long cyclesAhead = since / cycleLength;
            anchor = lastActual.plusDays(cyclesAhead * cycleLength);
            nextStart = anchor.plusDays(cycleLength);
            projectedAnchor = cyclesAhead > 0;
        }

        int cycleDay = (int) ChronoUnit.DAYS.between(anchor, date) + 1;
        LocalDate ovulation = nextStart.minusDays(LUTEAL_PHASE_DAYS);
        boolean fertile = !date.isBefore(ovulation.minusDays(FERTILE_DAYS_BEFORE_OVULATION))
                && !date.isAfter(ovulation.plusDays(FERTILE_DAYS_AFTER_OVULATION));
        boolean isOvulationDay = date.equals(ovulation);

        boolean predictedPeriod = projectedAnchor && cycleDay <= periodLength;

        CyclePhase phase;
        if (actualPeriod || (cycleDay <= periodLength && !projectedAnchor) || predictedPeriod) {
            phase = CyclePhase.MENSTRUATION;
        } else if (!date.isBefore(ovulation.minusDays(1)) && !date.isAfter(ovulation.plusDays(1))) {
            phase = CyclePhase.OVULATION;
        } else if (date.isBefore(ovulation)) {
            phase = CyclePhase.FOLLICULAR;
        } else {
            phase = CyclePhase.LUTEAL;
        }

        return new CycleDayInfo(date, cycleDay, phase, actualPeriod, predictedPeriod, fertile, isOvulationDay);
    }

    private Optional<LocalDate> lastPeriodStart(CycleProfile profile, List<PeriodRecord> records) {
        Optional<LocalDate> fromRecords = records.stream()
                .map(PeriodRecord::getStartDate)
                .max(Comparator.naturalOrder());
        if (fromRecords.isPresent()) {
            return fromRecords;
        }
        return Optional.ofNullable(profile.getLastPeriodStartDate());
    }

    private List<LocalDate> startDates(List<PeriodRecord> records) {
        return records.stream().map(PeriodRecord::getStartDate).toList();
    }
}
