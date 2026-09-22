package com.culitostracker.api;

import com.culitostracker.api.dto.DashboardDtos.DashboardResponse;
import com.culitostracker.application.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse dashboard(Authentication authentication) {
        return dashboardService.dashboard(CurrentUser.id(authentication));
    }
}
