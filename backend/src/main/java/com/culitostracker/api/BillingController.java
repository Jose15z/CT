package com.culitostracker.api;

import com.culitostracker.api.dto.BillingDtos.BillingMe;
import com.culitostracker.api.dto.BillingDtos.RedirectResponse;
import com.culitostracker.application.StripeBillingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final StripeBillingService billingService;

    public BillingController(StripeBillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/me")
    public BillingMe me(Authentication authentication) {
        return billingService.me(CurrentUser.id(authentication));
    }

    @PostMapping("/checkout")
    public RedirectResponse checkout(Authentication authentication) {
        return new RedirectResponse(billingService.checkoutUrl(CurrentUser.id(authentication)));
    }

    @PostMapping("/portal")
    public RedirectResponse portal(Authentication authentication) {
        return new RedirectResponse(billingService.portalUrl(CurrentUser.id(authentication)));
    }

    /** Public endpoint authenticated by the Stripe signature, not by a JWT. */
    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void webhook(@RequestBody String payload,
                        @RequestHeader("Stripe-Signature") String signature) {
        billingService.handleWebhook(payload, signature);
    }
}
