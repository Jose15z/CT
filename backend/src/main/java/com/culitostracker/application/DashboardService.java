package com.culitostracker.application;

import com.culitostracker.api.dto.AdviceDtos.AdviceItem;
import com.culitostracker.api.dto.CheckInDtos.CheckInResponse;
import com.culitostracker.api.dto.DashboardDtos.DashboardCycleSummary;
import com.culitostracker.api.dto.DashboardDtos.DashboardPartner;
import com.culitostracker.api.dto.DashboardDtos.DashboardResponse;
import com.culitostracker.api.dto.ObservationDtos.ObservationResponse;
import com.culitostracker.api.dto.PartnerDtos;
import com.culitostracker.api.dto.UserDtos.UserResponse;
import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.model.User;
import com.culitostracker.repository.CycleProfileRepository;
import com.culitostracker.repository.PartnerObservationRepository;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.PeriodRecordRepository;
import com.culitostracker.repository.RelationshipCheckInRepository;
import com.culitostracker.repository.RelationshipRepository;
import com.culitostracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Composes the "today with your partner(s)" view in a single request. */
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final RelationshipRepository relationshipRepository;
    private final CycleProfileRepository cycleProfileRepository;
    private final PeriodRecordRepository periodRecordRepository;
    private final RelationshipCheckInRepository checkInRepository;
    private final PartnerObservationRepository observationRepository;
    private final PartnerAccessService partnerAccessService;
    private final PartnerService partnerService;
    private final AdviceService adviceService;
    private final com.culitostracker.domain.service.CyclePredictionService predictionService;

    public DashboardService(UserRepository userRepository,
                            PartnerRepository partnerRepository,
                            RelationshipRepository relationshipRepository,
                            CycleProfileRepository cycleProfileRepository,
                            PeriodRecordRepository periodRecordRepository,
                            RelationshipCheckInRepository checkInRepository,
                            PartnerObservationRepository observationRepository,
                            PartnerAccessService partnerAccessService,
                            PartnerService partnerService,
                            AdviceService adviceService,
                            com.culitostracker.domain.service.CyclePredictionService predictionService) {
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.relationshipRepository = relationshipRepository;
        this.cycleProfileRepository = cycleProfileRepository;
        this.periodRecordRepository = periodRecordRepository;
        this.checkInRepository = checkInRepository;
        this.observationRepository = observationRepository;
        this.partnerAccessService = partnerAccessService;
        this.partnerService = partnerService;
        this.adviceService = adviceService;
        this.predictionService = predictionService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        LocalDate today = LocalDate.now();

        List<Partner> partners = partnerRepository
                .findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        List<DashboardPartner> cards = new ArrayList<>();
        for (Partner partner : partners) {
            Relationship relationship = relationshipRepository.findByPartnerId(partner.getId()).orElse(null);
            if (relationship == null || relationship.getStatus() == RelationshipStatus.ENDED) {
                continue;
            }
            cards.add(buildCard(userId, partner, relationship, today));
        }
        // Active first, then most serious, then newest.
        cards.sort(Comparator
                .comparing((DashboardPartner c) -> c.relationshipStatus() != RelationshipStatus.ACTIVE)
                .thenComparing(c -> !c.relationshipType().isSerious()));

        return new DashboardResponse(UserResponse.from(user), cards);
    }

    private DashboardPartner buildCard(UUID userId, Partner partner,
                                       Relationship relationship, LocalDate today) {
        PartnerDtos.RelationshipResponse rel =
                partnerService.toRelationshipResponse(relationship, today);

        DashboardCycleSummary cycle = cycleSummary(partner.getId(), today);

        CheckInResponse myCheckIn = checkInRepository
                .findByRelationshipIdAndAuthorUserIdAndCheckInDate(relationship.getId(), userId, today)
                .map(c -> CheckInResponse.from(c, partner.getId(), userId))
                .orElse(null);

        CheckInResponse partnerCheckIn = null;
        UUID counterpart = CheckInService.counterpartOf(partner, userId);
        if (counterpart != null
                && partnerAccessService.hasActiveGrant(partner.getId(), userId, AccessScope.CHECK_INS)) {
            partnerCheckIn = checkInRepository
                    .findByRelationshipIdAndAuthorUserIdAndCheckInDate(relationship.getId(), counterpart, today)
                    .map(c -> CheckInResponse.from(c, partner.getId(), userId))
                    .orElse(null);
        }

        ObservationResponse observation = observationRepository
                .findTopByPartnerIdAndObserverUserIdAndObservationDateGreaterThanEqualOrderByCreatedAtDesc(
                        partner.getId(), userId, today.minusDays(2))
                .map(ObservationResponse::from)
                .orElse(null);

        AdviceItem advice = adviceService.candidates(userId, partner.getId()).stream()
                .findFirst()
                .map(AdviceItem::from)
                .orElse(null);

        return new DashboardPartner(
                partner.getId(),
                partner.getName(),
                partner.getNickname(),
                partner.getAvatarEmoji(),
                relationship.getType(),
                relationship.getStatus(),
                rel.togetherSince(),
                rel.duration(),
                rel.nextAnniversary(),
                cycle,
                myCheckIn,
                partnerCheckIn,
                observation,
                advice);
    }

    private DashboardCycleSummary cycleSummary(UUID partnerId, LocalDate today) {
        var profile = cycleProfileRepository.findByPartnerId(partnerId).orElse(null);
        if (profile == null || !profile.isTrackingEnabled()) {
            return new DashboardCycleSummary(profile != null && profile.isTrackingEnabled(),
                    true, null, null, null, null);
        }
        var records = periodRecordRepository.findByCycleProfileIdOrderByStartDateDesc(profile.getId());
        return predictionService.predict(profile, records, today)
                .map(p -> new DashboardCycleSummary(true, false, p.currentPhase(),
                        p.currentCycleDay(), p.nextPeriodStart(), p.ovulationDate()))
                .orElse(new DashboardCycleSummary(true, true, null, null, null, null));
    }
}
