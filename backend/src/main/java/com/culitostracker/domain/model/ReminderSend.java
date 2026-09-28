package com.culitostracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** One row per (user, reminder kind, day): the scheduler's idempotency key. */
@Entity
@Table(name = "reminder_sends")
public class ReminderSend {

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "user_id", nullable = false)
        private UUID userId;
        @Column(nullable = false, length = 80)
        private String kind;
        @Column(nullable = false)
        private LocalDate day;

        protected Key() {
        }

        public Key(UUID userId, String kind, LocalDate day) {
            this.userId = userId;
            this.kind = kind;
            this.day = day;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.userId.equals(userId) && k.kind.equals(kind) && k.day.equals(day);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, kind, day);
        }
    }

    @EmbeddedId
    private Key key;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected ReminderSend() {
    }

    public ReminderSend(UUID userId, String kind, LocalDate day) {
        this.key = new Key(userId, kind, day);
    }

    public Key getKey() { return key; }
    public Instant getSentAt() { return sentAt; }
}
