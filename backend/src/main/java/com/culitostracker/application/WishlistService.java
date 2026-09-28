package com.culitostracker.application;

import com.culitostracker.api.dto.WishlistDtos.CreateWishRequest;
import com.culitostracker.api.dto.WishlistDtos.UpdateWishRequest;
import com.culitostracker.api.dto.WishlistDtos.WishResponse;
import com.culitostracker.domain.model.WishlistItem;
import com.culitostracker.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final PartnerAccessService partnerAccessService;

    public WishlistService(WishlistRepository wishlistRepository, PartnerAccessService partnerAccessService) {
        this.wishlistRepository = wishlistRepository;
        this.partnerAccessService = partnerAccessService;
    }

    @Transactional(readOnly = true)
    public List<WishResponse> list(UUID userId, UUID partnerId) {
        partnerAccessService.requireOwned(partnerId, userId);
        return wishlistRepository.findByPartnerIdOrderByDoneAscCreatedAtDesc(partnerId).stream()
                .map(WishResponse::from)
                .toList();
    }

    @Transactional
    public WishResponse create(UUID userId, UUID partnerId, CreateWishRequest request) {
        partnerAccessService.requireOwned(partnerId, userId);
        WishlistItem item = new WishlistItem();
        item.setOwnerUserId(userId);
        item.setPartnerId(partnerId);
        item.setTitle(request.title().trim());
        item.setNote(blankToNull(request.note()));
        item.setUrl(blankToNull(request.url()));
        return WishResponse.from(wishlistRepository.save(item));
    }

    @Transactional
    public WishResponse update(UUID userId, UUID itemId, UpdateWishRequest request) {
        WishlistItem item = wishlistRepository.findByIdAndOwnerUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Wish not found"));
        if (request.title() != null) {
            item.setTitle(request.title().trim());
        }
        if (request.note() != null) {
            item.setNote(blankToNull(request.note()));
        }
        if (request.url() != null) {
            item.setUrl(blankToNull(request.url()));
        }
        if (request.done() != null) {
            item.setDone(request.done());
        }
        return WishResponse.from(wishlistRepository.save(item));
    }

    @Transactional
    public void delete(UUID userId, UUID itemId) {
        WishlistItem item = wishlistRepository.findByIdAndOwnerUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Wish not found"));
        wishlistRepository.delete(item);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
