package com.culitostracker.repository;

import com.culitostracker.domain.model.PartnerObservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerObservationRepository extends JpaRepository<PartnerObservation, UUID> {

    /** Observations are private to their observer; every query filters by it. */
    List<PartnerObservation> findTop30ByPartnerIdAndObserverUserIdOrderByCreatedAtDesc(
            UUID partnerId, UUID observerUserId);

    Optional<PartnerObservation> findTopByPartnerIdAndObserverUserIdAndObservationDateGreaterThanEqualOrderByCreatedAtDesc(
            UUID partnerId, UUID observerUserId, LocalDate since);
}
