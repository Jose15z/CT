package com.culitostracker.domain.service;

import com.culitostracker.domain.model.AdviceCategory;
import com.culitostracker.domain.model.AdviceSource;

import java.util.Map;

/**
 * One piece of advice. messageKey is an i18n key resolved in the frontend
 * with the given params, so the same advice renders in ES and EN.
 */
public record AdviceCandidate(AdviceCategory category,
                              String messageKey,
                              Map<String, Object> params,
                              int priority,
                              AdviceSource source) {
}
