package com.culitostracker.repository;

import com.culitostracker.domain.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByProviderSubscriptionId(String providerSubscriptionId);

    Optional<Subscription> findByProviderCustomerId(String providerCustomerId);
}
