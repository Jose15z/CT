package com.culitostracker.api.dto;

import com.culitostracker.domain.model.CyclePhase;
import com.culitostracker.domain.model.CycleProfile;
import com.culitostracker.domain.model.PeriodRecord;
import com.culitostracker.domain.service.CycleDayInfo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CycleDtos {

    private CycleDtos() {
    }

    /** Disclaimer keys attached to every prediction payload. */
    public static final List<String> DISCLAIMER_KEYS = List.of(
            "cycle.disclaimer.estimate",
            "cycle.disclaimer.notContraception");

    public record CycleProfileResponse(UUID id,
                                       UUID partnerId,
                                       int averageCycleLength,
                                       int averagePeriodLength,
                                       LocalDate lastPeriodStartDate,
                                       boolean trackingEnabled) {

        public static CycleProfileResponse from(CycleProfile p) {
            return new CycleProfileResponse(p.getId(), p.getPartnerId(),
                    p.getAverageCycleLength(), p.getAveragePeriodLength(),
                    p.getLastPeriodStartDate(), p.isTrackingEnabled());
        }
    }

    /** PATCH semantics: null fields are left untouched. */
    public record UpdateCycleProfileRequest(
            @Min(15) @Max(60) Integer averageCycleLength,
            @Min(1) @Max(12) Integer averagePeriodLength,
            LocalDate lastPeriodStartDate,
            Boolean trackingEnabled) {
    }

    public record CreatePeriodRequest(LocalDate startDate,   // defaults to today
                                      LocalDate endDate,
                                      @Size(max = 2000) String notes) {
    }

    /** PATCH semantics: null fields are left untouched. */
    public record UpdatePeriodRequest(LocalDate startDate,
                                      LocalDate endDate,
                                      @Size(max = 2000) String notes) {
    }

    public record PeriodRecordResponse(UUID id,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       String notes) {

        public static PeriodRecordResponse from(PeriodRecord r) {
            return new PeriodRecordResponse(r.getId(), r.getStartDate(), r.getEndDate(), r.getNotes());
        }
    }

    public record DayDto(LocalDate date,
                         Integer cycleDay,
                         CyclePhase phase,
                         boolean actualPeriod,
                         boolean predictedPeriod,
                         boolean fertile,
                         boolean ovulation) {

        public static DayDto from(CycleDayInfo info) {
            return new DayDto(info.date(), info.cycleDay(), info.phase(),
                    info.actualPeriod(), info.predictedPeriod(), info.fertile(), info.ovulation());
        }
    }

    public record PredictionsResponse(boolean insufficientData,
                                      boolean trackingEnabled,
                                      Integer cycleLength,
                                      Integer periodLength,
                                      LocalDate lastPeriodStart,
                                      LocalDate nextPeriodStart,
                                      LocalDate ovulationDate,
                                      LocalDate fertileWindowStart,
                                      LocalDate fertileWindowEnd,
                                      Integer currentCycleDay,
                                      CyclePhase currentPhase,
                                      List<DayDto> days,
                                      List<String> disclaimers) {

        public static PredictionsResponse insufficient(boolean trackingEnabled, List<DayDto> days) {
            return new PredictionsResponse(true, trackingEnabled, null, null, null, null,
                    null, null, null, null, null, days, DISCLAIMER_KEYS);
        }
    }
}
