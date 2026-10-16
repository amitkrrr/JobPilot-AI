package com.jobpilot.search;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Structured search intent produced by Gemini intent classification.
 * Spring Boot uses this to drive all connectors — not raw natural language.
 */
public class JobSearchRequest {

    private String role;
    private List<String> skills;
    private String location;
    private Integer experienceMin;
    private Integer experienceMax;
    private boolean remote;
    private int page = 0;

    public JobSearchRequest() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getExperienceMin() {
        return experienceMin;
    }

    public void setExperienceMin(Integer experienceMin) {
        this.experienceMin = experienceMin;
    }

    public Integer getExperienceMax() {
        return experienceMax;
    }

    public void setExperienceMax(Integer experienceMax) {
        this.experienceMax = experienceMax;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Stable cache key used by @Cacheable in JobSearchService.
     * Format: "role|skill1,skill2|location|remote|page"
     */
    public String cacheKey() {
        String roleKey = role != null ? role.toLowerCase().trim() : "";
        String skillsKey = skills != null
                ? skills.stream().map(String::toLowerCase).sorted().collect(Collectors.joining(","))
                : "";
        String locationKey = location != null ? location.toLowerCase().trim() : "";
        return roleKey + "|" + skillsKey + "|" + locationKey + "|" + remote + "|" + page;
    }
}
