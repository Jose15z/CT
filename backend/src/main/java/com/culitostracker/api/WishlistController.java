package com.culitostracker.api;

import com.culitostracker.api.dto.WishlistDtos.CreateWishRequest;
import com.culitostracker.api.dto.WishlistDtos.UpdateWishRequest;
import com.culitostracker.api.dto.WishlistDtos.WishResponse;
import com.culitostracker.application.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping("/partners/{partnerId}/wishlist")
    public List<WishResponse> list(Authentication authentication, @PathVariable UUID partnerId) {
        return wishlistService.list(CurrentUser.id(authentication), partnerId);
    }

    @PostMapping("/partners/{partnerId}/wishlist")
    @ResponseStatus(HttpStatus.CREATED)
    public WishResponse create(Authentication authentication, @PathVariable UUID partnerId,
                               @Valid @RequestBody CreateWishRequest request) {
        return wishlistService.create(CurrentUser.id(authentication), partnerId, request);
    }

    @PatchMapping("/wishlist/{id}")
    public WishResponse update(Authentication authentication, @PathVariable UUID id,
                               @Valid @RequestBody UpdateWishRequest request) {
        return wishlistService.update(CurrentUser.id(authentication), id, request);
    }

    @DeleteMapping("/wishlist/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        wishlistService.delete(CurrentUser.id(authentication), id);
    }
}
