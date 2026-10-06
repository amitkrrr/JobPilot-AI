package com.jobpilot.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class JobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Company name is required")
    private String company;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Work style is required")
    @Pattern(regexp = "REMOTE|HYBRID|ONSITE", message = "Work style must be one of: REMOTE, HYBRID, ONSITE")
    private String workStyle;

    @NotBlank(message = "Job description is required")
    private String description;

    private String sourceUrl;

    private String salaryInfo;

    private String experienceRequired;

    public JobRequest() {
    }

    public JobRequest(String title, String company, String location, String workStyle, String description,
                      String sourceUrl, String salaryInfo, String experienceRequired) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.workStyle = workStyle;
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.salaryInfo = salaryInfo;
        this.experienceRequired = experienceRequired;
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
}
