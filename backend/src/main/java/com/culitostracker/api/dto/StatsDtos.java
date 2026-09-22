package com.culitostracker.api.dto;

import com.culitostracker.domain.model.RelationshipSituation;

public final class StatsDtos {

    private StatsDtos() {
    }

    public record StatsResponse(long partnersRegistered,
                                long uniquePartners,
                                long activeRelationships,
                                long seriousRelationships,
                                long casualRelationships,
                                PartnerDtos.DurationDto longestRelationship,
                                String longestRelationshipPartnerName,
                                long milestonesCount,
                                RelationshipSituation situation,
                                Integer leaderboardRank) {
    }
}
