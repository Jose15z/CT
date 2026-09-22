package com.culitostracker.api;

import com.culitostracker.api.dto.CycleDtos.CreatePeriodRequest;
import com.culitostracker.api.dto.CycleDtos.CycleProfileResponse;
import com.culitostracker.api.dto.CycleDtos.PeriodRecordResponse;
import com.culitostracker.api.dto.CycleDtos.PredictionsResponse;
import com.culitostracker.api.dto.CycleDtos.UpdateCycleProfileRequest;
import com.culitostracker.api.dto.CycleDtos.UpdatePeriodRequest;
import com.culitostracker.application.CycleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CycleController {

    private final CycleService cycleService;

    public CycleController(CycleService cycleService) {
        this.cycleService = cycleService;
    }

    @GetMapping("/partners/{partnerId}/cycle")
    public CycleProfileResponse getProfile(Authentication authentication, @PathVariable UUID partnerId) {
        return cycleService.getProfile(CurrentUser.id(authentication), partnerId);
    }

    @PatchMapping("/partners/{partnerId}/cycle")
    public CycleProfileResponse updateProfile(Authentication authentication, @PathVariable UUID partnerId,
                                              @Valid @RequestBody UpdateCycleProfileRequest request) {
        return cycleService.updateProfile(CurrentUser.id(authentication), partnerId, request);
    }

    @GetMapping("/partners/{partnerId}/periods")
    public List<PeriodRecordResponse> listPeriods(Authentication authentication, @PathVariable UUID partnerId) {
        return cycleService.listPeriods(CurrentUser.id(authentication), partnerId);
    }

    @PostMapping("/partners/{partnerId}/periods")
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodRecordResponse recordPeriod(Authentication authentication, @PathVariable UUID partnerId,
                                             @Valid @RequestBody CreatePeriodRequest request) {
        return cycleService.recordPeriod(CurrentUser.id(authentication), partnerId, request);
    }

    @PatchMapping("/periods/{id}")
    public PeriodRecordResponse updatePeriod(Authentication authentication, @PathVariable UUID id,
                                             @Valid @RequestBody UpdatePeriodRequest request) {
        return cycleService.updatePeriod(CurrentUser.id(authentication), id, request);
    }

    @DeleteMapping("/periods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePeriod(Authentication authentication, @PathVariable UUID id) {
        cycleService.deletePeriod(CurrentUser.id(authentication), id);
    }

    @GetMapping("/partners/{partnerId}/cycle/predictions")
    public PredictionsResponse predictions(Authentication authentication, @PathVariable UUID partnerId,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return cycleService.predictions(CurrentUser.id(authentication), partnerId, from, to);
    }
}
