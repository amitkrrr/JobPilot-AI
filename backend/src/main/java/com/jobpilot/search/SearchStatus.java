package com.jobpilot.search;

/**
 * Status of a single connector's search attempt.
 * The orchestrator uses this to build a transparent summary message.
 */
public enum SearchStatus {
    SUCCESS,
    NO_RESULTS,
    UNAVAILABLE,
    UNAUTHORIZED,
    RATE_LIMITED,
    ERROR
}
