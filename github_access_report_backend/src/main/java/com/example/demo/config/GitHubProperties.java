package com.example.githubaccessreportbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for GitHub API integration.
 * Values are loaded from application.properties / environment variables.
 */
// PUBLIC_INTERFACE
@Component
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    /** GitHub Personal Access Token for authentication */
    private String token;

    /** Default GitHub organization name to scan */
    private String org;

    /**
     * Sanitizes a GitHub organization value that may be a full URL.
     * <p>
     * Handles inputs like:
     * <ul>
     *   <li>{@code https://github.com/myorg}</li>
     *   <li>{@code https://github.com/myorg/}</li>
     *   <li>{@code http://github.com/myorg}</li>
     *   <li>{@code github.com/myorg}</li>
     *   <li>{@code myorg} (already clean, returned as-is)</li>
     * </ul>
     *
     * @param value the raw org value (may be a URL or plain name)
     * @return the extracted organization/user name, or the original value if not a URL
     */
    // PUBLIC_INTERFACE
    /**
     * Sanitizes a GitHub organization value that may be a full URL.
     */
    public static String sanitizeOrgValue(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        // Remove trailing slashes
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // Check if it looks like a GitHub URL (with or without scheme)
        // Patterns: https://github.com/org, http://github.com/org, github.com/org
        if (trimmed.contains("github.com/")) {
            int idx = trimmed.indexOf("github.com/");
            String afterGithub = trimmed.substring(idx + "github.com/".length());
            // Take only the first path segment (the org name)
            int slashIdx = afterGithub.indexOf('/');
            if (slashIdx > 0) {
                afterGithub = afterGithub.substring(0, slashIdx);
            }
            if (!afterGithub.isBlank()) {
                return afterGithub;
            }
        }
        return trimmed;
    }

    /** Base URL for the GitHub REST API */
    private String apiBaseUrl = "https://api.github.com";

    /** Number of items per page for paginated API calls */
    private int pageSize = 100;

    /** Maximum number of concurrent API calls */
    private int maxConcurrency = 10;

    // PUBLIC_INTERFACE
    /** Returns the configured GitHub token. */
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // PUBLIC_INTERFACE
    /** Returns the default GitHub organization (sanitized; URL prefixes are stripped). */
    public String getOrg() {
        return sanitizeOrgValue(org);
    }

    public void setOrg(String org) {
        this.org = org;
    }

    // PUBLIC_INTERFACE
    /** Returns the GitHub API base URL. */
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    // PUBLIC_INTERFACE
    /** Returns the configured page size for paginated requests. */
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    // PUBLIC_INTERFACE
    /** Returns the maximum concurrency level for parallel API calls. */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }
}
