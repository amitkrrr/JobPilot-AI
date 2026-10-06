package com.jobpilot.job;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.jobpilot.dto.job.ParsedJobRequirementResponse;

@Service
public class AIJobClient {

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
            System.err.println("Failed to contact Python AI JD Parsing Service: " + e.getMessage());
            throw new RuntimeException("Python AI Service is currently unavailable. Could not parse Job Description.", e);
        }
    }
}
