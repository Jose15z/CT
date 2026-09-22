package com.culitostracker.application;

import com.culitostracker.api.dto.XpDtos.XpPartnerBreakdown;
import com.culitostracker.api.dto.XpDtos.XpResponse;
import com.culitostracker.domain.model.Encounter;
import com.culitostracker.domain.model.Partner;
import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.service.XpEngine;
import com.culitostracker.domain.service.XpEngine.EncounterFact;
import com.culitostracker.repository.EncounterRepository;
import com.culitostracker.repository.PartnerRepository;
import com.culitostracker.repository.RelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Private gamification. XP only ever surfaces to its owner; it feeds no
 * public ranking and is recomputed from the encounter log on every read.
 */
@Service
public class XpService {

    /** Named title tiers exist up to this level; beyond it the last one sticks. */
    static final int MAX_TITLE_LEVEL = 10;

    private final EncounterRepository encounterRepository;
    private final PartnerRepository partnerRepository;
    private final RelationshipRepository relationshipRepository;
    private final XpEngine xpEngine;

    public XpService(EncounterRepository encounterRepository,
                     PartnerRepository partnerRepository,
                     RelationshipRepository relationshipRepository,
                     XpEngine xpEngine) {
        this.encounterRepository = encounterRepository;
        this.partnerRepository = partnerRepository;
        this.relationshipRepository = relationshipRepository;
        this.xpEngine = xpEngine;
    }

    @Transactional(readOnly = true)
    public XpResponse me(UUID userId) {
        List<Encounter> encounters = encounterRepository.findByOwnerUserIdOrderByDateAscCreatedAtAscIdAsc(userId);
        Map<UUID, Partner> partners = partnerRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream()
                .collect(Collectors.toMap(Partner::getId, Function.identity()));
        Map<UUID, Relationship> relationships = relationshipRepository.findAllByOwner(userId).stream()
                .collect(Collectors.toMap(Relationship::getPartnerId, Function.identity()));

        List<EncounterFact> facts = encounters.stream()
                .map(e -> {
                    Partner partner = partners.get(e.getPartnerId());
                    Relationship rel = relationships.get(e.getPartnerId());
                    return new EncounterFact(
                            e.getPartnerId(),
                            e.getDate(),
                            partner == null ? null : partner.ageAt(e.getDate()),
                            partner == null ? null : partner.getWeightKg(),
                            exclusiveAt(rel, e));
                })
                .toList();

        XpEngine.XpTotals totals = xpEngine.compute(facts);

        boolean exclusiveNow = relationships.values().stream()
                .anyMatch(r -> r.isActiveRomantic() && r.getType().isExclusive());

        List<XpPartnerBreakdown> breakdown = new ArrayList<>();
        totals.byPartner().forEach((partnerId, xp) -> breakdown.add(new XpPartnerBreakdown(
                partnerId,
                partners.containsKey(partnerId) ? partners.get(partnerId).getName() : null,
                xp.encounters(),
                xp.xp())));
        breakdown.sort((a, b) -> Long.compare(b.xp(), a.xp()));

        List<String> badges = new ArrayList<>();
        if (!encounters.isEmpty()) {
            badges.add("xp.badge.firstSteps");
        }
        if (totals.loyaltyStreak() >= 5) {
            badges.add("xp.badge.loyal");
        }
        if (exclusiveNow) {
            badges.add("xp.badge.exclusive");
        }
        if (encounters.size() >= 50) {
            badges.add("xp.badge.veteran");
        }

        Partner loyaltyPartner = totals.loyaltyPartnerId() == null
                ? null : partners.get(totals.loyaltyPartnerId());

        return new XpResponse(
                totals.totalXp(),
                totals.level(),
                "xp.title." + Math.min(totals.level(), MAX_TITLE_LEVEL),
                totals.xpIntoLevel(),
                totals.xpForNextLevel(),
                encounters.size(),
                totals.loyaltyStreak(),
                loyaltyPartner == null ? null : loyaltyPartner.getName(),
                exclusiveNow,
                badges,
                breakdown);
    }

    /**
     * The couple bonus applies while the exclusive relationship was in effect:
     * always for ongoing ones, and up to the end date for ended ones.
     */
    private static boolean exclusiveAt(Relationship rel, Encounter encounter) {
        if (rel == null || !rel.getType().isExclusive()) {
            return false;
        }
        return rel.getRelationshipEndDate() == null
                || !encounter.getDate().isAfter(rel.getRelationshipEndDate());
    }
}
