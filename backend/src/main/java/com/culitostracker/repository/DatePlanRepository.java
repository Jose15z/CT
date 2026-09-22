package com.culitostracker.repository;

import com.culitostracker.domain.model.DatePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatePlanRepository extends JpaRepository<DatePlan, UUID> {

    Optional<DatePlan> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<DatePlan> findByOwnerUserIdAndDateBetweenOrderByDateAscStartTimeAsc(
            UUID ownerUserId, LocalDate from, LocalDate to);

    List<DatePlan> findFirst5ByOwnerUserIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            UUID ownerUserId, LocalDate from);
}
