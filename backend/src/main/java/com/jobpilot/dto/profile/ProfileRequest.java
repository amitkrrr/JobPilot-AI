package com.jobpilot.dto.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ProfileRequest {

    @Size(max = 20, message = "Phone number is too long")
    private String phone;

    @Size(max = 100, message = "Location is too long")
    private String location;

    @Size(max = 200, message = "Headline is too long")
    private String headline;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 60, message = "Years of experience is invalid")
    private Integer yearsOfExperience;

    @Size(max = 150, message = "Company name is too long")
    private String currentCompany;

    @Size(max = 150, message = "Role is too long")
    private String currentRole;

    @Size(max = 300, message = "LinkedIn URL is too long")
    private String linkedinUrl;

    @Size(max = 300, message = "GitHub URL is too long")
    private String githubUrl;

    @Size(max = 300, message = "Portfolio URL is too long")
    private String portfolioUrl;

    public ProfileRequest() {
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }
}