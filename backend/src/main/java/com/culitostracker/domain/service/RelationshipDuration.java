package com.culitostracker.domain.service;

public record RelationshipDuration(int years, int months, int days, long totalDays) {

    public long totalMonths() {
        return years * 12L + months;
    }
}
