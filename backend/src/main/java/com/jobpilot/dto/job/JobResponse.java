package com.jobpilot.dto.job;

import java.time.LocalDateTime;

import com.jobpilot.job.Job;
import com.jobpilot.job.JobStatus;

public class JobResponse {

    private Long id;
    private String title;
    private String company;
    private String location;
    private String workStyle;
    private String description;
    private String sourceUrl;
    private String salaryInfo;
    private String experienceRequired;
    private JobStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private JobRequirementResponse requirements;

    public JobResponse() {
    }

    public JobResponse(Job job) {
        this.id = job.getId();
        this.title = job.getTitle();
        this.company = job.getCompany();
        this.location = job.getLocation();
        this.workStyle = job.getWorkStyle();
        this.description = job.getDescription();
        this.sourceUrl = job.getSourceUrl();
        this.salaryInfo = job.getSalaryInfo();
        this.experienceRequired = job.getExperienceRequired();
        this.status = job.getStatus();
        this.notes = job.getNotes();
        this.createdAt = job.getCreatedAt();
        if (job.getRequirements() != null) {
            this.requirements = new JobRequirementResponse(job.getRequirements());
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWorkStyle() {
        return workStyle;
    }

    public void setWorkStyle(String workStyle) {
        this.workStyle = workStyle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSalaryInfo() {
        return salaryInfo;
    }

    public void setSalaryInfo(String salaryInfo) {
        this.salaryInfo = salaryInfo;
    }

    public String getExperienceRequired() {
        return experienceRequired;
    }

    public void setExperienceRequired(String experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public JobRequirementResponse getRequirements() {
        return requirements;
    }

    public void setRequirements(JobRequirementResponse requirements) {
        this.requirements = requirements;
    }
}
