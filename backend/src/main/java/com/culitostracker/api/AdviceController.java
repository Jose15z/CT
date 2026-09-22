package com.culitostracker.api;

import com.culitostracker.api.dto.AdviceDtos.AdviceResponse;
import com.culitostracker.application.AdviceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/partners/{partnerId}/advice")
public class AdviceController {

    private final AdviceService adviceService;

    public AdviceController(AdviceService adviceService) {
        this.adviceService = adviceService;
    }

    @GetMapping("/today")
    public AdviceResponse today(Authentication authentication, @PathVariable UUID partnerId) {
        return adviceService.today(CurrentUser.id(authentication), partnerId);
    }
}
