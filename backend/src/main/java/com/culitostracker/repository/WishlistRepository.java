package com.culitostracker.repository;

import com.culitostracker.domain.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<WishlistItem, UUID> {

    Optional<WishlistItem> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<WishlistItem> findByPartnerIdOrderByDoneAscCreatedAtDesc(UUID partnerId);

    long countByPartnerIdAndDoneFalse(UUID partnerId);

    List<WishlistItem> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);
}
