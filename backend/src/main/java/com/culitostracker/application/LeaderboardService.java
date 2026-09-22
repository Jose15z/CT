package com.culitostracker.application;

import com.culitostracker.api.dto.LeaderboardDtos.LeaderboardEntry;
import com.culitostracker.api.dto.LeaderboardDtos.LeaderboardMeResponse;
import com.culitostracker.api.dto.LeaderboardDtos.LeaderboardResponse;
import com.culitostracker.api.dto.LeaderboardDtos.UpdateLeaderboardSettingsRequest;
import com.culitostracker.domain.model.LeaderboardProfile;
import com.culitostracker.domain.model.LeaderboardWindow;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.repository.LeaderboardProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Opt-in, aggregated-only ranking. The public payload contains an alias, an
 * optional avatar emoji and a score — never partner names, dates or notes.
 */
@Service
public class LeaderboardService {

    private final LeaderboardProfileRepository leaderboardProfileRepository;

    public LeaderboardService(LeaderboardProfileRepository leaderboardProfileRepository) {
        this.leaderboardProfileRepository = leaderboardProfileRepository;
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse ranking(LeaderboardWindow window, UUID viewerId) {
        Instant fromTs = windowStart(window);
        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (var row : leaderboardProfileRepository.ranking(fromTs)) {
            boolean me = viewerId != null && viewerId.toString().equals(row.getUserId());
            String avatar = Boolean.TRUE.equals(row.getShowAvatar()) ? row.getAvatarEmoji() : null;
            entries.add(new LeaderboardEntry(rank++, row.getAlias(), avatar,
                    row.getScore() == null ? 0 : row.getScore(), me));
        }
        return new LeaderboardResponse(window, entries);
    }

    @Transactional(readOnly = true)
    public long scoreOf(UUID userId) {
        return leaderboardProfileRepository.scoreForUser(userId, null);
    }

    @Transactional(readOnly = true)
    public LeaderboardMeResponse me(UUID userId) {
        LeaderboardProfile profile = leaderboardProfileRepository.findById(userId).orElse(null);
        long score = leaderboardProfileRepository.scoreForUser(userId, null);
        Integer rank = null;
        if (profile != null && profile.isPubliclyVisible()) {
            int i = 1;
            for (var row : leaderboardProfileRepository.ranking(null)) {
                if (userId.toString().equals(row.getUserId())) {
                    rank = i;
                    break;
                }
                i++;
            }
        }
        return new LeaderboardMeResponse(
                profile != null && profile.isEnabled(),
                profile != null ? profile.getPublicAlias() : null,
                profile != null && profile.isShowAvatar(),
                score,
                rank);
    }

    @Transactional
    public LeaderboardMeResponse updateSettings(UUID userId, UpdateLeaderboardSettingsRequest request) {
        LeaderboardProfile profile = leaderboardProfileRepository.findById(userId)
                .orElseGet(() -> {
                    LeaderboardProfile p = new LeaderboardProfile();
                    p.setUserId(userId);
                    return p;
                });
        if (request.publicAlias() != null) {
            profile.setPublicAlias(request.publicAlias().isBlank() ? null : request.publicAlias().trim());
        }
        if (request.showAvatar() != null) {
            profile.setShowAvatar(request.showAvatar());
        }
        if (request.enabled() != null) {
            if (request.enabled() && (profile.getPublicAlias() == null || profile.getPublicAlias().isBlank())) {
                throw new DomainRuleException("leaderboard.aliasRequired",
                        "A public alias is required to join the leaderboard");
            }
            profile.setEnabled(request.enabled());
        }
        leaderboardProfileRepository.save(profile);
        return me(userId);
    }

    private Instant windowStart(LeaderboardWindow window) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (window) {
            case GLOBAL -> null;
            case MONTH -> today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            case YEAR -> today.withDayOfYear(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        };
    }
}
