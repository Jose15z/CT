package com.culitostracker.domain.service;

import java.time.LocalDate;

/**
 * @param date      next anniversary date (today if it is today)
 * @param daysUntil 0 when the anniversary is today
 * @param years     how many years the couple completes on that date
 */
public record AnniversaryInfo(LocalDate date, long daysUntil, int years) {
}
