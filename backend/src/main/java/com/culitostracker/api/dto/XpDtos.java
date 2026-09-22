package com.culitostracker.api.dto;

import java.util.List;
import java.util.UUID;

public final class XpDtos {

    private XpDtos() {
    }

    public record XpPartnerBreakdown(UUID partnerId,
                                     String partnerName,
                                     int encounters,
                                     long xp) {
    }

    public record XpResponse(long totalXp,
                             int level,
                             String titleKey,
                             long xpIntoLevel,
                             long xpForNextLevel,
                             int encountersCount,
                             int loyaltyStreak,
                             String loyaltyPartnerName,
                             boolean exclusiveBonusActive,
                             List<String> badges,
                             List<XpPartnerBreakdown> breakdown) {
    }
}
