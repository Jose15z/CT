package com.culitostracker.application;

import com.culitostracker.api.dto.InviteDtos.GrantState;
import com.culitostracker.api.dto.InviteDtos.InvitePreview;
import com.culitostracker.api.dto.InviteDtos.InviteResponse;
import com.culitostracker.api.dto.InviteDtos.LinkResponse;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.PartnerInvite;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.infrastructure.config.AppUrls;
import com.culitostracker.repository.PartnerAccessRepository;
import com.culitostracker.repository.PartnerInviteRepository;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.RelationshipRepository;
import com.culitostracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Links the real person behind a partner record to their own account.
 * The owner creates a one-time invite link; whoever accepts it becomes the
 * partner's linked user and can then decide what to share back (grants).
 */
@Service
public class InviteService {

    static final Duration INVITE_TTL = Duration.ofDays(7);

    private final PartnerInviteRepository inviteRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerAccessRepository accessRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final PartnerAccessService partnerAccessService;
    private final AppUrls appUrls;
    private final SecureRandom secureRandom = new SecureRandom();

    public InviteService(PartnerInviteRepository inviteRepository,
                         PartnerRepository partnerRepository,
                         PartnerAccessRepository accessRepository,
                         RelationshipRepository relationshipRepository,
                         UserRepository userRepository,
                         PartnerAccessService partnerAccessService,
                         AppUrls appUrls) {
        this.inviteRepository = inviteRepository;
        this.partnerRepository = partnerRepository;
        this.accessRepository = accessRepository;
        this.relationshipRepository = relationshipRepository;
        this.userRepository = userRepository;
        this.partnerAccessService = partnerAccessService;
        this.appUrls = appUrls;
    }

    @Transactional
    public InviteResponse create(UUID userId, UUID partnerId) {
        Partner partner = partnerAccessService.requireOwned(partnerId, userId);
        if (partner.getLinkedUserId() != null) {
            throw new DomainRuleException("invite.alreadyLinked", "Partner is already linked to an account");
        }
        inviteRepository.deletePendingByPartnerId(partnerId);

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PartnerInvite invite = new PartnerInvite();
        invite.setPartnerId(partnerId);
        invite.setCreatedByUserId(userId);
        invite.setTokenHash(AuthService.sha256(rawToken));
        invite.setExpiresAt(Instant.now().plus(INVITE_TTL));
        inviteRepository.save(invite);

        return new InviteResponse(appUrls.invite(rawToken), invite.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public InvitePreview preview(String rawToken) {
        PartnerInvite invite = usable(rawToken);
        Partner partner = partnerRepository.findById(invite.getPartnerId())
                .orElseThrow(InviteService::invalid);
        User inviter = userRepository.findById(invite.getCreatedByUserId())
                .orElseThrow(InviteService::invalid);
        Relationship relationship = relationshipRepository.findByPartnerId(partner.getId()).orElse(null);
        return new InvitePreview(inviter.getDisplayName(), partner.getName(),
                relationship != null ? relationship.getType() : null, invite.getExpiresAt());
    }

    @Transactional
    public LinkResponse accept(UUID userId, String rawToken) {
        PartnerInvite invite = usable(rawToken);
        Partner partner = partnerRepository.findById(invite.getPartnerId())
                .orElseThrow(InviteService::invalid);
        if (partner.isDeleted()) {
            throw invalid();
        }
        if (partner.getOwnerUserId().equals(userId)) {
            throw new DomainRuleException("invite.selfLink", "You cannot accept your own invite");
        }
        if (partner.getLinkedUserId() != null) {
            throw new DomainRuleException("invite.alreadyLinked", "Partner is already linked to an account");
        }
        partner.setLinkedUserId(userId);
        partnerRepository.save(partner);
        invite.setAcceptedByUserId(userId);
        invite.setAcceptedAt(Instant.now());
        inviteRepository.save(invite);
        return toLink(partner, userId);
    }

    /** Partner records where the caller is the linked person. */
    @Transactional(readOnly = true)
    public List<LinkResponse> myLinks(UUID userId) {
        return partnerRepository.findByLinkedUserIdAndDeletedAtIsNull(userId).stream()
                .map(partner -> toLink(partner, userId))
                .toList();
    }

    /** Either side can break the link; every grant for that record dies with it. */
    @Transactional
    public void unlink(UUID userId, UUID partnerId) {
        Partner partner = partnerAccessService.requireParticipant(partnerId, userId);
        partner.setLinkedUserId(null);
        partnerRepository.save(partner);
        accessRepository.deleteByPartnerId(partnerId);
        inviteRepository.deletePendingByPartnerId(partnerId);
    }

    private PartnerInvite usable(String rawToken) {
        PartnerInvite invite = inviteRepository.findByTokenHash(AuthService.sha256(rawToken))
                .orElseThrow(InviteService::invalid);
        if (!invite.isUsable(Instant.now())) {
            throw invalid();
        }
        return invite;
    }

    private LinkResponse toLink(Partner partner, UUID me) {
        User owner = userRepository.findById(partner.getOwnerUserId())
                .orElseThrow(() -> new NotFoundException("Owner not found"));
        Relationship relationship = relationshipRepository.findByPartnerId(partner.getId()).orElse(null);
        List<GrantState> byMe = accessRepository.findByPartnerIdAndGrantedByUserId(partner.getId(), me).stream()
                .map(a -> new GrantState(a.getScope(), a.getStatus())).toList();
        List<GrantState> toMe = accessRepository.findByPartnerIdAndGrantedByUserId(partner.getId(), owner.getId()).stream()
                .map(a -> new GrantState(a.getScope(), a.getStatus())).toList();
        return new LinkResponse(partner.getId(), owner.getDisplayName(), owner.getUsername(),
                partner.getName(), relationship != null ? relationship.getType() : null, byMe, toMe);
    }

    private static DomainRuleException invalid() {
        return new DomainRuleException("invite.invalid", "Invite is invalid, expired or already used");
    }
}
