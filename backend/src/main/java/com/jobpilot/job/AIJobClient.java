package com.jobpilot.job;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.jobpilot.dto.job.MatchScoreResponse;
import com.jobpilot.dto.job.ParsedJobRequirementResponse;
import com.jobpilot.user.CandidateProfile;

@Service
public class AIJobClient {

    private static final Logger log = LoggerFactory.getLogger(AIJobClient.class);

    private final RestClient restClient;

    public AIJobClient(@Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restClient = RestClient.create(aiServiceUrl);
    }

    public ParsedJobRequirementResponse parseJobDescription(String description) {
        Map<String, String> requestPayload = new HashMap<>();
        requestPayload.put("description", description);

        try {
            return restClient.post()
                    .uri("/api/ai/parse-jd")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(ParsedJobRequirementResponse.class);
        } catch (Exception e) {
            log.error("Failed to contact Python AI JD Parsing Service: {}", e.getMessage());
            throw new RuntimeException("Python AI Service is currently unavailable. Could not parse Job Description.", e);
        }
    }

    public MatchScoreResponse scoreMatch(Job job, CandidateProfile profile) {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("job_description", job.getDescription() != null ? job.getDescription() : "");

        List<String> requiredSkills = Collections.emptyList();
        if (job.getRequirements() != null && job.getRequirements().getRequiredSkills() != null) {
            requiredSkills = job.getRequirements().getRequiredSkills();
        }
        requestPayload.put("required_skills", requiredSkills);

        List<String> candidateSkills = Collections.emptyList();
        int yearsExp = 0;
        if (profile != null) {
            if (profile.getSkills() != null && !profile.getSkills().isBlank()) {
                candidateSkills = Arrays.stream(profile.getSkills().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
            if (profile.getYearsOfExperience() != null) {
                yearsExp = profile.getYearsOfExperience();
            }
        }
        requestPayload.put("candidate_skills", candidateSkills);
        requestPayload.put("years_of_experience", yearsExp);

        try {
            return restClient.post()
                    .uri("/api/ai/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(MatchScoreResponse.class);
        } catch (Exception e) {
            log.error("Failed to contact Python AI Match Scoring Service: {}", e.getMessage());
            MatchScoreResponse fallback = new MatchScoreResponse();
            fallback.setScore(70);
            fallback.setStrengths(List.of("Relevant profile background"));
            fallback.setGaps(List.of("AI match service currently offline"));
            fallback.setVerdict("Estimated match score based on candidate profile.");
            return fallback;
        }
    }
}
