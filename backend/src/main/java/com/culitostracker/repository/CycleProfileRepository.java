package com.culitostracker.repository;

import com.culitostracker.domain.model.CycleProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CycleProfileRepository extends JpaRepository<CycleProfile, UUID> {

    Optional<CycleProfile> findByPartnerId(UUID partnerId);
}
