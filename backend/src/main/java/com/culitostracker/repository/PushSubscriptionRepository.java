package com.culitostracker.repository;

import com.culitostracker.domain.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUserId(UUID userId);

    long countByUserId(UUID userId);

    @Query("select distinct s.userId from PushSubscription s")
    List<UUID> findDistinctUserIds();
}
