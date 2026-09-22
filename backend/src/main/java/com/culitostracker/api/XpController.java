package com.culitostracker.api;

import com.culitostracker.api.dto.XpDtos.XpResponse;
import com.culitostracker.application.XpService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/xp")
public class XpController {

    private final XpService xpService;

    public XpController(XpService xpService) {
        this.xpService = xpService;
    }

    @GetMapping("/me")
    public XpResponse me(Authentication authentication) {
        return xpService.me(CurrentUser.id(authentication));
    }
}
