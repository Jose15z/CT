package com.culitostracker.application;

import com.culitostracker.api.dto.CheckInDtos.CheckInResponse;
import com.culitostracker.api.dto.CycleDtos.CycleProfileResponse;
import com.culitostracker.api.dto.CycleDtos.PeriodRecordResponse;
import com.culitostracker.api.dto.DatePlanDtos.DatePlanResponse;
import com.culitostracker.api.dto.EncounterDtos.EncounterResponse;
import com.culitostracker.api.dto.MilestoneDtos.MilestoneResponse;
import com.culitostracker.api.dto.ObservationDtos.ObservationResponse;
import com.culitostracker.api.dto.UserDtos.UserResponse;
import com.culitostracker.api.dto.WishlistDtos.WishResponse;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.repository.CycleProfileRepository;
import com.culitostracker.repository.DatePlanRepository;
import com.culitostracker.repository.EncounterRepository;
import com.culitostracker.repository.LeaderboardProfileRepository;
import com.culitostracker.repository.PartnerObservationRepository;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.PeriodRecordRepository;
import com.culitostracker.repository.RefreshTokenRepository;
import com.culitostracker.repository.RelationshipCheckInRepository;
import com.culitostracker.repository.RelationshipMilestoneRepository;
import com.culitostracker.repository.RelationshipRepository;
import com.culitostracker.repository.UserRepository;
import com.culitostracker.repository.WishlistRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data portability and the right to be forgotten. The export reuses the API
 * response shapes so it is readable and never leaks internals (no hashes,
 * no other users' data); deletion cascades through the schema's FKs.
 */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipMilestoneRepository milestoneRepository;
    private final CycleProfileRepository cycleProfileRepository;
    private final PeriodRecordRepository periodRecordRepository;
    private final RelationshipCheckInRepository checkInRepository;
    private final PartnerObservationRepository observationRepository;
    private final DatePlanRepository datePlanRepository;
    private final EncounterRepository encounterRepository;
    private final WishlistRepository wishlistRepository;
    private final LeaderboardProfileRepository leaderboardProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AvatarService avatarService;
    private final PartnerService partnerService;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository,
                          PartnerRepository partnerRepository,
                          RelationshipRepository relationshipRepository,
                          RelationshipMilestoneRepository milestoneRepository,
                          CycleProfileRepository cycleProfileRepository,
                          PeriodRecordRepository periodRecordRepository,
                          RelationshipCheckInRepository checkInRepository,
                          PartnerObservationRepository observationRepository,
                          DatePlanRepository datePlanRepository,
                          EncounterRepository encounterRepository,
                          WishlistRepository wishlistRepository,
                          LeaderboardProfileRepository leaderboardProfileRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          AvatarService avatarService,
                          PartnerService partnerService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.relationshipRepository = relationshipRepository;
        this.milestoneRepository = milestoneRepository;
        this.cycleProfileRepository = cycleProfileRepository;
        this.periodRecordRepository = periodRecordRepository;
        this.checkInRepository = checkInRepository;
        this.observationRepository = observationRepository;
        this.datePlanRepository = datePlanRepository;
        this.encounterRepository = encounterRepository;
        this.wishlistRepository = wishlistRepository;
        this.leaderboardProfileRepository = leaderboardProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.avatarService = avatarService;
        this.partnerService = partnerService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        LocalDate today = LocalDate.now();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", Instant.now());
        out.put("user", UserResponse.from(user, avatarService.exists(userId)));

        List<Partner> partners = partnerRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, String> names = partners.stream()
                .collect(Collectors.toMap(Partner::getId, Partner::getName, (a, b) -> a));
        List<Map<String, Object>> partnerDumps = new ArrayList<>();
        for (Partner partner : partners) {
            Map<String, Object> dump = new LinkedHashMap<>();
            Relationship relationship = relationshipRepository.findByPartnerId(partner.getId()).orElse(null);
            dump.put("partner", partnerService.toResponse(partner, relationship, today));
            dump.put("milestones", milestoneRepository.findByPartnerIdOrderByDateDesc(partner.getId()).stream()
                    .map(MilestoneResponse::from).toList());
            cycleProfileRepository.findByPartnerId(partner.getId()).ifPresent(profile -> {
                dump.put("cycleProfile", CycleProfileResponse.from(profile));
                dump.put("periods", periodRecordRepository
                        .findByCycleProfileIdOrderByStartDateDesc(profile.getId()).stream()
                        .map(PeriodRecordResponse::from).toList());
            });
            dump.put("wishlist", wishlistRepository.findByPartnerIdOrderByDoneAscCreatedAtDesc(partner.getId())
                    .stream().map(WishResponse::from).toList());
            partnerDumps.add(dump);
        }
        out.put("partners", partnerDumps);

        out.put("checkIns", checkInRepository.findByAuthorUserIdOrderByCheckInDateDesc(userId).stream()
                .map(c -> CheckInResponse.from(c,
                        relationshipRepository.findById(c.getRelationshipId())
                                .map(Relationship::getPartnerId).orElse(null),
                        userId))
                .toList());
        out.put("observations", observationRepository.findByObserverUserIdOrderByObservationDateDesc(userId)
                .stream().map(ObservationResponse::from).toList());
        out.put("datePlans", datePlanRepository.findByOwnerUserIdOrderByDateAsc(userId).stream()
                .map(plan -> DatePlanResponse.from(plan, names.get(plan.getPartnerId()))).toList());
        out.put("encounters", encounterRepository.findByOwnerUserIdOrderByDateAscCreatedAtAscIdAsc(userId)
                .stream().map(e -> EncounterResponse.from(e, names.get(e.getPartnerId()))).toList());
        leaderboardProfileRepository.findById(userId).ifPresent(profile -> out.put("leaderboardProfile",
                Map.of("enabled", profile.isEnabled(),
                        "publicAlias", profile.getPublicAlias() == null ? "" : profile.getPublicAlias(),
                        "showAvatar", profile.isShowAvatar())));
        return out;
    }

    /** Password re-check, then the FK cascades take everything the user owns. */
    @Transactional
    public void deleteAccount(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new DomainRuleException("auth.invalidCredentials", "Password does not match");
        }
        refreshTokenRepository.deleteByUserId(userId);
        avatarService.delete(userId);
        userRepository.delete(user);
    }
}
