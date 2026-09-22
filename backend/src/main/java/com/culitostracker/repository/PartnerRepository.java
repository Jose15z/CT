package com.culitostracker.repository;

import com.culitostracker.domain.model.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    /**
     * Ownership is enforced in the query itself: a partner that belongs to
     * someone else simply does not exist for this user (404, not 403).
     */
    Optional<Partner> findByIdAndOwnerUserIdAndDeletedAtIsNull(UUID id, UUID ownerUserId);

    List<Partner> findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID ownerUserId);

    /** History view: includes soft-deleted records. */
    List<Partner> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    boolean existsByOwnerUserIdAndNormalizedNameAndDeletedAtIsNull(UUID ownerUserId, String normalizedName);

    /** Used for the partner-creation rate limit (anti-cheat). */
    long countByOwnerUserIdAndCreatedAtAfter(UUID ownerUserId, Instant after);

    long countByOwnerUserIdAndDeletedAtIsNull(UUID ownerUserId);

    /** Partner records where the authenticated user is the linked person. */
    List<Partner> findByLinkedUserIdAndDeletedAtIsNull(UUID linkedUserId);

    Optional<Partner> findByIdAndLinkedUserIdAndDeletedAtIsNull(UUID id, UUID linkedUserId);
}
