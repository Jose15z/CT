package com.culitostracker.api.dto;

import com.culitostracker.domain.model.RelationshipSituation;
import com.culitostracker.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(UUID id,
                               String username,
                               String email,
                               String displayName,
                               String preferredLanguage,
                               String avatarEmoji,
                               boolean hasAvatar,
                               RelationshipSituation relationshipSituation,
                               Instant createdAt) {

        public static UserResponse from(User user) {
            return from(user, false);
        }

        public static UserResponse from(User user, boolean hasAvatar) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                    user.getDisplayName(), user.getPreferredLanguage(), user.getAvatarEmoji(),
                    hasAvatar, user.getRelationshipSituation(), user.getCreatedAt());
        }
    }

    /** PATCH semantics: null fields are left untouched. */
    public record UpdateUserRequest(
            @Size(min = 1, max = 60) String displayName,
            @Size(min = 2, max = 5) String preferredLanguage,
            @Size(max = 16) String avatarEmoji,
            RelationshipSituation relationshipSituation) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }
}
