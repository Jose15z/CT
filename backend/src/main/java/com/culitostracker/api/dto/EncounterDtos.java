package com.culitostracker.api.dto;

import com.culitostracker.domain.model.Encounter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class EncounterDtos {

    private EncounterDtos() {
    }

    public record CreateEncounterRequest(
            @NotNull UUID partnerId,
            @NotNull LocalDate date,
            @Size(max = 2000) String notes) {
    }

    public record EncounterResponse(UUID id,
                                    UUID partnerId,
                                    String partnerName,
                                    LocalDate date,
                                    String notes) {

        public static EncounterResponse from(Encounter encounter, String partnerName) {
            return new EncounterResponse(encounter.getId(), encounter.getPartnerId(),
                    partnerName, encounter.getDate(), encounter.getNotes());
        }
    }
}
