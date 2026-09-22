package com.culitostracker.api.dto;

import com.culitostracker.domain.model.LeaderboardWindow;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class LeaderboardDtos {

    private LeaderboardDtos() {
    }

    public record LeaderboardEntry(int rank,
                                   String alias,
                                   String avatarEmoji,
                                   long score,
                                   boolean me) {
    }

    public record LeaderboardResponse(LeaderboardWindow window,
                                      List<LeaderboardEntry> entries) {
    }

    public record LeaderboardMeResponse(boolean enabled,
                                        String publicAlias,
                                        boolean showAvatar,
                                        long score,
                                        Integer rank) {
    }

    /** PATCH semantics: null fields are left untouched. */
    public record UpdateLeaderboardSettingsRequest(
            Boolean enabled,
            @Size(min = 2, max = 30)
            @Pattern(regexp = "[\\p{L}0-9_.\\- ]+", message = "alias contains invalid characters")
            String publicAlias,
            Boolean showAvatar) {
    }
}
