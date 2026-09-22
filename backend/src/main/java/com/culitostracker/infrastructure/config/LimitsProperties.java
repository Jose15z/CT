package com.culitostracker.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.limits")
public record LimitsProperties(int partnersPerDay, int encountersPerDay) {
}
