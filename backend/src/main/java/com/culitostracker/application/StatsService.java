package com.culitostracker.application;

import com.culitostracker.api.dto.PartnerDtos.DurationDto;
import com.culitostracker.api.dto.StatsDtos.StatsResponse;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.RelationshipDuration;
import com.culitostracker.domain.service.RelationshipDurationCalculator;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.RelationshipMilestoneRepository;
import com.culitostracker.repository.RelationshipRepository;
import com.culitostracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Personal statistics. Private by default: the API only serves them to their
 * owner. The only public surface is the leaderboard, which is opt-in.
 */
@Service
public class StatsService {

    private final PartnerRepository partnerRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipMilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final LeaderboardService leaderboardService;
    private final RelationshipDurationCalculator durationCalculator;

    public StatsService(PartnerRepository partnerRepository,
                        RelationshipRepository relationshipRepository,
                        RelationshipMilestoneRepository milestoneRepository,
                        UserRepository userRepository,
                        LeaderboardService leaderboardService,
                        RelationshipDurationCalculator durationCalculator) {
        this.partnerRepository = partnerRepository;
        this.relationshipRepository = relationshipRepository;
        this.milestoneRepository = milestoneRepository;
        this.userRepository = userRepository;
        this.leaderboardService = leaderboardService;
        this.durationCalculator = durationCalculator;
    }

    @Transactional(readOnly = true)
    public StatsResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        LocalDate today = LocalDate.now();

        long registered = partnerRepository.countByOwnerUserIdAndDeletedAtIsNull(userId);
        long unique = leaderboardService.scoreOf(userId);
        List<Relationship> relationships = relationshipRepository.findAllByOwner(userId);

        long active = relationships.stream()
                .filter(r -> r.getStatus() == RelationshipStatus.ACTIVE)
                .count();
        long serious = relationships.stream().filter(r -> r.getType().isSerious()).count();
        long casual = relationships.size() - serious;

        DurationDto longest = null;
        String longestName = null;
        long longestDays = -1;
        for (Relationship r : relationships) {
            if (r.togetherSince() == null) {
                continue;
            }
            LocalDate until = r.getStatus() == RelationshipStatus.ENDED && r.getRelationshipEndDate() != null
                    ? r.getRelationshipEndDate() : today;
            RelationshipDuration d = durationCalculator.durationBetween(r.togetherSince(), until);
            if (d.totalDays() > longestDays) {
                longestDays = d.totalDays();
                longest = new DurationDto(d.years(), d.months(), d.days(), d.totalDays());
                longestName = partnerRepository.findById(r.getPartnerId())
                        .map(p -> p.getName())
                        .orElse(null);
            }
        }

        long milestones = milestoneRepository.countByOwner(userId);
        Integer rank = leaderboardService.me(userId).rank();

        return new StatsResponse(registered, unique, active, serious, casual,
                longest, longestName, milestones, user.getRelationshipSituation(), rank);
    }
}
