package com.culitostracker.api;

import com.culitostracker.api.dto.DatePlanDtos.CreateDatePlanRequest;
import com.culitostracker.api.dto.DatePlanDtos.DatePlanResponse;
import com.culitostracker.api.dto.DatePlanDtos.UpdateDatePlanRequest;
import com.culitostracker.application.DatePlanService;
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
@RequestMapping("/api/date-plans")
public class DatePlanController {

    private final DatePlanService datePlanService;

    public DatePlanController(DatePlanService datePlanService) {
        this.datePlanService = datePlanService;
    }

    @GetMapping
    public List<DatePlanResponse> list(Authentication authentication,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return datePlanService.list(CurrentUser.id(authentication), from, to);
    }

    @GetMapping("/upcoming")
    public List<DatePlanResponse> upcoming(Authentication authentication) {
        return datePlanService.upcoming(CurrentUser.id(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DatePlanResponse create(Authentication authentication,
                                   @Valid @RequestBody CreateDatePlanRequest request) {
        return datePlanService.create(CurrentUser.id(authentication), request);
    }

    @PatchMapping("/{id}")
    public DatePlanResponse update(Authentication authentication, @PathVariable UUID id,
                                   @Valid @RequestBody UpdateDatePlanRequest request) {
        return datePlanService.update(CurrentUser.id(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        datePlanService.delete(CurrentUser.id(authentication), id);
    }
}
