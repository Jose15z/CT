package com.culitostracker.api;

import com.culitostracker.api.dto.PartnerDtos.CreatePartnerRequest;
import com.culitostracker.api.dto.PartnerDtos.PartnerResponse;
import com.culitostracker.api.dto.PartnerDtos.RelationshipResponse;
import com.culitostracker.api.dto.PartnerDtos.UpdatePartnerRequest;
import com.culitostracker.api.dto.PartnerDtos.UpdateRelationshipRequest;
import com.culitostracker.application.PartnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping
    public List<PartnerResponse> list(Authentication authentication) {
        return partnerService.list(CurrentUser.id(authentication));
    }

    @GetMapping("/history")
    public List<PartnerResponse> history(Authentication authentication) {
        return partnerService.history(CurrentUser.id(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerResponse create(Authentication authentication,
                                  @Valid @RequestBody CreatePartnerRequest request) {
        return partnerService.create(CurrentUser.id(authentication), request);
    }

    @GetMapping("/{id}")
    public PartnerResponse get(Authentication authentication, @PathVariable UUID id) {
        return partnerService.get(CurrentUser.id(authentication), id);
    }

    @PatchMapping("/{id}")
    public PartnerResponse update(Authentication authentication, @PathVariable UUID id,
                                  @Valid @RequestBody UpdatePartnerRequest request) {
        return partnerService.update(CurrentUser.id(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        partnerService.softDelete(CurrentUser.id(authentication), id);
    }

    @GetMapping("/{id}/relationship")
    public RelationshipResponse getRelationship(Authentication authentication, @PathVariable UUID id) {
        return partnerService.getRelationship(CurrentUser.id(authentication), id);
    }

    @PatchMapping("/{id}/relationship")
    public RelationshipResponse updateRelationship(Authentication authentication, @PathVariable UUID id,
                                                   @Valid @RequestBody UpdateRelationshipRequest request) {
        return partnerService.updateRelationship(CurrentUser.id(authentication), id, request);
    }
}
