package com.culitostracker.api;

import com.culitostracker.api.dto.EncounterDtos.CreateEncounterRequest;
import com.culitostracker.api.dto.EncounterDtos.EncounterResponse;
import com.culitostracker.application.EncounterService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterService encounterService;

    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @GetMapping
    public List<EncounterResponse> list(Authentication authentication,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return encounterService.list(CurrentUser.id(authentication), from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EncounterResponse create(Authentication authentication,
                                    @Valid @RequestBody CreateEncounterRequest request) {
        return encounterService.create(CurrentUser.id(authentication), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        encounterService.delete(CurrentUser.id(authentication), id);
    }
}
