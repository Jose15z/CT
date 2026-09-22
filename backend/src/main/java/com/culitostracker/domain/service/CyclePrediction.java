package com.culitostracker.domain.service;

import com.culitostracker.domain.model.CyclePhase;

import java.time.LocalDate;

/**
 * Summary prediction for "today". All values are estimates derived from the
 * recorded data; the API and UI must always present them as such.
 */
public record CyclePrediction(LocalDate lastPeriodStart,
                              int cycleLength,
                              int periodLength,
                              LocalDate nextPeriodStart,
                              LocalDate ovulationDate,
                              LocalDate fertileWindowStart,
                              LocalDate fertileWindowEnd,
                              int currentCycleDay,
                              CyclePhase currentPhase) {
}
