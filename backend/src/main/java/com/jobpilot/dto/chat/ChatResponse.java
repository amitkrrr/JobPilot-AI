package com.jobpilot.dto.chat;

import java.util.List;
import com.jobpilot.dto.job.JobResponse;

public class ChatResponse {

    private String response;
    private List<JobResponse> jobs;

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
}
