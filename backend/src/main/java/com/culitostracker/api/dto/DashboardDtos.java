package com.culitostracker.api.dto;

import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.RelationshipStatus;
import com.culitostracker.domain.model.RelationshipType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardCycleSummary(boolean trackingEnabled,
                                        boolean insufficientData,
                                        CyclePhase currentPhase,
                                        Integer currentCycleDay,
                                        LocalDate nextPeriodStart,
                                        LocalDate ovulationDate) {
    }

    public record DashboardPartner(UUID partnerId,
                                   String name,
                                   String nickname,
                                   String avatarEmoji,
                                   RelationshipType relationshipType,
                                   RelationshipStatus relationshipStatus,
                                   LocalDate togetherSince,
                                   PartnerDtos.DurationDto duration,
                                   PartnerDtos.AnniversaryDto nextAnniversary,
                                   DashboardCycleSummary cycle,
                                   CheckInDtos.CheckInResponse myCheckInToday,
                                   CheckInDtos.CheckInResponse partnerCheckInToday,
                                   ObservationDtos.ObservationResponse latestObservation,
                                   AdviceDtos.AdviceItem adviceOfTheDay) {
    }

    public record DashboardResponse(UserDtos.UserResponse user,
                                    List<DashboardPartner> partners) {
    }
}
