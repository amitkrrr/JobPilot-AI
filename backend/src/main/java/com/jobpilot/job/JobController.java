package com.jobpilot.job;

import java.util.List;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobpilot.dto.job.JobRequest;
import com.jobpilot.dto.job.JobResponse;
import com.jobpilot.dto.job.MatchScoreResponse;
import com.jobpilot.user.User;
import com.jobpilot.user.UserRepository;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;

    public JobController(JobService jobService, UserRepository userRepository) {
        this.jobService = jobService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Unauthorized: No authentication context found");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse createJob(
            Authentication authentication,
            @Valid @RequestBody JobRequest request) {
        User user = getAuthenticatedUser(authentication);
        return jobService.createJob(request, user);
    }

    @GetMapping
    public List<JobResponse> getJobs(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return jobService.getJobsForUser(user);
    }

    @GetMapping("/{id}")
    public JobResponse getJob(
            Authentication authentication,
            @PathVariable Long id) {
        User user = getAuthenticatedUser(authentication);
        return jobService.getJobResponseForUser(id, user);
    }

    @PostMapping("/{id}/match")
    public MatchScoreResponse getMatchScore(
            Authentication authentication,
            @PathVariable Long id) {
        User user = getAuthenticatedUser(authentication);
        return jobService.getMatchScore(id, user);
    }

    @PatchMapping("/{id}/status")
    public JobResponse updateJobStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = getAuthenticatedUser(authentication);
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        JobStatus status = JobStatus.valueOf(statusStr.toUpperCase());
        return jobService.updateJobStatus(id, status, user);
    }

    @PatchMapping("/{id}/notes")
    public JobResponse updateJobNotes(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = getAuthenticatedUser(authentication);
        String notes = body.get("notes");
        return jobService.updateJobNotes(id, notes, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(
            Authentication authentication,
            @PathVariable Long id) {
        User user = getAuthenticatedUser(authentication);
        jobService.deleteJob(id, user);
    }
}
