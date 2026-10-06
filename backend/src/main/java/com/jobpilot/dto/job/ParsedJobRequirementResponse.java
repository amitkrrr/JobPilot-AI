package com.jobpilot.dto.job;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ParsedJobRequirementResponse {

    @JsonProperty("required_skills")
    private List<String> requiredSkills;

    @JsonProperty("preferred_skills")
    private List<String> preferredSkills;

    @JsonProperty("minimum_experience")
    private Integer minimumExperience;

    @JsonProperty("required_education")
    private String requiredEducation;

    private List<String> responsibilities;

    public ParsedJobRequirementResponse() {
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
