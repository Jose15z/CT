package com.culitostracker.application;

import com.culitostracker.api.dto.CheckInDtos.CheckInResponse;
import com.culitostracker.api.dto.CheckInDtos.CreateCheckInRequest;
import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipCheckIn;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.repository.RelationshipCheckInRepository;
import com.culitostracker.repository.RelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CheckInService {

    private final RelationshipCheckInRepository checkInRepository;
    private final RelationshipRepository relationshipRepository;
    private final PartnerAccessService partnerAccessService;

    public CheckInService(RelationshipCheckInRepository checkInRepository,
                          RelationshipRepository relationshipRepository,
                          PartnerAccessService partnerAccessService) {
        this.checkInRepository = checkInRepository;
        this.relationshipRepository = relationshipRepository;
        this.partnerAccessService = partnerAccessService;
    }

    /** Both participants (owner and linked account) can check in about the relationship. */
    @Transactional
    public CheckInResponse create(UUID userId, CreateCheckInRequest request) {
        Partner partner = partnerAccessService.requireParticipant(request.partnerId(), userId);
        Relationship relationship = relationshipRepository.findByPartnerId(partner.getId())
                .orElseThrow(() -> new NotFoundException("Relationship not found"));

        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new DomainRuleException("checkIn.futureDate", "Cannot check in for a future date");
        }

        // One check-in per author per day: checking in again replaces it.
        RelationshipCheckIn checkIn = checkInRepository
                .findByRelationshipIdAndAuthorUserIdAndCheckInDate(relationship.getId(), userId, date)
                .orElseGet(() -> {
                    RelationshipCheckIn c = new RelationshipCheckIn();
                    c.setRelationshipId(relationship.getId());
                    c.setAuthorUserId(userId);
                    c.setCheckInDate(date);
                    return c;
                });
        checkIn.setMood(request.mood());
        checkIn.setEnergyLevel(request.energyLevel());
        checkIn.setStressLevel(request.stressLevel());
        checkIn.setAffectionLevel(request.affectionLevel());
        checkIn.setRelationshipSatisfaction(request.relationshipSatisfaction());
        checkIn.setNote(request.note());
        return CheckInResponse.from(checkInRepository.save(checkIn), partner.getId(), userId);
    }

    /**
     * The viewer always sees their own check-ins. The other participant's
     * appear only when that person granted CHECK_INS access — consent, checked
     * here in the backend, not in the UI.
     */
    @Transactional(readOnly = true)
    public List<CheckInResponse> list(UUID userId, UUID partnerId) {
        Partner partner = partnerAccessService.requireParticipant(partnerId, userId);
        Relationship relationship = relationshipRepository.findByPartnerId(partner.getId())
                .orElseThrow(() -> new NotFoundException("Relationship not found"));

        List<RelationshipCheckIn> visible = new ArrayList<>(checkInRepository
                .findTop30ByRelationshipIdAndAuthorUserIdOrderByCheckInDateDesc(relationship.getId(), userId));

        UUID counterpart = counterpartOf(partner, userId);
        if (counterpart != null && partnerAccessService.hasActiveGrant(partnerId, userId, AccessScope.CHECK_INS)) {
            visible.addAll(checkInRepository
                    .findTop30ByRelationshipIdAndAuthorUserIdOrderByCheckInDateDesc(relationship.getId(), counterpart));
        }
        return visible.stream()
                .sorted(Comparator.comparing(RelationshipCheckIn::getCheckInDate).reversed())
                .map(c -> CheckInResponse.from(c, partnerId, userId))
                .toList();
    }

    static UUID counterpartOf(Partner partner, UUID userId) {
        return userId.equals(partner.getOwnerUserId())
                ? partner.getLinkedUserId()
                : partner.getOwnerUserId();
    }
}
