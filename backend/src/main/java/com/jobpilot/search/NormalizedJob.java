package com.jobpilot.search;

import java.io.Serializable;
import java.util.List;

/**
 * Platform-agnostic job model. Every connector maps its raw response
 * into this structure so all downstream logic works with one model.
 */
public class NormalizedJob implements Serializable {

    private String sourceJobId;
    private String source;
    private String title;
    private String company;
    private String location;
    private String description;
    private String jobUrl;
    private String employmentType;
    private Integer experienceMin;
    private Integer experienceMax;
    private String salaryInfo;
    private List<String> skills;

    public NormalizedJob() {
    }

    public String getSourceJobId() { return sourceJobId; }
    public void setSourceJobId(String sourceJobId) { this.sourceJobId = sourceJobId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public Integer getExperienceMin() { return experienceMin; }
    public void setExperienceMin(Integer experienceMin) { this.experienceMin = experienceMin; }

    public Integer getExperienceMax() { return experienceMax; }
    public void setExperienceMax(Integer experienceMax) { this.experienceMax = experienceMax; }

    public String getSalaryInfo() { return salaryInfo; }
    public void setSalaryInfo(String salaryInfo) { this.salaryInfo = salaryInfo; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    /** Heuristic: treat as remote if location string says "remote". */
    public boolean isRemoteByLocation() {
        return location != null && location.toLowerCase().contains("remote");
    }
}
