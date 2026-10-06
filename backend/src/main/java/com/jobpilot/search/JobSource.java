package com.jobpilot.search;

/**
 * Common interface every job source connector must implement.
 * The orchestrator only calls this — it never knows which platform it is.
 */
public interface JobSource {

    /**
     * Human-readable name of this source, used in status messages.
     * e.g. "Adzuna", "Jooble"
     */
    String getSourceName();

    /**
     * Execute a search against this source.
     * Must never throw — wrap all errors in JobSourceResult with the appropriate SearchStatus.
     */
    JobSourceResult search(JobSearchRequest request);
}
