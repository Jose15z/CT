package com.culitostracker.api.dto;

import com.culitostracker.domain.model.DatePlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class DatePlanDtos {

    private DatePlanDtos() {
    }

    public record CreateDatePlanRequest(
            @NotNull UUID partnerId,
            @NotBlank @Size(max = 120) String title,
            @Size(max = 120) String location,
            @Size(max = 2000) String notes,
            @NotNull LocalDate date,
            LocalTime startTime) {
    }

    /** PATCH semantics: null fields are left untouched. */
    public record UpdateDatePlanRequest(
            @Size(min = 1, max = 120) String title,
            @Size(max = 120) String location,
            @Size(max = 2000) String notes,
            LocalDate date,
            LocalTime startTime) {
    }

    public record DatePlanResponse(UUID id,
                                   UUID partnerId,
                                   String partnerName,
                                   String title,
                                   String location,
                                   String notes,
                                   LocalDate date,
                                   LocalTime startTime) {

        public static DatePlanResponse from(DatePlan plan, String partnerName) {
            return new DatePlanResponse(plan.getId(), plan.getPartnerId(), partnerName,
                    plan.getTitle(), plan.getLocation(), plan.getNotes(),
                    plan.getDate(), plan.getStartTime());
        }
    }
}
