package com.culitostracker.api;

import com.culitostracker.api.dto.LeaderboardDtos.LeaderboardMeResponse;
import com.culitostracker.api.dto.LeaderboardDtos.LeaderboardResponse;
import com.culitostracker.api.dto.LeaderboardDtos.UpdateLeaderboardSettingsRequest;
import com.culitostracker.application.LeaderboardService;
import com.culitostracker.domain.model.LeaderboardWindow;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public LeaderboardResponse ranking(Authentication authentication,
                                       @RequestParam(defaultValue = "GLOBAL") LeaderboardWindow window) {
        return leaderboardService.ranking(window, CurrentUser.id(authentication));
    }

    @GetMapping("/me")
    public LeaderboardMeResponse me(Authentication authentication) {
        return leaderboardService.me(CurrentUser.id(authentication));
    }

    @PatchMapping("/me/settings")
    public LeaderboardMeResponse updateSettings(Authentication authentication,
                                                @Valid @RequestBody UpdateLeaderboardSettingsRequest request) {
        return leaderboardService.updateSettings(CurrentUser.id(authentication), request);
    }
}
