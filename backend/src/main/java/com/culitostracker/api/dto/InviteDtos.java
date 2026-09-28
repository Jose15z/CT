package com.culitostracker.api.dto;

import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.AccessStatus;
import com.culitostracker.domain.model.RelationshipType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InviteDtos {

    private InviteDtos() {
    }

    /** Returned once, on creation: the raw token never leaves the server again. */
    public record InviteResponse(String url, Instant expiresAt) {
    }

    /** What the invitee sees before deciding; deliberately minimal. */
    public record InvitePreview(String inviterDisplayName,
                                String partnerName,
                                RelationshipType relationshipType,
                                Instant expiresAt) {
    }

    public record GrantState(AccessScope scope, AccessStatus status) {
    }

    /** A partner record where the caller is the linked person. */
    public record LinkResponse(UUID partnerId,
                               String ownerDisplayName,
                               String ownerUsername,
                               String partnerName,
                               RelationshipType relationshipType,
                               List<GrantState> grantsGivenByMe,
                               List<GrantState> grantsGivenToMe) {
    }
}
