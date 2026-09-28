package com.culitostracker.api.dto;

import com.culitostracker.domain.model.WishlistItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class WishlistDtos {

    private WishlistDtos() {
    }

    public record CreateWishRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 2000) String note,
            @Size(max = 500) String url) {
    }

    /** PATCH semantics: null fields are left untouched. */
    public record UpdateWishRequest(
            @Size(min = 1, max = 120) String title,
            @Size(max = 2000) String note,
            @Size(max = 500) String url,
            Boolean done) {
    }

    public record WishResponse(UUID id, UUID partnerId, String title, String note, String url, boolean done) {

        public static WishResponse from(WishlistItem w) {
            return new WishResponse(w.getId(), w.getPartnerId(), w.getTitle(), w.getNote(), w.getUrl(), w.isDone());
        }
    }
}
