package com.jobpilot.dto.chat;

import java.util.List;
import com.jobpilot.dto.job.JobResponse;
import com.jobpilot.search.SearchLink;

public class ChatResponse {

    private String response;
    private List<JobResponse> jobs;
    private List<SearchLink> searchLinks = List.of();

    public ChatResponse() {
    }

    public ChatResponse(String response, List<JobResponse> jobs) {
        this.response = response;
        this.jobs = jobs;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public List<JobResponse> getJobs() {
        return jobs;
    }

    public void setJobs(List<JobResponse> jobs) {
        this.jobs = jobs;
    }

    public List<SearchLink> getSearchLinks() {
        return searchLinks;
    }

    public void setSearchLinks(List<SearchLink> searchLinks) {
        this.searchLinks = searchLinks;
    }
}
