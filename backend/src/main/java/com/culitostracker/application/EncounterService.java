package com.culitostracker.application;

import com.culitostracker.api.dto.EncounterDtos.CreateEncounterRequest;
import com.culitostracker.api.dto.EncounterDtos.EncounterResponse;
import com.culitostracker.domain.model.Encounter;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.infrastructure.config.LimitsProperties;
import com.culitostracker.repository.EncounterRepository;
import com.culitostracker.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerAccessService partnerAccessService;
    private final LimitsProperties limits;

    public EncounterService(EncounterRepository encounterRepository,
                            PartnerRepository partnerRepository,
                            PartnerAccessService partnerAccessService,
                            LimitsProperties limits) {
        this.encounterRepository = encounterRepository;
        this.partnerRepository = partnerRepository;
        this.partnerAccessService = partnerAccessService;
        this.limits = limits;
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> list(UUID userId, LocalDate from, LocalDate to) {
        Map<UUID, String> names = partnerRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream()
                .collect(Collectors.toMap(Partner::getId, Partner::getName, (a, b) -> a));
        return encounterRepository.findByOwnerUserIdAndDateBetweenOrderByDateAscCreatedAtAsc(userId, from, to)
                .stream()
                .map(encounter -> EncounterResponse.from(encounter, names.get(encounter.getPartnerId())))
                .toList();
    }

    @Transactional
    public EncounterResponse create(UUID userId, CreateEncounterRequest request) {
        Partner partner = partnerAccessService.requireOwned(request.partnerId(), userId);
        if (request.date().isAfter(LocalDate.now())) {
            throw new DomainRuleException("encounter.dateInFuture",
                    "Encounters can only be logged for today or the past");
        }
        // Anti-farming cap: the XP engine's input can't be spammed.
        if (encounterRepository.countByOwnerUserIdAndDate(userId, request.date()) >= limits.encountersPerDay()) {
            throw new DomainRuleException("encounter.dailyLimit",
                    "Too many encounters logged for this day");
        }
        Encounter encounter = new Encounter();
        encounter.setOwnerUserId(userId);
        encounter.setPartnerId(partner.getId());
        encounter.setDate(request.date());
        encounter.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes());
        return EncounterResponse.from(encounterRepository.save(encounter), partner.getName());
    }

    @Transactional
    public void delete(UUID userId, UUID encounterId) {
        Encounter encounter = encounterRepository.findByIdAndOwnerUserId(encounterId, userId)
                .orElseThrow(() -> new NotFoundException("Encounter not found"));
        encounterRepository.delete(encounter);
    }
}
