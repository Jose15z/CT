package com.culitostracker.api.dto;

import java.time.LocalDate;
import java.util.List;

public final class TrendsDtos {

    private TrendsDtos() {
    }

    /** One day; nulls where nothing was recorded. Moods are mapped to 1–5. */
    public record TrendDay(LocalDate date,
                           Double myMood,
                           Double myEnergy,
                           Double myStress,
                           Double mySatisfaction,
                           Double partnerMood) {
    }

    public record MonthCount(String month, long count) {
    }

    public record TrendsResponse(LocalDate from,
                                 LocalDate to,
                                 int myCheckIns,
                                 int partnerCheckIns,
                                 Double myMoodAverage,
                                 Double mySatisfactionAverage,
                                 List<TrendDay> days,
                                 List<MonthCount> encountersByMonth) {
    }
}
