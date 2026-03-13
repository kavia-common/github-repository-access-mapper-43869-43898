package com.kavia.githubaccess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for GitHub API integration.
 * Values are loaded from environment variables via application.properties.
 */
@Component
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    /** GitHub Personal Access Token for authentication */
    private String token;

    /** GitHub organization name or URL */
    private String org;

    /** Nested API configuration */
    private Api api = new Api();

    // PUBLIC_INTERFACE
    /** Get the GitHub token. */
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // PUBLIC_INTERFACE
    /**
     * Get the GitHub organization identifier.
     * This may be an org name, a user name, or a full GitHub URL.
     */
    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    // PUBLIC_INTERFACE
    /** Get the nested API configuration. */
    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    /**
     * Extracts the owner name from the org field, handling URLs like
     * "https://github.com/someorg" as well as plain names.
     *
     * @return the extracted owner/org name
     */
    // PUBLIC_INTERFACE
    public String resolveOwnerName() {
        if (org == null || org.isBlank()) {
            return "";
        }
        String trimmed = org.trim();
        // Remove trailing slash
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // Handle full GitHub URLs
        if (trimmed.contains("github.com/")) {
            int idx = trimmed.indexOf("github.com/");
            String afterDomain = trimmed.substring(idx + "github.com/".length());
            // Take only the first path segment (the org/user name)
            int slashIdx = afterDomain.indexOf('/');
            if (slashIdx > 0) {
                return afterDomain.substring(0, slashIdx);
            }
            return afterDomain;
        }
        return trimmed;
    }

    /**
     * Nested configuration class for GitHub API settings.
     */
    public static class Api {
        private String baseUrl = "https://api.github.com";
        private int maxConcurrentRequests = 10;
        private int pageSize = 100;
        private int timeoutMs = 30000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getMaxConcurrentRequests() {
            return maxConcurrentRequests;
        }

        public void setMaxConcurrentRequests(int maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
