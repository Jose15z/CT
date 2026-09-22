package com.culitostracker.domain.service;

import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipSituation;
import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.model.RelationshipType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain rules for relationship exclusivity and date coherence. Enforced in
 * the backend on create, update and reactivate — never only in the UI.
 */
@Service
public class RelationshipRules {

    /**
     * @param type                     type being set
     * @param status                   status being set
     * @param otherActiveRelationships the user's OTHER relationships that are currently ACTIVE
     * @param situation                the user's self-declared situation
     */
    public void validateExclusivity(RelationshipType type,
                                    RelationshipStatus status,
                                    List<Relationship> otherActiveRelationships,
                                    RelationshipSituation situation) {
        if (status != RelationshipStatus.ACTIVE) {
            return; // paused/inactive/ended relationships never conflict
        }

        boolean otherActiveRomantic = otherActiveRelationships.stream()
                .anyMatch(Relationship::isActiveRomantic);
        boolean otherActiveMonogamous = otherActiveRelationships.stream()
                .anyMatch(r -> r.getStatus() == RelationshipStatus.ACTIVE
                        && r.getType() == RelationshipType.MONOGAMOUS);
        boolean otherActiveMarriage = otherActiveRelationships.stream()
                .anyMatch(r -> r.getStatus() == RelationshipStatus.ACTIVE
                        && r.getType() == RelationshipType.MARRIED);

        // An explicitly monogamous relationship is exclusive with any other
        // active romantic relationship, in both directions and regardless of
        // the user's declared situation: the contradiction is in the data itself.
        if (type == RelationshipType.MONOGAMOUS && otherActiveRomantic) {
            throw new DomainRuleException("relationship.monogamousConflict",
                    "A monogamous relationship cannot coexist with another active romantic relationship");
        }
        if (type.isRomantic() && otherActiveMonogamous) {
            throw new DomainRuleException("relationship.monogamousConflict",
                    "There is already an active monogamous relationship");
        }

        // One active marriage, unless the user declares a polyamorous situation.
        if (type == RelationshipType.MARRIED && otherActiveMarriage
                && situation != RelationshipSituation.POLYAMOROUS) {
            throw new DomainRuleException("relationship.marriageConflict",
                    "There is already an active marriage");
        }
    }

    public void validateDates(LocalDate datingStart,
                              LocalDate relationshipStart,
                              LocalDate engagement,
                              LocalDate marriage,
                              LocalDate end,
                              RelationshipStatus status) {
        requireOrder(datingStart, relationshipStart, "relationship.dates.datingAfterStart");
        requireOrder(relationshipStart, engagement, "relationship.dates.startAfterEngagement");
        requireOrder(engagement, marriage, "relationship.dates.engagementAfterMarriage");
        requireOrder(datingStart, marriage, "relationship.dates.datingAfterMarriage");

        if (end != null && status != RelationshipStatus.ENDED) {
            throw new DomainRuleException("relationship.dates.endWithoutEnded",
                    "An end date requires status ENDED");
        }
        LocalDate earliest = firstNonNull(datingStart, relationshipStart, engagement, marriage);
        if (end != null && earliest != null && end.isBefore(earliest)) {
            throw new DomainRuleException("relationship.dates.endBeforeStart",
                    "The end date cannot be before the relationship began");
        }
    }

    private void requireOrder(LocalDate earlier, LocalDate later, String code) {
        if (earlier != null && later != null && earlier.isAfter(later)) {
            throw new DomainRuleException(code, "Relationship dates are out of order");
        }
    }

    private LocalDate firstNonNull(LocalDate... dates) {
        for (LocalDate d : dates) {
            if (d != null) return d;
        }
        return null;
    }
}
