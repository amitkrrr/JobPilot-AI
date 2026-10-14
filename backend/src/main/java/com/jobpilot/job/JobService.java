package com.jobpilot.job;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jobpilot.dto.job.JobRequest;
import com.jobpilot.dto.job.JobResponse;
import com.jobpilot.dto.job.MatchScoreResponse;
import com.jobpilot.dto.job.ParsedJobRequirementResponse;
import com.jobpilot.user.CandidateProfile;
import com.jobpilot.user.CandidateProfileRepository;
import com.jobpilot.user.User;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final JobRequirementRepository jobRequirementRepository;
    private final AIJobClient aiJobClient;
    private final CandidateProfileRepository candidateProfileRepository;

    public JobService(JobRepository jobRepository,
                      JobRequirementRepository jobRequirementRepository,
                      AIJobClient aiJobClient,
                      CandidateProfileRepository candidateProfileRepository) {
        this.jobRepository = jobRepository;
        this.jobRequirementRepository = jobRequirementRepository;
        this.aiJobClient = aiJobClient;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    @Transactional
    public JobResponse createJob(JobRequest request, User user) {
        if (jobRepository.existsByUserAndTitleIgnoreCaseAndCompanyIgnoreCase(user, request.getTitle(), request.getCompany())) {
            throw new IllegalArgumentException("Job already saved: " + request.getTitle() + " at " + request.getCompany());
        }

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
                log.warn("Failed to parse job requirements for job {}: {}", id, e.getMessage());
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
    public JobResponse updateJobStatus(Long id, JobStatus status, User user) {
        Job job = jobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Job not found or unauthorized"));
        job.setStatus(status);
        Job savedJob = jobRepository.save(job);
        return new JobResponse(savedJob);
    }

    @Transactional
    public JobResponse updateJobNotes(Long id, String notes, User user) {
        Job job = jobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Job not found or unauthorized"));
        job.setNotes(notes);
        Job savedJob = jobRepository.save(job);
        return new JobResponse(savedJob);
    }

    @Transactional
    public MatchScoreResponse getMatchScore(Long id, User user) {
        // Transparently parse requirements if missing
        getJobResponseForUser(id, user);
        Job job = getJobEntityForUser(id, user);
        CandidateProfile profile = candidateProfileRepository.findByUserId(user.getId())
                .orElse(null);
        return aiJobClient.scoreMatch(job, profile);
    }

    @Transactional
    public void deleteJob(Long id, User user) {
        Job job = jobRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Job not found or unauthorized"));
        jobRepository.delete(job);
    }
}
