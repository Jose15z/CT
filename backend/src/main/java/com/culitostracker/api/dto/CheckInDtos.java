package com.culitostracker.api.dto;

import com.culitostracker.domain.model.Mood;
import com.culitostracker.domain.model.RelationshipCheckIn;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class CheckInDtos {

    private CheckInDtos() {
    }

    public record CreateCheckInRequest(
            @NotNull UUID partnerId,
            @NotNull Mood mood,
            @NotNull @Min(1) @Max(5) Integer energyLevel,
            @NotNull @Min(1) @Max(5) Integer stressLevel,
            @Min(1) @Max(5) Integer affectionLevel,
            @Min(1) @Max(5) Integer relationshipSatisfaction,
            @Size(max = 2000) String note,
            LocalDate date) {  // defaults to today; same-day check-in is replaced
    }

    public record CheckInResponse(UUID id,
                                  UUID partnerId,
                                  boolean mine,
                                  Mood mood,
                                  int energyLevel,
                                  int stressLevel,
                                  Integer affectionLevel,
                                  Integer relationshipSatisfaction,
                                  String note,
                                  LocalDate date) {

        public static CheckInResponse from(RelationshipCheckIn c, UUID partnerId, UUID viewerId) {
            boolean mine = c.getAuthorUserId().equals(viewerId);
            return new CheckInResponse(c.getId(), partnerId, mine, c.getMood(),
                    c.getEnergyLevel(), c.getStressLevel(), c.getAffectionLevel(),
                    c.getRelationshipSatisfaction(),
                    // A shared check-in's free-text note stays private unless it is yours.
                    mine ? c.getNote() : null,
                    c.getCheckInDate());
        }
    }
}
