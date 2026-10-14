package com.jobpilot.resume;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.jobpilot.dto.resume.ResumeParsedResponse;

@Service
public class AIParsingClient {

    private static final Logger log = LoggerFactory.getLogger(AIParsingClient.class);

    private final RestClient restClient;

    public AIParsingClient(@Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restClient = RestClient.create(aiServiceUrl);
    }

    public ResumeParsedResponse parseResume(String s3Key) {
        Map<String, String> requestPayload = new HashMap<>();
        requestPayload.put("s3_key", s3Key);

        try {
            return restClient.post()
                    .uri("/api/ai/parse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(ResumeParsedResponse.class);
        } catch (Exception e) {
            log.error("Failed to contact Python AI Service: {}", e.getMessage());
            throw new IllegalStateException("AI Service is temporarily unavailable or returned an error: " + e.getMessage());
        }
    }
}
