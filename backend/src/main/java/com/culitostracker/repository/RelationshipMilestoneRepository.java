package com.culitostracker.repository;

import com.culitostracker.domain.model.RelationshipMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelationshipMilestoneRepository extends JpaRepository<RelationshipMilestone, UUID> {

    List<RelationshipMilestone> findByPartnerIdOrderByDateDesc(UUID partnerId);

    @Query("""
            select m from RelationshipMilestone m
            join Partner p on p.id = m.partnerId
            where m.id = :id and p.ownerUserId = :ownerUserId and p.deletedAt is null
            """)
    Optional<RelationshipMilestone> findByIdAndOwner(@Param("id") UUID id,
                                                     @Param("ownerUserId") UUID ownerUserId);

    @Query("""
            select count(m) from RelationshipMilestone m
            join Partner p on p.id = m.partnerId
            where p.ownerUserId = :ownerUserId and p.deletedAt is null
            """)
    long countByOwner(@Param("ownerUserId") UUID ownerUserId);
}
