package com.culitostracker.api;

import com.culitostracker.api.dto.AccessDtos.AccessGrantResponse;
import com.culitostracker.api.dto.AccessDtos.SetGrantRequest;
import com.culitostracker.application.PartnerAccessService;
import com.culitostracker.domain.model.AccessScope;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Consent grants for linked partner accounts. The caller can only manage the
 * grants THEY gave; revocation is a PUT with enabled=false.
 */
@RestController
@RequestMapping("/api/partners/{partnerId}/access")
public class AccessController {

    private final PartnerAccessService partnerAccessService;

    public AccessController(PartnerAccessService partnerAccessService) {
        this.partnerAccessService = partnerAccessService;
    }

    @GetMapping
    public List<AccessGrantResponse> grantsGiven(Authentication authentication,
                                                 @PathVariable UUID partnerId) {
        return partnerAccessService.grantsGivenBy(partnerId, CurrentUser.id(authentication)).stream()
                .map(AccessGrantResponse::from)
                .toList();
    }

    @PutMapping("/{scope}")
    public AccessGrantResponse setGrant(Authentication authentication,
                                        @PathVariable UUID partnerId,
                                        @PathVariable AccessScope scope,
                                        @Valid @RequestBody SetGrantRequest request) {
        return AccessGrantResponse.from(partnerAccessService.setGrant(
                partnerId, CurrentUser.id(authentication), scope, request.enabled()));
    }
}
