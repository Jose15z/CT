package com.culitostracker.domain.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides which reminders a user should get at a given local hour. Pure and
 * deterministic: the scheduler gathers facts, this turns them into messages.
 *
 * Evening (20h): check-in nudge when nothing was recorded today.
 * Morning (9h): today's and tomorrow's dates, birthdays and anniversaries
 * (today or in a week) and an estimated period starting in two days.
 */
@Service
public class ReminderPlanner {

    public static final int MORNING_HOUR = 9;
    public static final int EVENING_HOUR = 20;

    public enum EventKind { DATE_PLAN, BIRTHDAY, ANNIVERSARY, PERIOD }

    /** A dated fact about one partner: {@code label} is the plan title or the partner's name. */
    public record Event(EventKind kind, String id, String label, LocalDate date, int daysUntil) {
    }

    public record Facts(String language,
                        LocalDate today,
                        int localHour,
                        boolean hasActiveRelationship,
                        boolean checkedInToday,
                        List<Event> events) {
    }

    /** {@code key} dedupes sends per day: one nudge, one message per event. */
    public record Reminder(String key, String title, String body, String url) {
    }

    public List<Reminder> plan(Facts facts) {
        List<Reminder> out = new ArrayList<>();
        boolean en = facts.language() != null && facts.language().startsWith("en");

        if (facts.localHour() == EVENING_HOUR && facts.hasActiveRelationship() && !facts.checkedInToday()) {
            out.add(new Reminder("checkIn",
                    en ? "How was today?" : "¿Cómo fue hoy?",
                    en ? "Your check-in takes ten seconds and keeps the advice honest."
                            : "Tu check-in tarda diez segundos y mantiene los consejos al día.",
                    "/check-in"));
        }

        if (facts.localHour() != MORNING_HOUR) {
            return out;
        }
        for (Event event : facts.events()) {
            switch (event.kind()) {
                case DATE_PLAN -> {
                    if (event.daysUntil() == 0) {
                        out.add(new Reminder("datePlan:" + event.id() + ":0",
                                en ? "Today: " + event.label() : "Hoy: " + event.label(),
                                en ? "You have a date scheduled for today." : "Tienes una cita agendada para hoy.",
                                "/calendar"));
                    } else if (event.daysUntil() == 1) {
                        out.add(new Reminder("datePlan:" + event.id() + ":1",
                                en ? "Tomorrow: " + event.label() : "Mañana: " + event.label(),
                                en ? "A date is coming up tomorrow." : "Mañana tienes una cita agendada.",
                                "/calendar"));
                    }
                }
                case BIRTHDAY -> {
                    if (event.daysUntil() == 0) {
                        out.add(new Reminder("birthday:" + event.id() + ":0",
                                en ? event.label() + "'s birthday is today" : "Hoy es el cumpleaños de " + event.label(),
                                en ? "Make yours the first message." : "Que tu felicitación sea la primera.",
                                "/dashboard"));
                    } else if (event.daysUntil() == 7) {
                        out.add(new Reminder("birthday:" + event.id() + ":7",
                                en ? event.label() + "'s birthday in a week" : "Cumpleaños de " + event.label() + " en una semana",
                                en ? "Time to check the gift ideas you saved." : "Buen momento para revisar las ideas de regalo guardadas.",
                                "/partners"));
                    }
                }
                case ANNIVERSARY -> {
                    if (event.daysUntil() == 0) {
                        out.add(new Reminder("anniversary:" + event.id() + ":0",
                                en ? "Anniversary with " + event.label() : "Aniversario con " + event.label(),
                                en ? "Today is the day." : "Hoy es el día.",
                                "/dashboard"));
                    } else if (event.daysUntil() == 7) {
                        out.add(new Reminder("anniversary:" + event.id() + ":7",
                                en ? "Anniversary with " + event.label() + " in a week" : "Aniversario con " + event.label() + " en una semana",
                                en ? "If you want to book something, this is the week." : "Si quieres reservar algo, esta es la semana.",
                                "/dashboard"));
                    }
                }
                case PERIOD -> {
                    if (event.daysUntil() == 2) {
                        out.add(new Reminder("period:" + event.id() + ":2",
                                en ? "Estimate: " + event.label() + "'s period in 2 days" : "Estimación: periodo de " + event.label() + " en 2 días",
                                en ? "Based on the cycles you logged. It's an estimate, not a certainty."
                                        : "Según los ciclos registrados. Es una estimación, no una certeza.",
                                "/calendar"));
                    }
                }
            }
        }
        return out;
    }
}
