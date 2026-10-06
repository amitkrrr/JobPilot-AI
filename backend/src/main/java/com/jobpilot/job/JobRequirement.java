package com.jobpilot.job;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_requirements")
public class JobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_requirement_required_skills", joinColumns = @JoinColumn(name = "job_requirement_id"))
    @Column(name = "skill", nullable = false)
    private List<String> requiredSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_requirement_preferred_skills", joinColumns = @JoinColumn(name = "job_requirement_id"))
    @Column(name = "skill", nullable = false)
    private List<String> preferredSkills = new ArrayList<>();

    @Column(name = "minimum_experience")
    private Integer minimumExperience;

    @Column(name = "required_education")
    private String requiredEducation;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_requirement_responsibilities", joinColumns = @JoinColumn(name = "job_requirement_id"))
    @Column(name = "responsibility", nullable = false, columnDefinition = "text")
    private List<String> responsibilities = new ArrayList<>();

    public JobRequirement() {
    }

    public JobRequirement(Job job, List<String> requiredSkills, List<String> preferredSkills,
                          Integer minimumExperience, String requiredEducation, List<String> responsibilities) {
        this.job = job;
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
        this.preferredSkills = preferredSkills != null ? preferredSkills : new ArrayList<>();
        this.minimumExperience = minimumExperience;
        this.requiredEducation = requiredEducation;
        this.responsibilities = responsibilities != null ? responsibilities : new ArrayList<>();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
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
