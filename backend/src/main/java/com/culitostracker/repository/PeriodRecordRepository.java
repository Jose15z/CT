package com.culitostracker.repository;

import com.culitostracker.domain.model.PeriodRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodRecordRepository extends JpaRepository<PeriodRecord, UUID> {

    List<PeriodRecord> findByCycleProfileIdOrderByStartDateDesc(UUID cycleProfileId);

    @Query("""
            select pr from PeriodRecord pr
            join CycleProfile cp on cp.id = pr.cycleProfileId
            join Partner p on p.id = cp.partnerId
            where pr.id = :id and p.ownerUserId = :ownerUserId and p.deletedAt is null
            """)
    Optional<PeriodRecord> findByIdAndOwner(@Param("id") UUID id,
                                            @Param("ownerUserId") UUID ownerUserId);
}
