package com.culitostracker.domain.service;

import com.culitostracker.domain.model.Relationship;
import com.culitostracker.domain.model.RelationshipSituation;
import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.model.RelationshipType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelationshipRulesTest {

    private final RelationshipRules rules = new RelationshipRules();

    private Relationship active(RelationshipType type) {
        Relationship r = new Relationship();
        r.setType(type);
        r.setStatus(RelationshipStatus.ACTIVE);
        return r;
    }

    // MONOGAMOUS + existing active romantic partner → reject
    @Test
    void monogamousRejectsSecondActiveRomanticPartner() {
        assertThatThrownBy(() -> rules.validateExclusivity(
                RelationshipType.DATING, RelationshipStatus.ACTIVE,
                List.of(active(RelationshipType.MONOGAMOUS)), RelationshipSituation.IN_RELATIONSHIP))
                .isInstanceOf(DomainRuleException.class)
                .hasFieldOrPropertyWithValue("code", "relationship.monogamousConflict");
    }

    @Test
    void cannotDeclareMonogamousWithAnotherActiveRomantic() {
        assertThatThrownBy(() -> rules.validateExclusivity(
                RelationshipType.MONOGAMOUS, RelationshipStatus.ACTIVE,
                List.of(active(RelationshipType.DATING)), RelationshipSituation.SINGLE))
                .isInstanceOf(DomainRuleException.class)
                .hasFieldOrPropertyWithValue("code", "relationship.monogamousConflict");
    }

    @Test
    void monogamousAllowsCoexistingCasualRecord() {
        // CASUAL is not a romantic type; it does not clash with monogamy rules.
        assertThatCode(() -> rules.validateExclusivity(
                RelationshipType.CASUAL, RelationshipStatus.ACTIVE,
                List.of(active(RelationshipType.MONOGAMOUS)), RelationshipSituation.IN_RELATIONSHIP))
                .doesNotThrowAnyException();
    }

    @Test
    void secondActiveMarriageRejected() {
        assertThatThrownBy(() -> rules.validateExclusivity(
                RelationshipType.MARRIED, RelationshipStatus.ACTIVE,
                List.of(active(RelationshipType.MARRIED)), RelationshipSituation.MARRIED))
                .isInstanceOf(DomainRuleException.class)
                .hasFieldOrPropertyWithValue("code", "relationship.marriageConflict");
    }

    @Test
    void polyamorousSituationAllowsSecondMarriage() {
        assertThatCode(() -> rules.validateExclusivity(
                RelationshipType.MARRIED, RelationshipStatus.ACTIVE,
                List.of(active(RelationshipType.MARRIED)), RelationshipSituation.POLYAMOROUS))
                .doesNotThrowAnyException();
    }

    @Test
    void endedRelationshipsNeverConflict() {
        assertThatCode(() -> rules.validateExclusivity(
                RelationshipType.MONOGAMOUS, RelationshipStatus.ENDED,
                List.of(active(RelationshipType.MONOGAMOUS)), RelationshipSituation.SINGLE))
                .doesNotThrowAnyException();
    }

    @Test
    void multipleActiveNonMonogamousRomanticAllowed() {
        // Two DATING relationships can coexist (e.g. single or poly users).
        assertThatCode(() -> rules.validateExclusivity(
                RelationshipType.DATING, RelationshipStatus.ACTIVE,
                List.of(active(RelationshipType.DATING)), RelationshipSituation.SINGLE))
                .doesNotThrowAnyException();
    }

    @Test
    void datesMustBeChronological() {
        assertThatThrownBy(() -> rules.validateDates(
                LocalDate.of(2025, 5, 1), LocalDate.of(2025, 4, 1), null, null,
                null, RelationshipStatus.ACTIVE))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void endDateRequiresEndedStatus() {
        assertThatThrownBy(() -> rules.validateDates(
                LocalDate.of(2024, 1, 1), null, null, null,
                LocalDate.of(2025, 1, 1), RelationshipStatus.ACTIVE))
                .isInstanceOf(DomainRuleException.class)
                .hasFieldOrPropertyWithValue("code", "relationship.dates.endWithoutEnded");
    }

    @Test
    void endDateCannotPrecedeStart() {
        assertThatThrownBy(() -> rules.validateDates(
                LocalDate.of(2024, 6, 1), null, null, null,
                LocalDate.of(2024, 1, 1), RelationshipStatus.ENDED))
                .isInstanceOf(DomainRuleException.class)
                .hasFieldOrPropertyWithValue("code", "relationship.dates.endBeforeStart");
    }

    @Test
    void coherentDatesPass() {
        assertThatCode(() -> rules.validateDates(
                LocalDate.of(2023, 1, 10), LocalDate.of(2023, 3, 1),
                LocalDate.of(2024, 6, 1), LocalDate.of(2025, 2, 14),
                null, RelationshipStatus.ACTIVE))
                .doesNotThrowAnyException();
    }
}
