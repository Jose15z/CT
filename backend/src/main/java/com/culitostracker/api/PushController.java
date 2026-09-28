package com.culitostracker.api;

import com.culitostracker.api.dto.PushDtos.PushConfigResponse;
import com.culitostracker.api.dto.PushDtos.SubscribeRequest;
import com.culitostracker.api.dto.PushDtos.UnsubscribeRequest;
import com.culitostracker.application.PlanService;
import com.culitostracker.application.UserService;
import com.culitostracker.application.WebPushService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final WebPushService webPushService;
    private final UserService userService;
    private final PlanService planService;

    public PushController(WebPushService webPushService, UserService userService, PlanService planService) {
        this.webPushService = webPushService;
        this.userService = userService;
        this.planService = planService;
    }

    /** Whether push is configured on this deployment, and the VAPID public key. */
    @GetMapping("/config")
    public PushConfigResponse config(Authentication authentication) {
        UUID userId = CurrentUser.id(authentication);
        return new PushConfigResponse(webPushService.enabled(),
                webPushService.enabled() ? webPushService.publicKey() : null,
                webPushService.hasSubscriptions(userId));
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(Authentication authentication, @Valid @RequestBody SubscribeRequest request) {
        UUID userId = CurrentUser.id(authentication);
        planService.requirePro(userId); // reminders are a Pro feature once billing is on
        webPushService.subscribe(userId, request.endpoint(), request.p256dh(), request.auth());
        if (request.timezone() != null && !request.timezone().isBlank()) {
            userService.updateTimezone(userId, request.timezone());
        }
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(Authentication authentication, @Valid @RequestBody UnsubscribeRequest request) {
        webPushService.unsubscribe(CurrentUser.id(authentication), request.endpoint());
    }
}
