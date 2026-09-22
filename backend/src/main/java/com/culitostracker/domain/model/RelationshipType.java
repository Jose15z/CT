package com.culitostracker.domain.model;

import java.util.Set;

public enum RelationshipType {
    CASUAL,
    DATING,
    SERIOUS_RELATIONSHIP,
    MONOGAMOUS,
    POLYAMOROUS,
    ENGAGED,
    MARRIED,
    FRIENDS_WITH_BENEFITS,
    OTHER;

    private static final Set<RelationshipType> ROMANTIC = Set.of(
            DATING, SERIOUS_RELATIONSHIP, MONOGAMOUS, POLYAMOROUS, ENGAGED, MARRIED);

    /** Romantic types count against monogamy/marriage exclusivity rules. */
    public boolean isRomantic() {
        return ROMANTIC.contains(this);
    }

    /** Types where the relationship timeline and check-ins make the most sense. */
    public boolean isSerious() {
        return this == SERIOUS_RELATIONSHIP || this == MONOGAMOUS
                || this == ENGAGED || this == MARRIED || this == POLYAMOROUS;
    }

    /** Exclusive commitments: these earn the XP engine's couple bonus. */
    public boolean isExclusive() {
        return this == MONOGAMOUS || this == ENGAGED || this == MARRIED;
    }
}
