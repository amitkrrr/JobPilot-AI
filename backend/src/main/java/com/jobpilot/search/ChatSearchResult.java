package com.jobpilot.search;

import java.util.List;

/**
 * Final result of a multi-source job search — returned to ChatController.
 */
public class ChatSearchResult {

    private String summary;
    private List<NormalizedJob> jobs;
    private List<JobSourceResult> sourceResults;

    public ChatSearchResult(String summary, List<NormalizedJob> jobs, List<JobSourceResult> sourceResults) {
        this.summary = summary;
        this.jobs = jobs;
        this.sourceResults = sourceResults;
    }

    public String getSummary() { return summary; }
    public List<NormalizedJob> getJobs() { return jobs; }
    public List<JobSourceResult> getSourceResults() { return sourceResults; }
}
