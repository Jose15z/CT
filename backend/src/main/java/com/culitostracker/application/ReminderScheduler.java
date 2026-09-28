package com.culitostracker.application;

import com.culitostracker.domain.model.CycleProfile;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.ReminderSend;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.AnniversaryInfo;
import com.culitostracker.domain.service.CyclePredictionService;
import com.culitostracker.domain.service.ReminderPlanner;
import com.culitostracker.domain.service.ReminderPlanner.Event;
import com.culitostracker.domain.service.ReminderPlanner.EventKind;
import com.culitostracker.domain.service.ReminderPlanner.Facts;
import com.culitostracker.domain.service.ReminderPlanner.Reminder;
import com.culitostracker.domain.service.RelationshipDurationCalculator;
import com.culitostracker.repository.CycleProfileRepository;
import com.culitostracker.repository.DatePlanRepository;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.PeriodRecordRepository;
import com.culitostracker.repository.PushSubscriptionRepository;
import com.culitostracker.repository.RelationshipCheckInRepository;
import com.culitostracker.repository.RelationshipRepository;
import com.culitostracker.repository.ReminderSendRepository;
import com.culitostracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hourly sweep over users with push subscriptions: gathers today's facts in
 * each user's own time zone and lets {@link ReminderPlanner} decide. The
 * reminder_sends table makes every (user, kind, day) idempotent, so a restart
 * or a double tick never sends twice.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final WebPushService webPushService;
    private final UserRepository userRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final ReminderSendRepository sendRepository;
    private final PartnerRepository partnerRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipCheckInRepository checkInRepository;
    private final DatePlanRepository datePlanRepository;
    private final CycleProfileRepository cycleProfileRepository;
    private final PeriodRecordRepository periodRecordRepository;
    private final CyclePredictionService predictionService;
    private final RelationshipDurationCalculator durationCalculator;
    private final ReminderPlanner planner;

    public ReminderScheduler(WebPushService webPushService,
                             UserRepository userRepository,
                             PushSubscriptionRepository subscriptionRepository,
                             ReminderSendRepository sendRepository,
                             PartnerRepository partnerRepository,
                             RelationshipRepository relationshipRepository,
                             RelationshipCheckInRepository checkInRepository,
                             DatePlanRepository datePlanRepository,
                             CycleProfileRepository cycleProfileRepository,
                             PeriodRecordRepository periodRecordRepository,
                             CyclePredictionService predictionService,
                             RelationshipDurationCalculator durationCalculator,
                             ReminderPlanner planner) {
        this.webPushService = webPushService;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.sendRepository = sendRepository;
        this.partnerRepository = partnerRepository;
        this.relationshipRepository = relationshipRepository;
        this.checkInRepository = checkInRepository;
        this.datePlanRepository = datePlanRepository;
        this.cycleProfileRepository = cycleProfileRepository;
        this.periodRecordRepository = periodRecordRepository;
        this.predictionService = predictionService;
        this.durationCalculator = durationCalculator;
        this.planner = planner;
    }

    /** Five past every hour: cheap, and every local hour is reached once. */
    @Scheduled(cron = "0 5 * * * *")
    public void tick() {
        if (!webPushService.enabled()) {
            return;
        }
        for (UUID userId : subscriptionRepository.findDistinctUserIds()) {
            try {
                runFor(userId, ZonedDateTime.now());
            } catch (RuntimeException e) {
                log.warn("Reminder sweep failed for user {}: {}", userId, e.getMessage());
            }
        }
    }

    /** One user, one instant; returns how many reminders went out. */
    @Transactional
    public int runFor(UUID userId, ZonedDateTime now) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isRemindersEnabled()) {
            return 0;
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(user.getTimezone());
        } catch (RuntimeException e) {
            zone = ZoneId.of("UTC");
        }
        ZonedDateTime local = now.withZoneSameInstant(zone);
        LocalDate today = local.toLocalDate();
        int hour = local.getHour();
        if (hour != ReminderPlanner.MORNING_HOUR && hour != ReminderPlanner.EVENING_HOUR) {
            return 0;
        }

        Map<UUID, Relationship> active = relationshipRepository.findActiveByOwner(userId).stream()
                .collect(Collectors.toMap(Relationship::getPartnerId, Function.identity()));
        boolean checkedInToday = !checkInRepository
                .findByAuthorUserIdAndCheckInDateBetween(userId, today, today).isEmpty();

        List<Event> events = new ArrayList<>();
        datePlanRepository.findByOwnerUserIdAndDateBetweenOrderByDateAscStartTimeAsc(
                        userId, today, today.plusDays(1))
                .forEach(plan -> events.add(new Event(EventKind.DATE_PLAN, plan.getId().toString(),
                        plan.getTitle(), plan.getDate(), (int) ChronoUnit.DAYS.between(today, plan.getDate()))));

        for (Partner partner : partnerRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)) {
            Relationship relationship = active.get(partner.getId());
            if (relationship == null) {
                continue;
            }
            String name = partner.getNickname() != null ? partner.getNickname() : partner.getName();
            if (partner.getBirthDate() != null) {
                AnniversaryInfo b = durationCalculator.nextAnniversary(partner.getBirthDate(), today);
                events.add(new Event(EventKind.BIRTHDAY, partner.getId().toString(), name, b.date(), (int) b.daysUntil()));
            }
            LocalDate base = relationship.getMarriageDate() != null
                    ? relationship.getMarriageDate() : relationship.togetherSince();
            if (base != null && base.isBefore(today)) {
                AnniversaryInfo a = durationCalculator.nextAnniversary(base, today);
                if (a.years() >= 1) {
                    events.add(new Event(EventKind.ANNIVERSARY, partner.getId().toString(), name, a.date(), (int) a.daysUntil()));
                }
            }
            CycleProfile profile = cycleProfileRepository.findByPartnerId(partner.getId()).orElse(null);
            if (profile != null && profile.isTrackingEnabled()) {
                var records = periodRecordRepository.findByCycleProfileIdOrderByStartDateDesc(profile.getId());
                predictionService.predict(profile, records, today)
                        .map(p -> p.nextPeriodStart())
                        .filter(next -> next != null && !next.isBefore(today))
                        .ifPresent(next -> events.add(new Event(EventKind.PERIOD, partner.getId().toString(),
                                name, next, (int) ChronoUnit.DAYS.between(today, next))));
            }
        }

        Facts facts = new Facts(user.getPreferredLanguage(), today, hour, !active.isEmpty(), checkedInToday, events);
        int sent = 0;
        for (Reminder reminder : planner.plan(facts)) {
            ReminderSend.Key key = new ReminderSend.Key(userId, reminder.key(), today);
            if (sendRepository.existsById(key)) {
                continue;
            }
            if (webPushService.send(userId, reminder.title(), reminder.body(), reminder.url()) > 0) {
                sendRepository.save(new ReminderSend(userId, reminder.key(), today));
                sent++;
            }
        }
        return sent;
    }
}
