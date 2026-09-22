package com.culitostracker.repository;

import com.culitostracker.domain.model.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelationshipRepository extends JpaRepository<Relationship, UUID> {

    Optional<Relationship> findByPartnerId(UUID partnerId);

    @Query("""
            select r from Relationship r
            join Partner p on p.id = r.partnerId
            where p.ownerUserId = :ownerUserId
              and p.deletedAt is null
              and r.status = com.culitostracker.domain.model.RelationshipStatus.ACTIVE
            """)
    List<Relationship> findActiveByOwner(@Param("ownerUserId") UUID ownerUserId);

    @Query("""
            select r from Relationship r
            join Partner p on p.id = r.partnerId
            where p.ownerUserId = :ownerUserId and p.deletedAt is null
            """)
    List<Relationship> findAllByOwner(@Param("ownerUserId") UUID ownerUserId);
}
