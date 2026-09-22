package com.culitostracker.repository;

import com.culitostracker.domain.model.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EncounterRepository extends JpaRepository<Encounter, UUID> {

    Optional<Encounter> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<Encounter> findByOwnerUserIdAndDateBetweenOrderByDateAscCreatedAtAsc(
            UUID ownerUserId, LocalDate from, LocalDate to);

    /** Full history in stable chronological order: the XP engine's input. */
    List<Encounter> findByOwnerUserIdOrderByDateAscCreatedAtAscIdAsc(UUID ownerUserId);

    long countByOwnerUserIdAndDate(UUID ownerUserId, LocalDate date);
}
