package com.culitostracker.application;

import com.culitostracker.api.dto.AdviceDtos.AdviceItem;
import com.culitostracker.api.dto.AdviceDtos.AdviceResponse;
import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.CycleProfile;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.PartnerObservation;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipCheckIn;
import com.culitostracker.domain.model.RelationshipStage;
import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.service.AdviceCandidate;
import com.culitostracker.domain.service.AdviceContext;
import com.culitostracker.domain.service.AnniversaryInfo;
import com.culitostracker.domain.service.CyclePredictionService;
import com.culitostracker.domain.service.RelationshipAdviceEngine;
import com.culitostracker.domain.service.RelationshipDurationCalculator;
import com.culitostracker.repository.CycleProfileRepository;
import com.culitostracker.repository.DailyTipRepository;
import com.culitostracker.repository.PartnerObservationRepository;
import com.culitostracker.repository.PeriodRecordRepository;
import com.culitostracker.repository.RelationshipCheckInRepository;
import com.culitostracker.repository.RelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Builds the {@link AdviceContext} from persisted signals and delegates to the
 * pure rule engine. Signals are separated by origin: self-reports, the
 * partner's own shared check-ins (only with an active consent grant) and the
 * user's observations. Nothing is invented.
 */
@Service
public class AdviceService {

    private final PartnerAccessService partnerAccessService;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipCheckInRepository checkInRepository;
    private final PartnerObservationRepository observationRepository;
    private final CycleProfileRepository cycleProfileRepository;
    private final PeriodRecordRepository periodRecordRepository;
    private final DailyTipRepository dailyTipRepository;
    private final CyclePredictionService predictionService;
    private final RelationshipDurationCalculator durationCalculator;
    private final RelationshipAdviceEngine adviceEngine;

    public AdviceService(PartnerAccessService partnerAccessService,
                         RelationshipRepository relationshipRepository,
                         RelationshipCheckInRepository checkInRepository,
                         PartnerObservationRepository observationRepository,
                         CycleProfileRepository cycleProfileRepository,
                         PeriodRecordRepository periodRecordRepository,
                         DailyTipRepository dailyTipRepository,
                         CyclePredictionService predictionService,
                         RelationshipDurationCalculator durationCalculator,
                         RelationshipAdviceEngine adviceEngine) {
        this.partnerAccessService = partnerAccessService;
        this.relationshipRepository = relationshipRepository;
        this.checkInRepository = checkInRepository;
        this.observationRepository = observationRepository;
        this.cycleProfileRepository = cycleProfileRepository;
        this.periodRecordRepository = periodRecordRepository;
        this.dailyTipRepository = dailyTipRepository;
        this.predictionService = predictionService;
        this.durationCalculator = durationCalculator;
        this.adviceEngine = adviceEngine;
    }

    @Transactional(readOnly = true)
    public AdviceResponse today(UUID userId, UUID partnerId) {
        List<AdviceCandidate> candidates = candidates(userId, partnerId);
        return new AdviceResponse(candidates.stream().map(AdviceItem::from).toList());
    }

    @Transactional(readOnly = true)
    public List<AdviceCandidate> candidates(UUID userId, UUID partnerId) {
        Partner partner = partnerAccessService.requireOwned(partnerId, userId);
        Relationship relationship = relationshipRepository.findByPartnerId(partnerId).orElse(null);
        LocalDate today = LocalDate.now();

        RelationshipStage stage = null;
        Long monthsTogether = null;
        Integer daysUntilAnniversary = null;
        Integer anniversaryYears = null;
        if (relationship != null && relationship.togetherSince() != null) {
            LocalDate since = relationship.togetherSince();
            monthsTogether = ChronoUnit.MONTHS.between(since, today);
            stage = RelationshipStage.fromMonths(monthsTogether);
            if (relationship.getStatus() == RelationshipStatus.ACTIVE) {
                LocalDate base = relationship.getMarriageDate() != null
                        ? relationship.getMarriageDate() : since;
                AnniversaryInfo anniversary = durationCalculator.nextAnniversary(base, today);
                daysUntilAnniversary = (int) anniversary.daysUntil();
                anniversaryYears = anniversary.years();
            }
        }

        CyclePhase phase = null;
        CycleProfile profile = cycleProfileRepository.findByPartnerId(partnerId).orElse(null);
        if (profile != null && profile.isTrackingEnabled()) {
            var records = periodRecordRepository.findByCycleProfileIdOrderByStartDateDesc(profile.getId());
            phase = predictionService.predict(profile, records, today)
                    .map(p -> p.currentPhase())
                    .orElse(null);
        }

        RelationshipCheckIn myCheckIn = null;
        RelationshipCheckIn partnerCheckIn = null;
        if (relationship != null) {
            myCheckIn = checkInRepository
                    .findTopByRelationshipIdAndAuthorUserIdAndCheckInDateGreaterThanEqualOrderByCheckInDateDesc(
                            relationship.getId(), userId, today.minusDays(1))
                    .orElse(null);
            UUID counterpart = CheckInService.counterpartOf(partner, userId);
            if (counterpart != null
                    && partnerAccessService.hasActiveGrant(partnerId, userId, AccessScope.CHECK_INS)) {
                partnerCheckIn = checkInRepository
                        .findTopByRelationshipIdAndAuthorUserIdAndCheckInDateGreaterThanEqualOrderByCheckInDateDesc(
                                relationship.getId(), counterpart, today.minusDays(1))
                        .orElse(null);
            }
        }

        PartnerObservation observation = observationRepository
                .findTopByPartnerIdAndObserverUserIdAndObservationDateGreaterThanEqualOrderByCreatedAtDesc(
                        partnerId, userId, today.minusDays(2))
                .orElse(null);

        AdviceContext context = new AdviceContext(
                partnerId,
                partner.getNickname() != null ? partner.getNickname() : partner.getName(),
                relationship != null ? relationship.getType() : null,
                stage,
                monthsTogether,
                daysUntilAnniversary,
                anniversaryYears,
                phase,
                myCheckIn != null ? myCheckIn.getMood() : null,
                myCheckIn != null ? myCheckIn.getStressLevel() : null,
                myCheckIn != null ? myCheckIn.getEnergyLevel() : null,
                partnerCheckIn != null ? partnerCheckIn.getMood() : null,
                partnerCheckIn != null ? partnerCheckIn.getStressLevel() : null,
                observation != null ? observation.getObservationType() : null,
                stage != null ? dailyTipRepository.findByRelationshipStageAndActiveTrue(stage) : List.of(),
                phase != null ? dailyTipRepository.findByCyclePhaseAndActiveTrue(phase) : List.of(),
                dailyTipRepository.findByRelationshipStageIsNullAndCyclePhaseIsNullAndActiveTrue(),
                today);

        return adviceEngine.advise(context);
    }
}
