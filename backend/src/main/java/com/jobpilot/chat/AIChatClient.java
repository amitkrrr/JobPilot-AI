package com.jobpilot.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobpilot.dto.profile.ProfileResponse;
import com.jobpilot.search.JobSearchRequest;

/**
 * Calls the Python FastAPI AI service for a single focused job:
 * classify the user's natural language message into a structured SearchIntent.
 *
 * Spring Boot owns job fetching — Gemini owns language understanding.
 */
@Service
public class AIChatClient {

    private static final Logger log = LoggerFactory.getLogger(AIChatClient.class);

    private final RestClient restClient;

    public AIChatClient(@Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restClient = RestClient.create(aiServiceUrl);
    }

    /**
     * Ask Gemini (via Python) to classify the message into a SearchIntent.
     * Returns null if the AI service is unreachable — caller handles fallback.
     */
    public SearchIntent classifyIntent(String message, ProfileResponse profile) {
        return classifyIntent(message, profile, null);
    }

    public SearchIntent classifyIntent(String message, ProfileResponse profile, SearchIntent lastIntent) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("profile", profile);
        if (lastIntent != null) {
            payload.put("last_intent", lastIntent);
        }

        try {
            return restClient.post()
                    .uri("/api/ai/intent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(SearchIntent.class);
        } catch (Exception e) {
            log.error("AI intent service unreachable: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Structured intent returned by the Python /api/ai/intent endpoint.
     * Maps directly to the SearchIntent Pydantic schema.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchIntent {

        private String intent;      // "JOB_SEARCH" or "GENERAL_CHAT"
        private String role;
        private List<String> skills;
        private String location;

        @JsonProperty("experience_min")
        private Integer experienceMin;

        @JsonProperty("experience_max")
        private Integer experienceMax;

        private boolean remote;
        private String reply;       // populated only for GENERAL_CHAT

        public boolean isJobSearch() {
            return "JOB_SEARCH".equals(intent);
        }

        /** Convert this intent into a JobSearchRequest for JobSearchService. */
        public JobSearchRequest toSearchRequest() {
            JobSearchRequest req = new JobSearchRequest();
            req.setRole(role);
            req.setSkills(skills);
            req.setLocation(location);
            req.setExperienceMin(experienceMin);
            req.setExperienceMax(experienceMax);
            req.setRemote(remote);
            return req;
        }

        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public Integer getExperienceMin() { return experienceMin; }
        public void setExperienceMin(Integer experienceMin) { this.experienceMin = experienceMin; }

        public Integer getExperienceMax() { return experienceMax; }
        public void setExperienceMax(Integer experienceMax) { this.experienceMax = experienceMax; }

        public boolean isRemote() { return remote; }
        public void setRemote(boolean remote) { this.remote = remote; }

        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
    }
}
