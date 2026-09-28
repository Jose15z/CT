package com.culitostracker.repository;

import com.culitostracker.domain.model.RelationshipCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelationshipCheckInRepository extends JpaRepository<RelationshipCheckIn, UUID> {

    Optional<RelationshipCheckIn> findByRelationshipIdAndAuthorUserIdAndCheckInDate(
            UUID relationshipId, UUID authorUserId, LocalDate checkInDate);

    /** Everything the user wrote, for the data export. */
    List<RelationshipCheckIn> findByAuthorUserIdOrderByCheckInDateDesc(UUID authorUserId);

    List<RelationshipCheckIn> findByAuthorUserIdAndCheckInDateBetween(
            UUID authorUserId, LocalDate from, LocalDate to);

    List<RelationshipCheckIn> findByRelationshipIdAndAuthorUserIdAndCheckInDateBetween(
            UUID relationshipId, UUID authorUserId, LocalDate from, LocalDate to);

    List<RelationshipCheckIn> findTop30ByRelationshipIdAndAuthorUserIdOrderByCheckInDateDesc(
            UUID relationshipId, UUID authorUserId);

    Optional<RelationshipCheckIn> findTopByRelationshipIdAndAuthorUserIdAndCheckInDateGreaterThanEqualOrderByCheckInDateDesc(
            UUID relationshipId, UUID authorUserId, LocalDate since);
}
