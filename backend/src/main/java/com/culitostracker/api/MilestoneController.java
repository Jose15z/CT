package com.culitostracker.api;

import com.culitostracker.api.dto.MilestoneDtos.CreateMilestoneRequest;
import com.culitostracker.api.dto.MilestoneDtos.MilestoneResponse;
import com.culitostracker.api.dto.MilestoneDtos.UpdateMilestoneRequest;
import com.culitostracker.application.MilestoneService;
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
@RequestMapping("/api")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @GetMapping("/partners/{partnerId}/milestones")
    public List<MilestoneResponse> list(Authentication authentication, @PathVariable UUID partnerId) {
        return milestoneService.list(CurrentUser.id(authentication), partnerId);
    }

    @PostMapping("/partners/{partnerId}/milestones")
    @ResponseStatus(HttpStatus.CREATED)
    public MilestoneResponse create(Authentication authentication, @PathVariable UUID partnerId,
                                    @Valid @RequestBody CreateMilestoneRequest request) {
        return milestoneService.create(CurrentUser.id(authentication), partnerId, request);
    }

    @PatchMapping("/milestones/{id}")
    public MilestoneResponse update(Authentication authentication, @PathVariable UUID id,
                                    @Valid @RequestBody UpdateMilestoneRequest request) {
        return milestoneService.update(CurrentUser.id(authentication), id, request);
    }

    @DeleteMapping("/milestones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        milestoneService.delete(CurrentUser.id(authentication), id);
    }
}
