package com.jobpilot.job;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobpilot.dto.job.JobRequest;
import com.jobpilot.dto.job.JobResponse;
import com.jobpilot.dto.job.ParsedJobRequirementResponse;
import com.jobpilot.user.User;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobRequirementRepository jobRequirementRepository;
    private final AIJobClient aiJobClient;

    public JobService(JobRepository jobRepository,
                      JobRequirementRepository jobRequirementRepository,
                      AIJobClient aiJobClient) {
        this.jobRepository = jobRepository;
        this.jobRequirementRepository = jobRequirementRepository;
        this.aiJobClient = aiJobClient;
    }

    @Transactional
    public JobResponse createJob(JobRequest request, User user) {
        Job job = new Job(
            user,
            request.getTitle(),
            request.getCompany(),
            request.getLocation(),
            request.getWorkStyle(),
            request.getDescription(),
            request.getSourceUrl(),
            request.getSalaryInfo(),
            request.getExperienceRequired()
        );

        Job savedJob = jobRepository.save(job);
        return new JobResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getJobsForUser(User user) {
        return jobRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(JobResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public JobResponse getJobResponseForUser(Long id, User user) {
        Job job = jobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Job not found or unauthorized"));
        
        // Transparently parse and save job requirements on the fly if not already parsed
        if (job.getRequirements() == null) {
            try {
                ParsedJobRequirementResponse parsedRequirement = aiJobClient.parseJobDescription(job.getDescription());
                JobRequirement requirement = new JobRequirement(
                    job,
                    parsedRequirement.getRequiredSkills(),
                    parsedRequirement.getPreferredSkills(),
                    parsedRequirement.getMinimumExperience(),
                    parsedRequirement.getRequiredEducation(),
                    parsedRequirement.getResponsibilities()
                );
                JobRequirement savedRequirement = jobRequirementRepository.save(requirement);
                job.setRequirements(savedRequirement);
            } catch (Exception e) {
                System.err.println("Failed to parse and save job requirements: " + e.getMessage());
                // Handle fallback gracefully to keep core application resilient
            }
        }
        
        return new JobResponse(job);
    }

    @Transactional(readOnly = true)
    public Job getJobEntityForUser(Long id, User user) {
        return jobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Job not found or unauthorized"));
    }

    @Transactional
    public void deleteJob(Long id, User user) {
        Job job = jobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Job not found or unauthorized"));
        jobRepository.delete(job);
    }
}
