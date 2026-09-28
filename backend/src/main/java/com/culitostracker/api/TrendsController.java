package com.culitostracker.api;

import com.culitostracker.api.dto.TrendsDtos.TrendsResponse;
import com.culitostracker.application.TrendsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trends")
public class TrendsController {

    private final TrendsService trendsService;

    public TrendsController(TrendsService trendsService) {
        this.trendsService = trendsService;
    }

    @GetMapping
    public TrendsResponse trends(Authentication authentication,
                                 @RequestParam(defaultValue = "8") int weeks) {
        return trendsService.forUser(CurrentUser.id(authentication), weeks);
    }
}
