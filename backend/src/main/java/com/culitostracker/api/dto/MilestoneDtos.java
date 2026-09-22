package com.culitostracker.api.dto;

import com.culitostracker.domain.model.MilestoneType;
import com.culitostracker.domain.model.RelationshipMilestone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class MilestoneDtos {

    private MilestoneDtos() {
    }

    public record CreateMilestoneRequest(
            @NotNull MilestoneType type,
            @NotBlank @Size(max = 120) String title,
            @Size(max = 2000) String description,
            @NotNull LocalDate date) {
    }

    /** PATCH semantics: null fields are left untouched. */
    public record UpdateMilestoneRequest(
            MilestoneType type,
            @Size(min = 1, max = 120) String title,
            @Size(max = 2000) String description,
            LocalDate date) {
    }

    public record MilestoneResponse(UUID id,
                                    UUID partnerId,
                                    MilestoneType type,
                                    String title,
                                    String description,
                                    LocalDate date) {

        public static MilestoneResponse from(RelationshipMilestone m) {
            return new MilestoneResponse(m.getId(), m.getPartnerId(), m.getType(),
                    m.getTitle(), m.getDescription(), m.getDate());
        }
    }
}
