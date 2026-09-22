package com.culitostracker.domain.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Service
public class RelationshipDurationCalculator {

    public RelationshipDuration durationBetween(LocalDate start, LocalDate today) {
        LocalDate end = today.isBefore(start) ? start : today;
        Period period = Period.between(start, end);
        long totalDays = ChronoUnit.DAYS.between(start, end);
        return new RelationshipDuration(period.getYears(), period.getMonths(), period.getDays(), totalDays);
    }

    /**
     * Next annual anniversary of {@code start}. A Feb 29 start is celebrated on
     * Feb 28 in non-leap years. Returns today with daysUntil 0 when it falls today.
     */
    public AnniversaryInfo nextAnniversary(LocalDate start, LocalDate today) {
        MonthDay monthDay = MonthDay.from(start);
        LocalDate candidate = atYearSafe(monthDay, today.getYear());
        if (candidate.isBefore(today)) {
            candidate = atYearSafe(monthDay, today.getYear() + 1);
        }
        long daysUntil = ChronoUnit.DAYS.between(today, candidate);
        int years = candidate.getYear() - start.getYear();
        return new AnniversaryInfo(candidate, daysUntil, years);
    }

    private LocalDate atYearSafe(MonthDay monthDay, int year) {
        // MonthDay.atYear already maps Feb 29 to Feb 28 on non-leap years.
        return monthDay.atYear(year);
    }
}
