package com.culitostracker.repository;

import com.culitostracker.domain.model.PartnerInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PartnerInviteRepository extends JpaRepository<PartnerInvite, UUID> {

    Optional<PartnerInvite> findByTokenHash(String tokenHash);

    /** A new invite replaces any pending one for the same partner record. */
    @Modifying
    @Query("delete from PartnerInvite i where i.partnerId = :partnerId and i.acceptedAt is null")
    void deletePendingByPartnerId(@Param("partnerId") UUID partnerId);
}
