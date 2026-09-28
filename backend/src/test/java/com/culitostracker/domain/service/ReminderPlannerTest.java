package com.culitostracker.domain.service;

import com.culitostracker.domain.service.ReminderPlanner.Event;
import com.culitostracker.domain.service.ReminderPlanner.EventKind;
import com.culitostracker.domain.service.ReminderPlanner.Facts;
import com.culitostracker.domain.service.ReminderPlanner.Reminder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderPlannerTest {

    private final ReminderPlanner planner = new ReminderPlanner();
    private final LocalDate today = LocalDate.of(2026, 10, 1);

    private Facts facts(int hour, boolean active, boolean checkedIn, List<Event> events) {
        return new Facts("es", today, hour, active, checkedIn, events);
    }

    @Test
    void eveningNudgeOnlyWhenNothingWasRecorded() {
        assertThat(planner.plan(facts(20, true, false, List.of())))
                .extracting(Reminder::key).containsExactly("checkIn");
        assertThat(planner.plan(facts(20, true, true, List.of()))).isEmpty();
        assertThat(planner.plan(facts(20, false, false, List.of()))).isEmpty();
        // The nudge is an evening thing; mornings never nag.
        assertThat(planner.plan(facts(9, true, false, List.of()))).isEmpty();
    }

    @Test
    void morningCoversDatesBirthdaysAnniversariesAndPeriods() {
        List<Event> events = List.of(
                new Event(EventKind.DATE_PLAN, "p1", "Cena", today, 0),
                new Event(EventKind.DATE_PLAN, "p2", "Cine", today.plusDays(1), 1),
                new Event(EventKind.BIRTHDAY, "a", "Laura", today.plusDays(7), 7),
                new Event(EventKind.ANNIVERSARY, "a", "Laura", today, 0),
                new Event(EventKind.PERIOD, "a", "Laura", today.plusDays(2), 2),
                // Not due: birthday in 3 days, period in 5.
                new Event(EventKind.BIRTHDAY, "b", "Ana", today.plusDays(3), 3),
                new Event(EventKind.PERIOD, "b", "Ana", today.plusDays(5), 5));
        List<Reminder> plan = planner.plan(facts(9, true, false, events));
        assertThat(plan).extracting(Reminder::key).containsExactly(
                "datePlan:p1:0", "datePlan:p2:1", "birthday:a:7", "anniversary:a:0", "period:a:2");
        assertThat(plan.get(0).title()).isEqualTo("Hoy: Cena");
        assertThat(plan.get(4).body()).contains("estimación");
    }

    @Test
    void outsideTheTwoHoursNothingFires() {
        List<Event> events = List.of(new Event(EventKind.DATE_PLAN, "p1", "Cena", today, 0));
        assertThat(planner.plan(facts(12, true, false, events))).isEmpty();
    }

    @Test
    void speaksTheUsersLanguage() {
        Facts en = new Facts("en", today, 20, true, false, List.of());
        assertThat(planner.plan(en).get(0).title()).isEqualTo("How was today?");
    }
}
