package com.jobpilot.search.connectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 * Connector for the Adzuna Jobs API (https://developer.adzuna.com).
 * Free tier: 250 requests/month. Sign up at developer.adzuna.com.
 *
 * Supports India jobs (country code: in) — covers Naukri/LinkedIn-indexed roles.
 */
@Component
public class AdzunaConnector implements JobSource {

    private static final Logger log = LoggerFactory.getLogger(AdzunaConnector.class);

    private static final String BASE_URL = "https://api.adzuna.com/v1/api/jobs";
    private static final String COUNTRY = "in"; // India

    private final RestClient restClient;
    private final String appId;
    private final String appKey;
    private final boolean enabled;

    public AdzunaConnector(
            @Value("${adzuna.app-id:}") String appId,
            @Value("${adzuna.app-key:}") String appKey) {
        this.appId = appId;
        this.appKey = appKey;
        this.enabled = !appId.isBlank() && !appKey.isBlank();
        this.restClient = RestClient.create(BASE_URL);
    }

    @Override
    public String getSourceName() {
        return "Adzuna";
    }

    @Override
    public JobSourceResult search(JobSearchRequest request) {
        if (!enabled) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAVAILABLE,
                    "Adzuna API credentials not configured");
        }

        try {
            String query = buildQuery(request);
            // Strip country suffix (e.g. "Allahabad, India" → "Allahabad") — Adzuna where param is city only
            String location = cleanLocation(request.getLocation());

            log.info("Adzuna: searching query='{}' location='{}'", query, location);

            AdzunaResponse response = fetchAdzuna(query, location, request.getPage());

            // If specific city returned nothing, retry with no location filter (India-wide)
            if ((response == null || response.results == null || response.results.isEmpty()) && !location.isBlank()) {
                log.debug("Adzuna: no results for location '{}', retrying India-wide...", location);
                response = fetchAdzuna(query, "", request.getPage());
            }

            if (response == null || response.results == null || response.results.isEmpty()) {
                log.debug("Adzuna: no results found for query='{}'", query);
                return new JobSourceResult(getSourceName(), SearchStatus.NO_RESULTS,
                        java.util.Collections.emptyList());
            }

            List<NormalizedJob> jobs = new ArrayList<>();
            for (AdzunaJob raw : response.results) {
                jobs.add(normalize(raw));
            }

            log.info("Adzuna: found {} jobs", jobs.size());
            return new JobSourceResult(getSourceName(), SearchStatus.SUCCESS, jobs);

        } catch (HttpClientErrorException.Unauthorized e) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAUTHORIZED,
                    "Adzuna credentials invalid or expired");
        } catch (HttpClientErrorException.TooManyRequests e) {
            return new JobSourceResult(getSourceName(), SearchStatus.RATE_LIMITED,
                    "Adzuna rate limit reached");
        } catch (ResourceAccessException e) {
            return new JobSourceResult(getSourceName(), SearchStatus.UNAVAILABLE,
                    "Adzuna API unreachable: " + e.getMessage());
        } catch (Exception e) {
            log.error("Adzuna connector error: {}", e.getMessage());
            return new JobSourceResult(getSourceName(), SearchStatus.ERROR,
                    "Adzuna search failed: " + e.getMessage());
        }
    }

    private AdzunaResponse fetchAdzuna(String query, String location, int page) {
        int pageNumber = page + 1;
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{country}/search/" + pageNumber)
                        .queryParam("app_id", appId)
                        .queryParam("app_key", appKey)
                        .queryParam("what", query)
                        .queryParam("where", location)
                        .queryParam("results_per_page", 20)
                        .queryParam("content-type", "application/json")
                        .build(COUNTRY))
                .retrieve()
                .body(AdzunaResponse.class);
    }

    /** Strip country name suffix: "Allahabad, India" → "Allahabad" */
    private String cleanLocation(String location) {
        if (location == null) return "";
        // Remove everything after the last comma (country/state suffix)
        int comma = location.lastIndexOf(',');
        if (comma > 0) {
            return location.substring(0, comma).trim();
        }
        return location.trim();
    }

    private String buildQuery(JobSearchRequest request) {
        StringBuilder q = new StringBuilder();
        if (request.getRole() != null && !request.getRole().isBlank()) {
            q.append(request.getRole());
        }
        if (request.getSkills() != null) {
            for (String skill : request.getSkills()) {
                if (q.length() > 0) q.append(" ");
                q.append(skill);
            }
        }
        return q.toString().trim();
    }

    private NormalizedJob normalize(AdzunaJob raw) {
        NormalizedJob job = new NormalizedJob();
        job.setSource(getSourceName());
        job.setSourceJobId(raw.id);
        job.setTitle(raw.title);
        job.setCompany(raw.company != null ? raw.company.displayName : null);
        job.setLocation(raw.location != null ? raw.location.displayName : null);
        job.setDescription(raw.description);
        job.setJobUrl(raw.redirectUrl);
        job.setSalaryInfo(formatSalary(raw.salaryMin, raw.salaryMax));
        return job;
    }

    private String formatSalary(Double min, Double max) {
        if (min == null && max == null) return null;
        if (min != null && max != null) return "₹" + Math.round(min / 100000) + "–" + Math.round(max / 100000) + " LPA";
        if (min != null) return "₹" + Math.round(min / 100000) + "+ LPA";
        return null;
    }

    // --- Adzuna API response model ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AdzunaResponse {
        @JsonProperty("results")
        List<AdzunaJob> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AdzunaJob {
        @JsonProperty("id")
        String id;
        @JsonProperty("title")
        String title;
        @JsonProperty("description")
        String description;
        @JsonProperty("redirect_url")
        String redirectUrl;
        @JsonProperty("salary_min")
        Double salaryMin;
        @JsonProperty("salary_max")
        Double salaryMax;
        @JsonProperty("company")
        AdzunaCompany company;
        @JsonProperty("location")
        AdzunaLocation location;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AdzunaCompany {
        @JsonProperty("display_name")
        String displayName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AdzunaLocation {
        @JsonProperty("display_name")
        String displayName;
    }
}
