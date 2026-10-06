package com.jobpilot.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * Orchestrates all registered JobSource connectors.
 * Runs them in parallel, collects JobSourceResults, deduplicates,
 * ranks by relevance, and returns a ChatSearchResult with a transparent summary.
 */
@Service
public class JobSearchService {

    private final List<JobSource> sources;

    // Spring injects all beans that implement JobSource automatically
    public JobSearchService(List<JobSource> sources) {
        this.sources = sources;
    }

    public ChatSearchResult search(JobSearchRequest request) {
        List<JobSourceResult> results = new ArrayList<>();

        // Query all connectors (sequential for now; upgrade to parallel with CompletableFuture later)
        for (JobSource source : sources) {
            results.add(source.search(request));
        }

        // Collect all jobs from successful sources
        List<NormalizedJob> allJobs = results.stream()
                .filter(r -> r.getStatus() == SearchStatus.SUCCESS)
                .flatMap(r -> r.getJobs().stream())
                .collect(Collectors.toList());

        // Deduplicate by normalized (title + company) key
        List<NormalizedJob> uniqueJobs = deduplicate(allJobs);

        // Rank by relevance to the search request
        List<NormalizedJob> ranked = rank(uniqueJobs, request);

        // Build transparent summary message
        String summary = buildSummary(results, ranked.size());

        return new ChatSearchResult(summary, ranked, results);
    }

    private List<NormalizedJob> deduplicate(List<NormalizedJob> jobs) {
        Map<String, NormalizedJob> seen = new LinkedHashMap<>();
        for (NormalizedJob job : jobs) {
            String key = normalize(job.getTitle()) + "|" + normalize(job.getCompany());
            // First occurrence wins (connectors are ordered by priority)
            seen.putIfAbsent(key, job);
        }
        return new ArrayList<>(seen.values());
    }

    private List<NormalizedJob> rank(List<NormalizedJob> jobs, JobSearchRequest request) {
        Map<NormalizedJob, Integer> scores = new HashMap<>();
        for (NormalizedJob job : jobs) {
            scores.put(job, score(job, request));
        }
        return jobs.stream()
                .sorted((a, b) -> scores.get(b) - scores.get(a))
                .collect(Collectors.toList());
    }

    /**
     * Deterministic relevance scoring:
     * Role match   40 pts
     * Skill match  40 pts (10 per matched skill, up to 4)
     * Location     20 pts
     */
    private int score(NormalizedJob job, JobSearchRequest request) {
        int score = 0;
        String titleLower = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
        String descLower = job.getDescription() != null ? job.getDescription().toLowerCase() : "";
        String locationLower = job.getLocation() != null ? job.getLocation().toLowerCase() : "";

        // Role match
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String roleLower = request.getRole().toLowerCase();
            if (titleLower.contains(roleLower)) score += 40;
            else if (descLower.contains(roleLower)) score += 20;
        }

        // Skills match
        if (request.getSkills() != null) {
            int matchedSkills = 0;
            for (String skill : request.getSkills()) {
                String s = skill.toLowerCase();
                if (titleLower.contains(s) || descLower.contains(s)) {
                    matchedSkills++;
                }
            }
            score += Math.min(matchedSkills, 4) * 10;
        }

        // Location match
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            if (locationLower.contains(request.getLocation().toLowerCase())) score += 20;
        }

        return score;
    }

    private String buildSummary(List<JobSourceResult> results, int totalUnique) {
        if (totalUnique == 0) {
            boolean allUnavailable = results.stream()
                    .allMatch(r -> r.getStatus() == SearchStatus.UNAVAILABLE);
            if (allUnavailable) {
                return "Job connectors are not yet configured. Add your Adzuna and Jooble API keys to start seeing real jobs.";
            }
            return "No jobs found matching your criteria across all sources. Try broader keywords or a different location.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("I found ").append(totalUnique).append(" unique job")
                .append(totalUnique == 1 ? "" : "s").append(" for you");

        // Source breakdown — show count per successful source
        List<String> sourceSummaries = new ArrayList<>();
        for (JobSourceResult r : results) {
            if (r.getStatus() == SearchStatus.SUCCESS) {
                sourceSummaries.add(r.getJobs().size() + " from " + r.getSource());
            }
        }
        if (!sourceSummaries.isEmpty()) {
            sb.append(" (").append(String.join(", ", sourceSummaries)).append(")");
        }

        // Mention any non-successful sources transparently
        for (JobSourceResult r : results) {
            if (r.getStatus() == SearchStatus.UNAVAILABLE || r.getStatus() == SearchStatus.RATE_LIMITED) {
                sb.append(". ").append(r.getSource()).append(" is temporarily unavailable");
            } else if (r.getStatus() == SearchStatus.NO_RESULTS) {
                sb.append(". ").append(r.getSource()).append(" returned no results for this search");
            } else if (r.getStatus() == SearchStatus.ERROR || r.getStatus() == SearchStatus.UNAUTHORIZED) {
                sb.append(". ").append(r.getSource()).append(" encountered an error");
            }
        }

        sb.append(".");
        return sb.toString();
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
