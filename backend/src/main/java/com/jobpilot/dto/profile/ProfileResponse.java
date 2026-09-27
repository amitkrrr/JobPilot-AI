package com.jobpilot.dto.profile;

public class ProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String location;
    private String headline;
    private Integer yearsOfExperience;
    private String currentCompany;
    private String currentRole;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;

    public ProfileResponse(
            Long id,
            String name,
            String email,
            String phone,
            String location,
            String headline,
            Integer yearsOfExperience,
            String currentCompany,
            String currentRole,
            String linkedinUrl,
            String githubUrl,
            String portfolioUrl) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.location = location;
        this.headline = headline;
        this.yearsOfExperience = yearsOfExperience;
        this.currentCompany = currentCompany;
        this.currentRole = currentRole;
        this.linkedinUrl = linkedinUrl;
        this.githubUrl = githubUrl;
        this.portfolioUrl = portfolioUrl;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getLocation() {
        return location;
    }

    public String getHeadline() {
        return headline;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }
}