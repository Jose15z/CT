package com.culitostracker.application;

import com.culitostracker.api.dto.PartnerDtos.AnniversaryDto;
import com.culitostracker.api.dto.PartnerDtos.CreatePartnerRequest;
import com.culitostracker.api.dto.PartnerDtos.DurationDto;
import com.culitostracker.api.dto.PartnerDtos.PartnerResponse;
import com.culitostracker.api.dto.PartnerDtos.RelationshipResponse;
import com.culitostracker.api.dto.PartnerDtos.UpdatePartnerRequest;
import com.culitostracker.api.dto.PartnerDtos.UpdateRelationshipRequest;
import com.culitostracker.domain.model.CycleProfile;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.model.RelationshipType;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.AnniversaryInfo;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.domain.service.RelationshipDuration;
import com.culitostracker.domain.service.RelationshipDurationCalculator;
import com.culitostracker.domain.service.RelationshipRules;
import com.culitostracker.infrastructure.config.LimitsProperties;
import com.culitostracker.repository.CycleProfileRepository;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.RelationshipRepository;
import com.culitostracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final RelationshipRepository relationshipRepository;
    private final CycleProfileRepository cycleProfileRepository;
    private final UserRepository userRepository;
    private final PartnerAccessService partnerAccessService;
    private final RelationshipRules relationshipRules;
    private final RelationshipDurationCalculator durationCalculator;
    private final LimitsProperties limits;

    public PartnerService(PartnerRepository partnerRepository,
                          RelationshipRepository relationshipRepository,
                          CycleProfileRepository cycleProfileRepository,
                          UserRepository userRepository,
                          PartnerAccessService partnerAccessService,
                          RelationshipRules relationshipRules,
                          RelationshipDurationCalculator durationCalculator,
                          LimitsProperties limits) {
        this.partnerRepository = partnerRepository;
        this.relationshipRepository = relationshipRepository;
        this.cycleProfileRepository = cycleProfileRepository;
        this.userRepository = userRepository;
        this.partnerAccessService = partnerAccessService;
        this.relationshipRules = relationshipRules;
        this.durationCalculator = durationCalculator;
        this.limits = limits;
    }

    @Transactional
    public PartnerResponse create(UUID userId, CreatePartnerRequest request) {
        // Anti-cheat rate limit: creating partners is capped per rolling day.
        long createdToday = partnerRepository.countByOwnerUserIdAndCreatedAtAfter(
                userId, Instant.now().minus(1, ChronoUnit.DAYS));
        if (createdToday >= limits.partnersPerDay()) {
            throw new DomainRuleException("partner.rateLimit",
                    "Too many partners created today");
        }

        String normalized = Partner.normalizeName(request.name());
        if (partnerRepository.existsByOwnerUserIdAndNormalizedNameAndDeletedAtIsNull(userId, normalized)) {
            throw new DomainRuleException("partner.duplicate",
                    "An active partner with this name already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        relationshipRules.validateDates(request.datingStartDate(), request.relationshipStartDate(),
                request.engagementDate(), request.marriageDate(), null, RelationshipStatus.ACTIVE);
        List<Relationship> active = relationshipRepository.findActiveByOwner(userId);
        relationshipRules.validateExclusivity(request.relationshipType(), RelationshipStatus.ACTIVE,
                active, user.getRelationshipSituation());

        Partner partner = new Partner();
        partner.setOwnerUserId(userId);
        partner.rename(request.name().trim());
        partner.setNickname(request.nickname());
        partner.setNotes(request.notes());
        partner.setAvatarEmoji(request.avatarEmoji());
        partner = partnerRepository.save(partner);

        Relationship relationship = new Relationship();
        relationship.setPartnerId(partner.getId());
        relationship.setType(request.relationshipType());
        relationship.setStatus(RelationshipStatus.ACTIVE);
        relationship.setDatingStartDate(request.datingStartDate());
        relationship.setRelationshipStartDate(request.relationshipStartDate());
        relationship.setEngagementDate(request.engagementDate());
        relationship.setMarriageDate(request.marriageDate());
        relationship = relationshipRepository.save(relationship);

        CycleProfile cycleProfile = new CycleProfile();
        cycleProfile.setPartnerId(partner.getId());
        cycleProfileRepository.save(cycleProfile);

        return toResponse(partner, relationship, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<PartnerResponse> list(UUID userId) {
        LocalDate today = LocalDate.now();
        return partnerRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(p -> toResponse(p, relationshipRepository.findByPartnerId(p.getId()).orElse(null), today))
                .toList();
    }

    /** Includes ended and soft-deleted partners: the user's own history. */
    @Transactional(readOnly = true)
    public List<PartnerResponse> history(UUID userId) {
        LocalDate today = LocalDate.now();
        return partnerRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream()
                .map(p -> toResponse(p, relationshipRepository.findByPartnerId(p.getId()).orElse(null), today))
                .toList();
    }

    @Transactional(readOnly = true)
    public PartnerResponse get(UUID userId, UUID partnerId) {
        Partner partner = partnerAccessService.requireOwned(partnerId, userId);
        Relationship relationship = relationshipRepository.findByPartnerId(partnerId).orElse(null);
        return toResponse(partner, relationship, LocalDate.now());
    }

    @Transactional
    public PartnerResponse update(UUID userId, UUID partnerId, UpdatePartnerRequest request) {
        Partner partner = partnerAccessService.requireOwned(partnerId, userId);
        if (request.name() != null) {
            String normalized = Partner.normalizeName(request.name());
            boolean sameName = normalized.equals(partner.getNormalizedName());
            if (!sameName && partnerRepository
                    .existsByOwnerUserIdAndNormalizedNameAndDeletedAtIsNull(userId, normalized)) {
                throw new DomainRuleException("partner.duplicate",
                        "An active partner with this name already exists");
            }
            partner.rename(request.name().trim());
        }
        if (request.nickname() != null) {
            partner.setNickname(request.nickname().isBlank() ? null : request.nickname());
        }
        if (request.notes() != null) {
            partner.setNotes(request.notes().isBlank() ? null : request.notes());
        }
        if (request.avatarEmoji() != null) {
            partner.setAvatarEmoji(request.avatarEmoji().isBlank() ? null : request.avatarEmoji());
        }
        partner = partnerRepository.save(partner);
        Relationship relationship = relationshipRepository.findByPartnerId(partnerId).orElse(null);
        return toResponse(partner, relationship, LocalDate.now());
    }

    @Transactional
    public void softDelete(UUID userId, UUID partnerId) {
        Partner partner = partnerAccessService.requireOwned(partnerId, userId);
        partner.setDeletedAt(Instant.now());
        partnerRepository.save(partner);
        // The relationship record stays for history; mark it ended if active.
        relationshipRepository.findByPartnerId(partnerId).ifPresent(rel -> {
            if (rel.getStatus() != RelationshipStatus.ENDED) {
                rel.setStatus(RelationshipStatus.ENDED);
                if (rel.getRelationshipEndDate() == null) {
                    rel.setRelationshipEndDate(LocalDate.now());
                }
                relationshipRepository.save(rel);
            }
        });
    }

    @Transactional(readOnly = true)
    public RelationshipResponse getRelationship(UUID userId, UUID partnerId) {
        partnerAccessService.requireOwned(partnerId, userId);
        Relationship relationship = relationshipRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new NotFoundException("Relationship not found"));
        return toRelationshipResponse(relationship, LocalDate.now());
    }

    @Transactional
    public RelationshipResponse updateRelationship(UUID userId, UUID partnerId,
                                                   UpdateRelationshipRequest request) {
        partnerAccessService.requireOwned(partnerId, userId);
        Relationship relationship = relationshipRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new NotFoundException("Relationship not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        RelationshipType newType = request.type() != null ? request.type() : relationship.getType();
        RelationshipStatus newStatus = request.status() != null ? request.status() : relationship.getStatus();
        LocalDate dating = request.datingStartDate() != null ? request.datingStartDate() : relationship.getDatingStartDate();
        LocalDate start = request.relationshipStartDate() != null ? request.relationshipStartDate() : relationship.getRelationshipStartDate();
        LocalDate engagement = request.engagementDate() != null ? request.engagementDate() : relationship.getEngagementDate();
        LocalDate marriage = request.marriageDate() != null ? request.marriageDate() : relationship.getMarriageDate();
        LocalDate end = request.relationshipEndDate() != null ? request.relationshipEndDate() : relationship.getRelationshipEndDate();

        if (newStatus == RelationshipStatus.ENDED && end == null) {
            end = LocalDate.now();
        }
        if (newStatus == RelationshipStatus.ACTIVE) {
            end = null; // reactivating clears the end date
        }

        relationshipRules.validateDates(dating, start, engagement, marriage, end, newStatus);
        List<Relationship> others = relationshipRepository.findActiveByOwner(userId).stream()
                .filter(r -> !r.getId().equals(relationship.getId()))
                .toList();
        relationshipRules.validateExclusivity(newType, newStatus, others, user.getRelationshipSituation());

        relationship.setType(newType);
        relationship.setStatus(newStatus);
        relationship.setDatingStartDate(dating);
        relationship.setRelationshipStartDate(start);
        relationship.setEngagementDate(engagement);
        relationship.setMarriageDate(marriage);
        relationship.setRelationshipEndDate(end);
        Relationship saved = relationshipRepository.save(relationship);
        return toRelationshipResponse(saved, LocalDate.now());
    }

    PartnerResponse toResponse(Partner partner, Relationship relationship, LocalDate today) {
        return new PartnerResponse(
                partner.getId(),
                partner.getName(),
                partner.getNickname(),
                partner.getNotes(),
                partner.getAvatarEmoji(),
                partner.getLinkedUserId() != null,
                partner.isDeleted(),
                relationship == null ? null : toRelationshipResponse(relationship, today),
                partner.getCreatedAt());
    }

    RelationshipResponse toRelationshipResponse(Relationship r, LocalDate today) {
        LocalDate since = r.togetherSince();
        DurationDto duration = null;
        AnniversaryDto anniversary = null;
        if (since != null) {
            LocalDate until = r.getStatus() == RelationshipStatus.ENDED && r.getRelationshipEndDate() != null
                    ? r.getRelationshipEndDate() : today;
            RelationshipDuration d = durationCalculator.durationBetween(since, until);
            duration = new DurationDto(d.years(), d.months(), d.days(), d.totalDays());
            if (r.getStatus() != RelationshipStatus.ENDED) {
                // Married couples celebrate the wedding date; others the start date.
                LocalDate anniversaryBase = r.getMarriageDate() != null ? r.getMarriageDate() : since;
                AnniversaryInfo a = durationCalculator.nextAnniversary(anniversaryBase, today);
                anniversary = new AnniversaryDto(a.date(), a.daysUntil(), a.years());
            }
        }
        return new RelationshipResponse(r.getId(), r.getType(), r.getStatus(),
                r.getDatingStartDate(), r.getRelationshipStartDate(), r.getEngagementDate(),
                r.getMarriageDate(), r.getRelationshipEndDate(), since, duration, anniversary);
    }
}
