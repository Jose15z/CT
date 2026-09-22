package com.culitostracker.api.dto;

import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.model.RelationshipType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PartnerDtos {

    private PartnerDtos() {
    }

    public record CreatePartnerRequest(
            @NotBlank @Size(max = 60) String name,
            @Size(max = 60) String nickname,
            @Size(max = 2000) String notes,
            @Size(max = 16) String avatarEmoji,
            LocalDate birthDate,
            @DecimalMin("30") @DecimalMax("300") BigDecimal weightKg,
            @NotNull RelationshipType relationshipType,
            LocalDate datingStartDate,
            LocalDate relationshipStartDate,
            LocalDate engagementDate,
            LocalDate marriageDate,
            /** The user confirms they have this person's consent to store their data. */
            @AssertTrue(message = "consent confirmation is required")
            boolean consentConfirmed) {
    }

    /** PATCH semantics: null fields are left untouched. */
    public record UpdatePartnerRequest(
            @Size(min = 1, max = 60) String name,
            @Size(max = 60) String nickname,
            @Size(max = 2000) String notes,
            @Size(max = 16) String avatarEmoji,
            LocalDate birthDate,
            @DecimalMin("30") @DecimalMax("300") BigDecimal weightKg) {
    }

    /** PATCH semantics: null fields are left untouched. Setting status ACTIVE clears the end date. */
    public record UpdateRelationshipRequest(
            RelationshipType type,
            RelationshipStatus status,
            LocalDate datingStartDate,
            LocalDate relationshipStartDate,
            LocalDate engagementDate,
            LocalDate marriageDate,
            LocalDate relationshipEndDate) {
    }

    public record DurationDto(int years, int months, int days, long totalDays) {
    }

    public record AnniversaryDto(LocalDate date, long daysUntil, int years) {
    }

    public record RelationshipResponse(UUID id,
                                       RelationshipType type,
                                       RelationshipStatus status,
                                       LocalDate datingStartDate,
                                       LocalDate relationshipStartDate,
                                       LocalDate engagementDate,
                                       LocalDate marriageDate,
                                       LocalDate relationshipEndDate,
                                       LocalDate togetherSince,
                                       DurationDto duration,
                                       AnniversaryDto nextAnniversary) {
    }

    public record PartnerResponse(UUID id,
                                  String name,
                                  String nickname,
                                  String notes,
                                  String avatarEmoji,
                                  LocalDate birthDate,
                                  Integer age,
                                  BigDecimal weightKg,
                                  boolean linked,
                                  boolean deleted,
                                  RelationshipResponse relationship,
                                  Instant createdAt) {
    }
}
