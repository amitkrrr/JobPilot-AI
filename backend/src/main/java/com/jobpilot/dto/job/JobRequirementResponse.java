package com.jobpilot.dto.job;

import java.util.ArrayList;
import java.util.List;

import com.jobpilot.job.JobRequirement;

public class JobRequirementResponse {

    private Long id;
    private List<String> requiredSkills = new ArrayList<>();
    private List<String> preferredSkills = new ArrayList<>();
    private Integer minimumExperience;
    private String requiredEducation;
    private List<String> responsibilities = new ArrayList<>();

    public JobRequirementResponse() {
    }

    public JobRequirementResponse(JobRequirement requirement) {
        if (requirement != null) {
            this.id = requirement.getId();
            this.requiredSkills = requirement.getRequiredSkills() != null 
                    ? new ArrayList<>(requirement.getRequiredSkills()) : new ArrayList<>();
            this.preferredSkills = requirement.getPreferredSkills() != null 
                    ? new ArrayList<>(requirement.getPreferredSkills()) : new ArrayList<>();
            this.minimumExperience = requirement.getMinimumExperience();
            this.requiredEducation = requirement.getRequiredEducation();
            this.responsibilities = requirement.getResponsibilities() != null 
                    ? new ArrayList<>(requirement.getResponsibilities()) : new ArrayList<>();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public List<String> getPreferredSkills() {
        return preferredSkills;
    }

    public void setPreferredSkills(List<String> preferredSkills) {
        this.preferredSkills = preferredSkills;
    }

    public Integer getMinimumExperience() {
        return minimumExperience;
    }

    public void setMinimumExperience(Integer minimumExperience) {
        this.minimumExperience = minimumExperience;
    }

    public String getRequiredEducation() {
        return requiredEducation;
    }

    public void setRequiredEducation(String requiredEducation) {
        this.requiredEducation = requiredEducation;
    }

    public List<String> getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(List<String> responsibilities) {
        this.responsibilities = responsibilities;
    }
}
