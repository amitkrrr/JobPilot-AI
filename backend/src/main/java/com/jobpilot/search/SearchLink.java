package com.jobpilot.search;

/**
 * A ready-made search URL on an external job platform (no data is fetched from the platform).
 */
public record SearchLink(String platform, String url) {
}
