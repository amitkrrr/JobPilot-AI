package com.jobpilot.search;

import java.io.Serializable;
import java.util.List;

/**
 * Result envelope returned by every JobSource connector.
 * Carries status alongside jobs so the orchestrator can build
 * a transparent message like "24 jobs from Adzuna; Jooble unavailable."
 */
public class JobSourceResult implements Serializable {

    private String source;
    private SearchStatus status;
    private List<NormalizedJob> jobs;
    private String error;

    /** Required by Jackson for Redis deserialization. */
    public JobSourceResult() {
    }

    public JobSourceResult(String source, SearchStatus status, List<NormalizedJob> jobs) {
        this.source = source;
        this.status = status;
        this.jobs = jobs;
    }

    public JobSourceResult(String source, SearchStatus status, String error) {
        this.source = source;
        this.status = status;
        this.jobs = java.util.Collections.emptyList();
        this.error = error;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public SearchStatus getStatus() { return status; }
    public void setStatus(SearchStatus status) { this.status = status; }

    public List<NormalizedJob> getJobs() { return jobs; }
    public void setJobs(List<NormalizedJob> jobs) { this.jobs = jobs; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
