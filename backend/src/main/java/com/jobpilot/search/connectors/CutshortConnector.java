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
 * Connector for the Cutshort Jobs API (https://cutshort.io/api).
 * Cutshort focuses on tech roles in India — strong signal-to-noise for startup/product companies.
 * API key available at https://cutshort.io/developer
 * Free tier: 100 requests/day.
 */
@Component
public class CutshortConnector implements JobSource {

    private static final Logger log = LoggerFactory.getLogger(CutshortConnector.class);
    private static final String BASE_URL = "https://cutshort.io/api/public/v1";

    private final RestClient restClient;
    private final String apiKey;
    private final boolean enabled;

    public CutshortConnector(@Value("${cutshort.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.enabled = !apiKey.isBlank();
        this.restClient = RestClient.create(BASE_URL);
    }

    @Override
    public String getSourceName() { return "Cutshort"; }

    @Override
    public JobSourceResult search(JobSearchRequest request) {
        if (!enabled) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAVAILABLE,
                    "Cutshort API key not configured");
        }

        try {
            String keywords = buildKeywords(request);
            log.info("Cutshort: searching keywords='{}' location='{}'", keywords, request.getLocation());

            CutshortResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/jobs")
                            .queryParam("search", keywords)
                            .queryParam("locations", request.getLocation() != null ? request.getLocation() : "India")
                            .queryParam("page", request.getPage() + 1)
                            .queryParam("size", 20)
                            .build())
                    .header("Authorization", "Token " + apiKey)
                    .retrieve()
                    .body(CutshortResponse.class);

            if (response == null || response.data == null || response.data.isEmpty()) {
                return new JobSourceResult(getSourceName(), SearchStatus.NO_RESULTS,
                        java.util.Collections.emptyList());
            }

            List<NormalizedJob> jobs = new ArrayList<>();
            for (CutshortJob raw : response.data) {
                jobs.add(normalize(raw));
            }

            log.info("Cutshort: found {} jobs", jobs.size());
            return new JobSourceResult(getSourceName(), SearchStatus.SUCCESS, jobs);

        } catch (HttpClientErrorException.Unauthorized e) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAUTHORIZED,
                    "Cutshort API key invalid");
        } catch (HttpClientErrorException.TooManyRequests e) {
            return new JobSourceResult(getSourceName(), SearchStatus.RATE_LIMITED,
                    "Cutshort rate limit reached");
        } catch (ResourceAccessException e) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAVAILABLE,
                    "Cutshort API unreachable: " + e.getMessage());
        } catch (Exception e) {
            log.error("Cutshort connector error: {}", e.getMessage());
            return new JobSourceResult(getSourceName(), SearchStatus.ERROR,
                    "Cutshort search failed: " + e.getMessage());
        }
    }

    private String buildKeywords(JobSearchRequest request) {
        StringBuilder kw = new StringBuilder();
        if (request.getRole() != null && !request.getRole().isBlank()) kw.append(request.getRole());
        if (request.getSkills() != null) {
            for (String skill : request.getSkills()) {
                if (kw.length() > 0) kw.append(" ");
                kw.append(skill);
            }
        }
        return kw.toString().trim();
    }

    private NormalizedJob normalize(CutshortJob raw) {
        NormalizedJob job = new NormalizedJob();
        job.setSource(getSourceName());
        job.setSourceJobId(raw.id != null ? String.valueOf(raw.id) : null);
        job.setTitle(raw.title);
        job.setCompany(raw.company != null ? raw.company.name : null);
        job.setLocation(raw.locations != null && !raw.locations.isEmpty()
                ? String.join(", ", raw.locations) : "India");
        job.setDescription(raw.description);
        job.setJobUrl("https://cutshort.io/job/" + raw.id);
        if (raw.salaryMin != null && raw.salaryMax != null) {
            job.setSalaryInfo("₹" + raw.salaryMin / 100000 + "–" + raw.salaryMax / 100000 + " LPA");
        }
        if (raw.skills != null) job.setSkills(raw.skills);
        return job;
    }

    // --- Cutshort API response model ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CutshortResponse {
        @JsonProperty("data")
        List<CutshortJob> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CutshortJob {
        @JsonProperty("id") Object id;
        @JsonProperty("title") String title;
        @JsonProperty("description") String description;
        @JsonProperty("locations") List<String> locations;
        @JsonProperty("skills") List<String> skills;
        @JsonProperty("salary_min") Integer salaryMin;
        @JsonProperty("salary_max") Integer salaryMax;
        @JsonProperty("company") CutshortCompany company;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CutshortCompany {
        @JsonProperty("name") String name;
    }
}
