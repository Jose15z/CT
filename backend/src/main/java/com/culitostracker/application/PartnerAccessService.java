package com.culitostracker.application;

import com.culitostracker.domain.model.AccessScope;
import com.culitostracker.domain.model.AccessStatus;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.PartnerAccess;
import com.culitostracker.repository.PartnerAccessRepository;
import com.culitostracker.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Central authorization point for partner-scoped data.
 *
 * Two access levels:
 * - OWNER: the user who created the partner record. Full read/write.
 * - LINKED + GRANT: the partner's own account (linkedUserId) may read a scope
 *   the owner granted, and vice versa: the linked account may grant the owner
 *   access to data it authors (check-ins). Grants are revocable.
 *
 * Every miss throws NotFoundException (mapped to 404) so IDs can't be probed.
 */
@Service
public class PartnerAccessService {

    private final PartnerRepository partnerRepository;
    private final PartnerAccessRepository partnerAccessRepository;

    public PartnerAccessService(PartnerRepository partnerRepository,
                                PartnerAccessRepository partnerAccessRepository) {
        this.partnerRepository = partnerRepository;
        this.partnerAccessRepository = partnerAccessRepository;
    }

    /** The user must own the partner record. Used for every write. */
    @Transactional(readOnly = true)
    public Partner requireOwned(UUID partnerId, UUID userId) {
        return partnerRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(partnerId, userId)
                .orElseThrow(() -> new NotFoundException("Partner not found"));
    }

    /** Owner, or the linked account itself (a participant in the relationship). */
    @Transactional(readOnly = true)
    public Partner requireParticipant(UUID partnerId, UUID userId) {
        return partnerRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(partnerId, userId)
                .or(() -> partnerRepository.findByIdAndLinkedUserIdAndDeletedAtIsNull(partnerId, userId))
                .orElseThrow(() -> new NotFoundException("Partner not found"));
    }

    /**
     * Owner always; the linked account only with an ACTIVE grant for the scope.
     */
    @Transactional(readOnly = true)
    public Partner requireViewable(UUID partnerId, UUID userId, AccessScope scope) {
        var owned = partnerRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(partnerId, userId);
        if (owned.isPresent()) {
            return owned.get();
        }
        var linked = partnerRepository.findByIdAndLinkedUserIdAndDeletedAtIsNull(partnerId, userId);
        if (linked.isPresent() && hasActiveGrant(partnerId, userId, scope)) {
            return linked.get();
        }
        throw new NotFoundException("Partner not found");
    }

    @Transactional(readOnly = true)
    public boolean hasActiveGrant(UUID partnerId, UUID grantedToUserId, AccessScope scope) {
        return partnerAccessRepository.existsByPartnerIdAndGrantedToUserIdAndScopeAndStatus(
                partnerId, grantedToUserId, scope, AccessStatus.ACTIVE);
    }

    /**
     * Grant or revoke a scope to the other party of a linked partner record.
     * The caller must be a participant; the grant applies to the counterpart.
     */
    @Transactional
    public PartnerAccess setGrant(UUID partnerId, UUID callerUserId, AccessScope scope, boolean enabled) {
        Partner partner = requireParticipant(partnerId, callerUserId);
        UUID counterpart = callerUserId.equals(partner.getOwnerUserId())
                ? partner.getLinkedUserId()
                : partner.getOwnerUserId();
        if (counterpart == null) {
            throw new NotFoundException("Partner is not linked to an account");
        }
        PartnerAccess access = partnerAccessRepository
                .findByPartnerIdAndGrantedToUserIdAndScope(partnerId, counterpart, scope)
                .orElseGet(() -> {
                    PartnerAccess a = new PartnerAccess();
                    a.setPartnerId(partnerId);
                    a.setGrantedByUserId(callerUserId);
                    a.setGrantedToUserId(counterpart);
                    a.setScope(scope);
                    return a;
                });
        // Only the person who owns the shared data may flip its grant.
        if (!access.getGrantedByUserId().equals(callerUserId)) {
            throw new NotFoundException("Grant not found");
        }
        access.setStatus(enabled ? AccessStatus.ACTIVE : AccessStatus.REVOKED);
        return partnerAccessRepository.save(access);
    }

    @Transactional(readOnly = true)
    public List<PartnerAccess> grantsGivenBy(UUID partnerId, UUID callerUserId) {
        requireParticipant(partnerId, callerUserId);
        return partnerAccessRepository.findByPartnerIdAndGrantedByUserId(partnerId, callerUserId);
    }
}
