package com.example.githubaccessreportbackend.client;

import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Low-level client for the GitHub REST API v3.
 * Handles pagination via Link headers and rate-limit awareness.
 */
// PUBLIC_INTERFACE
@Component
public class GitHubApiClient {

    private static final Logger logger = LoggerFactory.getLogger(GitHubApiClient.class);

    /** Regex to extract the "next" page URL from the GitHub Link header. */
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final RestTemplate restTemplate;
    private final GitHubProperties properties;

    public GitHubApiClient(RestTemplate gitHubRestTemplate, GitHubProperties properties) {
        this.restTemplate = gitHubRestTemplate;
        this.properties = properties;
    }

    // PUBLIC_INTERFACE
    /**
     * Fetches all repositories for the given organization, handling pagination.
     *
     * @param org the GitHub organization name
     * @return list of repository maps (each map represents a repo JSON object)
     */
    public List<Map<String, Object>> fetchOrganizationRepos(String org) {
        String url = String.format("%s/orgs/%s/repos?per_page=%d&type=all",
                properties.getApiBaseUrl(), org, properties.getPageSize());
        logger.info("Fetching repositories for organization: {}", org);
        return fetchAllPages(url);
    }

    // PUBLIC_INTERFACE
    /**
     * Fetches all collaborators for a given repository, handling pagination.
     * Uses the collaborators endpoint which includes permission details.
     *
     * @param repoFullName full repository name in "org/repo" format
     * @return list of collaborator maps (each map represents a collaborator JSON object)
     */
    public List<Map<String, Object>> fetchRepoCollaborators(String repoFullName) {
        String url = String.format("%s/repos/%s/collaborators?per_page=%d&affiliation=all",
                properties.getApiBaseUrl(), repoFullName, properties.getPageSize());
        logger.debug("Fetching collaborators for repository: {}", repoFullName);
        return fetchAllPages(url);
    }

    /**
     * Follows pagination via the GitHub "Link" header, collecting all items
     * across all pages into a single list.
     *
     * @param initialUrl the first page URL
     * @return aggregated list of items from all pages
     */
    private List<Map<String, Object>> fetchAllPages(String initialUrl) {
        List<Map<String, Object>> allItems = new ArrayList<>();
        String url = initialUrl;

        while (url != null) {
            ResponseEntity<List<Map<String, Object>>> response = executeGetRequest(url);

            List<Map<String, Object>> body = response.getBody();
            if (body != null) {
                allItems.addAll(body);
            }

            // Check for rate limit headers and log if getting close
            checkRateLimitHeaders(response.getHeaders());

            // Extract next page URL from Link header
            url = extractNextPageUrl(response.getHeaders());
        }

        return allItems;
    }

    /**
     * Executes a GET request against the GitHub API with proper error handling.
     *
     * @param url the URL to fetch
     * @return the response entity containing a list of maps
     */
    private ResponseEntity<List<Map<String, Object>>> executeGetRequest(String url) {
        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
        } catch (HttpClientErrorException e) {
            handleHttpClientError(e, url);
            // unreachable, handleHttpClientError always throws
            throw new GitHubApiException("Unexpected error for URL: " + url, e.getStatusCode().value(), e);
        } catch (HttpServerErrorException e) {
            logger.error("GitHub server error for URL {}: {} - {}",
                    url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new GitHubApiException(
                    "GitHub server error: " + e.getStatusCode(),
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            logger.error("Network error communicating with GitHub API: {}", e.getMessage());
            throw new GitHubApiException(
                    "Failed to communicate with GitHub API: " + e.getMessage(), 502, e);
        }
    }

    /**
     * Handles HTTP 4xx errors from the GitHub API with specific messaging.
     */
    private void handleHttpClientError(HttpClientErrorException e, String url) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        if (statusCode == 401) {
            logger.error("Authentication failed. Check your GITHUB_TOKEN.");
            throw new GitHubApiException(
                    "Authentication failed. Ensure GITHUB_TOKEN is set and valid.", statusCode, e);
        } else if (statusCode == 403) {
            // Could be rate limit or insufficient permissions
            if (responseBody.contains("rate limit")) {
                logger.error("GitHub API rate limit exceeded.");
                throw new GitHubApiException(
                        "GitHub API rate limit exceeded. Please wait and try again.", 429, e);
            }
            logger.error("Forbidden: insufficient permissions for URL: {}", url);
            throw new GitHubApiException(
                    "Forbidden: insufficient permissions. Token may lack required scopes (read:org, repo).",
                    statusCode, e);
        } else if (statusCode == 404) {
            logger.error("Resource not found at URL: {}. Verify the organization name "
                    + "(use org name only, not a full URL) and token permissions.", url);
            throw new GitHubApiException(
                    "Resource not found. Verify the organization name (use the org name only, "
                    + "not a full GitHub URL) and that the token has access.",
                    statusCode, e);
        } else {
            logger.error("GitHub API client error for URL {}: {} - {}",
                    url, statusCode, responseBody);
            throw new GitHubApiException(
                    "GitHub API error (HTTP " + statusCode + "): " + responseBody,
                    statusCode, e);
        }
    }

    /**
     * Logs a warning when the remaining rate limit is getting low.
     */
    private void checkRateLimitHeaders(HttpHeaders headers) {
        String remaining = headers.getFirst("X-RateLimit-Remaining");
        String limit = headers.getFirst("X-RateLimit-Limit");
        if (remaining != null && limit != null) {
            int remainingCount = Integer.parseInt(remaining);
            if (remainingCount < 100) {
                String resetEpoch = headers.getFirst("X-RateLimit-Reset");
                logger.warn("GitHub API rate limit running low: {}/{} remaining. Reset at epoch: {}",
                        remaining, limit, resetEpoch);
            }
            if (remainingCount == 0) {
                throw new GitHubApiException(
                        "GitHub API rate limit exhausted. Please wait for the reset window.", 429);
            }
        }
    }

    /**
     * Extracts the URL for the next page from the GitHub "Link" response header.
     *
     * @param headers the HTTP response headers
     * @return the next page URL, or null if there is no next page
     */
    private String extractNextPageUrl(HttpHeaders headers) {
        List<String> linkHeaders = headers.getOrDefault("Link", Collections.emptyList());
        for (String linkHeader : linkHeaders) {
            Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
