package com.culitostracker.api;

import com.culitostracker.api.dto.CheckInDtos.CheckInResponse;
import com.culitostracker.api.dto.CheckInDtos.CreateCheckInRequest;
import com.culitostracker.application.CheckInService;
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
@RequestMapping("/api")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/check-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInResponse create(Authentication authentication,
                                  @Valid @RequestBody CreateCheckInRequest request) {
        return checkInService.create(CurrentUser.id(authentication), request);
    }

    @GetMapping("/partners/{partnerId}/check-ins")
    public List<CheckInResponse> list(Authentication authentication, @PathVariable UUID partnerId) {
        return checkInService.list(CurrentUser.id(authentication), partnerId);
    }
}
