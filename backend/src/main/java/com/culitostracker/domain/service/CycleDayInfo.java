package com.culitostracker.domain.service;

import com.culitostracker.domain.model.CyclePhase;

import java.time.LocalDate;

/**
 * Classification of a single calendar day.
 *
 * @param cycleDay      1-based day within the cycle, null when unknown
 * @param phase         null when the day cannot be classified
 * @param actualPeriod  the day falls inside a recorded period
 * @param predictedPeriod the day falls inside a projected (future) period
 * @param fertile       inside the estimated fertile window
 * @param ovulation     the estimated ovulation day
 */
public record CycleDayInfo(LocalDate date,
                           Integer cycleDay,
                           CyclePhase phase,
                           boolean actualPeriod,
                           boolean predictedPeriod,
                           boolean fertile,
                           boolean ovulation) {
}
