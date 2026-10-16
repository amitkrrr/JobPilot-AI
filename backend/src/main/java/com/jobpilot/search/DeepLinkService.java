package com.jobpilot.search;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Builds "search on platform" deep links from a structured search request.
 * Nothing is scraped or fetched — the user opens the platform's own search page.
 */
@Service
public class DeepLinkService {

    public List<SearchLink> buildLinks(JobSearchRequest request) {
        String keywords = keywords(request);
        if (keywords.isBlank()) {
            return List.of();
        }
        String location = request.getLocation() != null ? request.getLocation().trim() : "";

        return List.of(
                new SearchLink("LinkedIn", linkedIn(keywords, location, request.isRemote())),
                new SearchLink("Naukri", naukri(keywords, location)),
                new SearchLink("Instahyre", instahyre(keywords, location)),
                new SearchLink("Cutshort", cutshort(keywords, location)),
                new SearchLink("Greenhouse", greenhouse(keywords)));
    }

    private String keywords(JobSearchRequest request) {
        if (request.getRole() != null && !request.getRole().isBlank()) {
            return request.getRole().trim();
        }
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            return String.join(" ", request.getSkills());
        }
        return "";
    }

    private String linkedIn(String keywords, String location, boolean remote) {
        StringBuilder sb = new StringBuilder("https://www.linkedin.com/jobs/search/?keywords=")
                .append(enc(keywords));
        if (!location.isEmpty()) {
            sb.append("&location=").append(enc(location));
        }
        if (remote) {
            sb.append("&f_WT=2");
        }
        return sb.toString();
    }

    private String naukri(String keywords, String location) {
        String path = slug(keywords) + "-jobs" + (location.isEmpty() ? "" : "-in-" + slug(location));
        StringBuilder sb = new StringBuilder("https://www.naukri.com/").append(path)
                .append("?k=").append(enc(keywords));
        if (!location.isEmpty()) {
            sb.append("&l=").append(enc(location));
        }
        return sb.toString();
    }

    private String instahyre(String keywords, String location) {
        StringBuilder sb = new StringBuilder("https://www.instahyre.com/search-jobs/?search=true&skills=")
                .append(enc(keywords));
        if (!location.isEmpty()) {
            sb.append("&location=").append(enc(location));
        }
        return sb.toString();
    }

    private String slug(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private String cutshort(String keywords, String location) {
        StringBuilder sb = new StringBuilder("https://cutshort.io/jobs#")
                .append("?q=").append(enc(keywords));
        if (!location.isEmpty()) {
            sb.append("&city=").append(enc(location));
        }
        return sb.toString();
    }

    private String greenhouse(String keywords) {
        // Greenhouse is a recruiting platform used by thousands of companies.
        // Their public job board index: https://boards.greenhouse.io/
        // We redirect to a Google site search as there's no global search endpoint.
        return "https://www.google.com/search?q=" + enc("site:boards.greenhouse.io " + keywords + " jobs");
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
