package com.culitostracker.application;

import com.culitostracker.api.dto.TrendsDtos.MonthCount;
import com.culitostracker.api.dto.TrendsDtos.TrendDay;
import com.culitostracker.api.dto.TrendsDtos.TrendsResponse;
import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.Mood;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipCheckIn;
import com.culitostracker.repository.EncounterRepository;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.RelationshipCheckInRepository;
import com.culitostracker.repository.RelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Time series for the trends page. The partner's series comes only from
 * linked accounts that granted CHECK_INS; nothing is inferred or invented.
 */
@Service
public class TrendsService {

    static final int MAX_WEEKS = 26;

    private final RelationshipCheckInRepository checkInRepository;
    private final PartnerRepository partnerRepository;
    private final RelationshipRepository relationshipRepository;
    private final EncounterRepository encounterRepository;
    private final PartnerAccessService partnerAccessService;

    public TrendsService(RelationshipCheckInRepository checkInRepository,
                         PartnerRepository partnerRepository,
                         RelationshipRepository relationshipRepository,
                         EncounterRepository encounterRepository,
                         PartnerAccessService partnerAccessService) {
        this.checkInRepository = checkInRepository;
        this.partnerRepository = partnerRepository;
        this.relationshipRepository = relationshipRepository;
        this.encounterRepository = encounterRepository;
        this.partnerAccessService = partnerAccessService;
    }

    /** Moods on a 1–5 scale so they can share an axis with the levels. */
    static double moodScore(Mood mood) {
        return switch (mood) {
            case VERY_HAPPY -> 5;
            case HAPPY, AFFECTIONATE, CALM -> 4;
            case NEUTRAL, CUSTOM -> 3;
            case TIRED -> 2.5;
            case STRESSED, ANXIOUS -> 2;
            case SAD, OVERWHELMED -> 1.5;
            case ANGRY -> 1;
        };
    }

    @Transactional(readOnly = true)
    public TrendsResponse forUser(UUID userId, int weeks) {
        int span = Math.max(1, Math.min(weeks, MAX_WEEKS));
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusWeeks(span).plusDays(1);

        // Mine: every relationship I check in about, averaged per day.
        Map<LocalDate, List<RelationshipCheckIn>> mine = checkInRepository
                .findByAuthorUserIdAndCheckInDateBetween(userId, from, to).stream()
                .collect(Collectors.groupingBy(RelationshipCheckIn::getCheckInDate));

        // Theirs: linked partners who share their check-ins with me.
        Map<LocalDate, List<RelationshipCheckIn>> theirs = new TreeMap<>();
        for (Partner partner : partnerRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)) {
            UUID counterpart = partner.getLinkedUserId();
            if (counterpart == null
                    || !partnerAccessService.hasActiveGrant(partner.getId(), userId, AccessScope.CHECK_INS)) {
                continue;
            }
            Relationship relationship = relationshipRepository.findByPartnerId(partner.getId()).orElse(null);
            if (relationship == null) {
                continue;
            }
            checkInRepository.findByRelationshipIdAndAuthorUserIdAndCheckInDateBetween(
                            relationship.getId(), counterpart, from, to)
                    .forEach(c -> theirs.computeIfAbsent(c.getCheckInDate(), d -> new ArrayList<>()).add(c));
        }

        List<TrendDay> days = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            List<RelationshipCheckIn> m = mine.getOrDefault(day, List.of());
            List<RelationshipCheckIn> p = theirs.getOrDefault(day, List.of());
            days.add(new TrendDay(day,
                    avg(m, c -> moodScore(c.getMood())),
                    avg(m, c -> (double) c.getEnergyLevel()),
                    avg(m, c -> (double) c.getStressLevel()),
                    avg(m.stream().filter(c -> c.getRelationshipSatisfaction() != null).toList(),
                            c -> (double) c.getRelationshipSatisfaction()),
                    avg(p, c -> moodScore(c.getMood()))));
        }

        int myCount = mine.values().stream().mapToInt(List::size).sum();
        int theirCount = theirs.values().stream().mapToInt(List::size).sum();
        Double moodAvg = avg(days.stream().filter(d -> d.myMood() != null).toList(), TrendDay::myMood);
        Double satisfactionAvg = avg(days.stream().filter(d -> d.mySatisfaction() != null).toList(),
                TrendDay::mySatisfaction);

        // Encounters per calendar month, last six months, zero-filled.
        YearMonth thisMonth = YearMonth.from(to);
        LocalDate monthsFrom = thisMonth.minusMonths(5).atDay(1);
        Map<YearMonth, Long> perMonth = encounterRepository
                .findByOwnerUserIdAndDateBetweenOrderByDateAscCreatedAtAsc(userId, monthsFrom, to).stream()
                .collect(Collectors.groupingBy(e -> YearMonth.from(e.getDate()), TreeMap::new, Collectors.counting()));
        List<MonthCount> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = thisMonth.minusMonths(i);
            months.add(new MonthCount(ym.toString(), perMonth.getOrDefault(ym, 0L)));
        }

        return new TrendsResponse(from, to, myCount, theirCount, moodAvg, satisfactionAvg, days, months);
    }

    private static <T> Double avg(List<T> items, java.util.function.ToDoubleFunction<T> f) {
        if (items.isEmpty()) {
            return null;
        }
        double sum = 0;
        for (T item : items) {
            sum += f.applyAsDouble(item);
        }
        return Math.round(sum / items.size() * 100.0) / 100.0;
    }
}
