package com.culitostracker.api;

import com.culitostracker.api.dto.StatsDtos.StatsResponse;
import com.culitostracker.application.StatsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/me")
    public StatsResponse me(Authentication authentication) {
        return statsService.me(CurrentUser.id(authentication));
    }
}
