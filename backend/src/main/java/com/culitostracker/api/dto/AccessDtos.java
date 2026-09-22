package com.culitostracker.api.dto;

import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.AccessStatus;
import com.culitostracker.domain.model.PartnerAccess;
import jakarta.validation.constraints.NotNull;

public final class AccessDtos {

    private AccessDtos() {
    }

    public record SetGrantRequest(@NotNull Boolean enabled) {
    }

    public record AccessGrantResponse(AccessScope scope, AccessStatus status) {

        public static AccessGrantResponse from(PartnerAccess access) {
            return new AccessGrantResponse(access.getScope(), access.getStatus());
        }
    }
}
