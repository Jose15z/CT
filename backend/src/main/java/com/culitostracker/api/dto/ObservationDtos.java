package com.culitostracker.api.dto;

import com.culitostracker.domain.model.ObservationType;
import com.culitostracker.domain.model.PartnerObservation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class ObservationDtos {

    private ObservationDtos() {
    }

    public record CreateObservationRequest(
            @NotNull ObservationType observationType,
            @Size(max = 2000) String note,
            LocalDate date) {  // defaults to today
    }

    public record ObservationResponse(UUID id,
                                      UUID partnerId,
                                      ObservationType observationType,
                                      String note,
                                      LocalDate date) {

        public static ObservationResponse from(PartnerObservation o) {
            return new ObservationResponse(o.getId(), o.getPartnerId(),
                    o.getObservationType(), o.getNote(), o.getObservationDate());
        }
    }
}
