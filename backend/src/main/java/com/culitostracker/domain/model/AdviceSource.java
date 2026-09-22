package com.culitostracker.domain.model;

/**
 * Where an advice signal comes from. The UI wording depends on it:
 * a self-report is "Laura said...", an observation is "You noted...",
 * a cycle signal is always "estimated".
 */
public enum AdviceSource {
    SELF_REPORT,
    OBSERVATION,
    CYCLE,
    RELATIONSHIP
}
