package com.culitostracker.domain.model;

/**
 * Personalization buckets by relationship duration. A tool for picking advice,
 * not a universal psychological claim.
 */
public enum RelationshipStage {
    NEW,            // 0-3 months
    DEVELOPING,     // 3-12 months
    ESTABLISHED,    // 1-3 years
    LONG_TERM,      // 3-7 years
    VERY_LONG_TERM; // 7+ years

    public static RelationshipStage fromMonths(long months) {
        if (months < 3) return NEW;
        if (months < 12) return DEVELOPING;
        if (months < 36) return ESTABLISHED;
        if (months < 84) return LONG_TERM;
        return VERY_LONG_TERM;
    }
}
