package com.culitostracker.domain.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RelationshipDurationCalculatorTest {

    private final RelationshipDurationCalculator calculator = new RelationshipDurationCalculator();

    @Test
    void computesYearsMonthsDays() {
        RelationshipDuration d = calculator.durationBetween(
                LocalDate.of(2024, 2, 14), LocalDate.of(2026, 6, 26));
        assertThat(d.years()).isEqualTo(2);
        assertThat(d.months()).isEqualTo(4);
        assertThat(d.days()).isEqualTo(12);
        assertThat(d.totalMonths()).isEqualTo(28);
    }

    @Test
    void zeroDurationOnStartDay() {
        RelationshipDuration d = calculator.durationBetween(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
        assertThat(d.totalDays()).isZero();
    }

    @Test
    void startInFutureClampsToZero() {
        RelationshipDuration d = calculator.durationBetween(
                LocalDate.of(2027, 1, 1), LocalDate.of(2026, 1, 1));
        assertThat(d.totalDays()).isZero();
    }

    @Test
    void nextAnniversaryLaterThisYear() {
        AnniversaryInfo a = calculator.nextAnniversary(
                LocalDate.of(2024, 2, 14), LocalDate.of(2026, 9, 21));
        assertThat(a.date()).isEqualTo(LocalDate.of(2027, 2, 14));
        assertThat(a.years()).isEqualTo(3);
        assertThat(a.daysUntil()).isEqualTo(146);
    }

    @Test
    void anniversaryTodayCountsAsToday() {
        AnniversaryInfo a = calculator.nextAnniversary(
                LocalDate.of(2024, 2, 14), LocalDate.of(2026, 2, 14));
        assertThat(a.daysUntil()).isZero();
        assertThat(a.years()).isEqualTo(2);
    }

    @Test
    void feb29CelebratedOnFeb28InNonLeapYears() {
        AnniversaryInfo a = calculator.nextAnniversary(
                LocalDate.of(2024, 2, 29), LocalDate.of(2025, 1, 15));
        assertThat(a.date()).isEqualTo(LocalDate.of(2025, 2, 28));
        assertThat(a.years()).isEqualTo(1);
    }
}
