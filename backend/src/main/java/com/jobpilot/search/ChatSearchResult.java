package com.jobpilot.search;

import java.io.Serializable;
import java.util.List;

/**
 * Final result of a multi-source job search — returned to ChatController.
 * Must be Serializable for Redis cache serialization.
 */
public class ChatSearchResult implements Serializable {

    private String summary;
    private List<NormalizedJob> jobs;
    private List<JobSourceResult> sourceResults;

    /** Required by Jackson for Redis deserialization. */
    public ChatSearchResult() {
    }

    public ChatSearchResult(String summary, List<NormalizedJob> jobs, List<JobSourceResult> sourceResults) {
        this.summary = summary;
        this.jobs = jobs;
        this.sourceResults = sourceResults;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<NormalizedJob> getJobs() { return jobs; }
    public void setJobs(List<NormalizedJob> jobs) { this.jobs = jobs; }

    public List<JobSourceResult> getSourceResults() { return sourceResults; }
    public void setSourceResults(List<JobSourceResult> sourceResults) { this.sourceResults = sourceResults; }
}
