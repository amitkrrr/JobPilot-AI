package com.jobpilot.search.connectors;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobpilot.search.JobSource;
import com.jobpilot.search.JobSearchRequest;
import com.jobpilot.search.JobSourceResult;
import com.jobpilot.search.NormalizedJob;
import com.jobpilot.search.SearchStatus;

/**
 * Connector for the Jooble Jobs API (https://jooble.org/api/about).
 * Free tier available — register at jooble.org/api/about to get a key.
 * Aggregates jobs from hundreds of Indian job boards including Naukri, Shine, etc.
 */
@Component
public class JoobleConnector implements JobSource {

    private static final Logger log = LoggerFactory.getLogger(JoobleConnector.class);

    private static final String BASE_URL = "https://jooble.org/api/";

    private final RestClient restClient;
    private final String apiKey;
    private final boolean enabled;

    public JoobleConnector(@Value("${jooble.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.enabled = !apiKey.isBlank();
        this.restClient = RestClient.create(BASE_URL);
    }

    @Override
    public String getSourceName() {
        return "Jooble";
    }

    @Override
    public JobSourceResult search(JobSearchRequest request) {
        if (!enabled) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAVAILABLE,
                    "Jooble API key not configured");
        }

        try {
            String keywords = buildKeywords(request);
            String location = request.getLocation() != null ? request.getLocation() : "India";

            JoobleRequest body = new JoobleRequest(keywords, location);

            JoobleResponse response = restClient.post()
                    .uri("/" + apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JoobleResponse.class);

            if (response == null || response.jobs == null || response.jobs.isEmpty()) {
                return new JobSourceResult(getSourceName(), SearchStatus.NO_RESULTS,
                        java.util.Collections.emptyList());
            }

            List<NormalizedJob> jobs = new ArrayList<>();
            for (JoobleJob raw : response.jobs) {
                jobs.add(normalize(raw));
            }

            log.info("Jooble: found {} jobs for keywords: {}", jobs.size(), keywords);
            return new JobSourceResult(getSourceName(), SearchStatus.SUCCESS, jobs);

        } catch (HttpClientErrorException.Unauthorized e) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAUTHORIZED,
                    "Jooble API key invalid");
        } catch (HttpClientErrorException.TooManyRequests e) {
            return new JobSourceResult(getSourceName(), SearchStatus.RATE_LIMITED,
                    "Jooble rate limit reached");
        } catch (ResourceAccessException e) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAVAILABLE,
                    "Jooble API unreachable: " + e.getMessage());
        } catch (Exception e) {
            log.error("Jooble connector error: {}", e.getMessage());
            return new JobSourceResult(getSourceName(), SearchStatus.ERROR,
                    "Jooble search failed: " + e.getMessage());
        }
    }

    private String buildKeywords(JobSearchRequest request) {
        StringBuilder kw = new StringBuilder();
        if (request.getRole() != null && !request.getRole().isBlank()) {
            kw.append(request.getRole());
        }
        if (request.getSkills() != null) {
            for (String skill : request.getSkills()) {
                if (kw.length() > 0) kw.append(" ");
                kw.append(skill);
            }
        }
        return kw.toString().trim();
    }

    private NormalizedJob normalize(JoobleJob raw) {
        NormalizedJob job = new NormalizedJob();
        job.setSource(getSourceName());
        job.setSourceJobId(raw.id);
        job.setTitle(raw.title);
        job.setCompany(raw.company);
        job.setLocation(raw.location);
        job.setDescription(raw.snippet);
        job.setJobUrl(raw.link);
        job.setSalaryInfo(raw.salary);
        job.setEmploymentType(raw.type);
        return job;
    }

    // --- Jooble API request/response model ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class JoobleRequest {
        @JsonProperty("keywords")
        String keywords;
        @JsonProperty("location")
        String location;

        JoobleRequest(String keywords, String location) {
            this.keywords = keywords;
            this.location = location;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class JoobleResponse {
        @JsonProperty("jobs")
        List<JoobleJob> jobs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class JoobleJob {
        @JsonProperty("id")
        String id;
        @JsonProperty("title")
        String title;
        @JsonProperty("location")
        String location;
        @JsonProperty("snippet")
        String snippet;
        @JsonProperty("salary")
        String salary;
        @JsonProperty("type")
        String type;
        @JsonProperty("link")
        String link;
        @JsonProperty("company")
        String company;
    }
}
