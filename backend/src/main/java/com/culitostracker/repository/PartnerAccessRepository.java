package com.culitostracker.repository;

import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.AccessStatus;
import com.culitostracker.domain.model.PartnerAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerAccessRepository extends JpaRepository<PartnerAccess, UUID> {

    Optional<PartnerAccess> findByPartnerIdAndGrantedToUserIdAndScope(
            UUID partnerId, UUID grantedToUserId, AccessScope scope);

    boolean existsByPartnerIdAndGrantedToUserIdAndScopeAndStatus(
            UUID partnerId, UUID grantedToUserId, AccessScope scope, AccessStatus status);

    List<PartnerAccess> findByPartnerIdAndGrantedByUserId(UUID partnerId, UUID grantedByUserId);
}
