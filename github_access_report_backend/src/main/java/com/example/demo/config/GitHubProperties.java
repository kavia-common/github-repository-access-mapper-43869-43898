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
    /** Returns the default GitHub organization. */
    public String getOrg() {
        return org;
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
