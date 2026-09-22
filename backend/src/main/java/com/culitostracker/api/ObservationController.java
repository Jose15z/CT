package com.culitostracker.api;

import com.culitostracker.api.dto.ObservationDtos.CreateObservationRequest;
import com.culitostracker.api.dto.ObservationDtos.ObservationResponse;
import com.culitostracker.application.ObservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partners/{partnerId}/observations")
public class ObservationController {

    private final ObservationService observationService;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObservationResponse create(Authentication authentication, @PathVariable UUID partnerId,
                                      @Valid @RequestBody CreateObservationRequest request) {
        return observationService.create(CurrentUser.id(authentication), partnerId, request);
    }

    @GetMapping
    public List<ObservationResponse> list(Authentication authentication, @PathVariable UUID partnerId) {
        return observationService.list(CurrentUser.id(authentication), partnerId);
    }
}
