package com.kavia.githubaccess.service;

import com.kavia.githubaccess.config.GitHubProperties;
import com.kavia.githubaccess.exception.GitHubApiException;
import com.kavia.githubaccess.model.CollaboratorInfo;
import com.kavia.githubaccess.model.RepositoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Low-level client for interacting with the GitHub REST API.
 * Handles pagination, error mapping, and rate limit awareness.
 */
@Service
public class GitHubApiClient {

    private static final Logger logger = LoggerFactory.getLogger(GitHubApiClient.class);
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final WebClient webClient;
    private final GitHubProperties properties;

    // PUBLIC_INTERFACE
    /**
     * Constructs the GitHubApiClient.
     *
     * @param webClient  pre-configured WebClient for GitHub API
     * @param properties GitHub configuration properties
     */
    public GitHubApiClient(WebClient webClient, GitHubProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    // PUBLIC_INTERFACE
    /**
     * Fetches all repositories for the given owner (org or user).
     * Automatically paginates through all pages.
     * First tries the org endpoint; falls back to user endpoint on 404.
     *
     * @param owner the organization or user name
     * @return list of all repositories
     */
    public List<RepositoryInfo> fetchAllRepositories(String owner) {
        logger.info("Fetching repositories for owner: {}", owner);
        try {
            // Try organization repos first
            return fetchAllPages(
                    "/orgs/" + owner + "/repos?per_page=" + properties.getApi().getPageSize() + "&type=all",
                    new ParameterizedTypeReference<List<RepositoryInfo>>() {}
            );
        } catch (GitHubApiException e) {
            if (e.getStatusCode() == 404) {
                logger.info("Owner '{}' is not an organization, trying user endpoint", owner);
                // Fall back to user repos
                return fetchAllPages(
                        "/users/" + owner + "/repos?per_page=" + properties.getApi().getPageSize() + "&type=all",
                        new ParameterizedTypeReference<List<RepositoryInfo>>() {}
                );
            }
            throw e;
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Fetches all collaborators for a given repository.
     * Uses the collaborators endpoint for org repos, falls back to
     * listing the owner as the sole collaborator on error.
     *
     * @param owner    the organization or user name
     * @param repoName the repository name
     * @return list of collaborators with their permissions
     */
    public List<CollaboratorInfo> fetchCollaborators(String owner, String repoName) {
        logger.debug("Fetching collaborators for {}/{}", owner, repoName);
        try {
            return fetchAllPages(
                    "/repos/" + owner + "/" + repoName + "/collaborators?per_page="
                            + properties.getApi().getPageSize() + "&affiliation=all",
                    new ParameterizedTypeReference<List<CollaboratorInfo>>() {}
            );
        } catch (GitHubApiException e) {
            if (e.getStatusCode() == 403 || e.getStatusCode() == 404) {
                // For repos where we can't list collaborators (e.g. not enough permissions),
                // return an empty list rather than failing the entire report
                logger.warn("Cannot fetch collaborators for {}/{}: {} - skipping",
                        owner, repoName, e.getMessage());
                return List.of();
            }
            throw e;
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Verifies the current GitHub token is valid by calling /user.
     *
     * @return true if authentication is valid
     */
    public boolean verifyAuthentication() {
        try {
            webClient.get()
                    .uri("/user")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new GitHubApiException(
                                                    "Authentication failed: " + body,
                                                    response.statusCode().value()))))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofMillis(properties.getApi().getTimeoutMs()))
                    .block();
            return true;
        } catch (Exception e) {
            logger.error("Authentication verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generic paginated fetcher that follows GitHub Link headers.
     *
     * @param initialUri the first page URI
     * @param typeRef    type reference for deserialization
     * @param <T>        element type
     * @return combined list from all pages
     */
    private <T> List<T> fetchAllPages(String initialUri,
                                       ParameterizedTypeReference<List<T>> typeRef) {
        return Flux.<List<T>, String>generate(
                        () -> initialUri,
                        (uri, sink) -> {
                            if (uri == null) {
                                sink.complete();
                                return null;
                            }

                            try {
                                // Make the API call and capture the response with headers
                                var responseSpec = webClient.get()
                                        .uri(uri)
                                        .retrieve()
                                        .onStatus(HttpStatusCode::isError, response ->
                                                response.bodyToMono(String.class)
                                                        .flatMap(body -> {
                                                            int code = response.statusCode().value();
                                                            String msg = mapErrorMessage(code, body);
                                                            return Mono.error(new GitHubApiException(msg, code));
                                                        }));

                                var responseMono = responseSpec.toEntityList(typeRef)
                                        .timeout(Duration.ofMillis(properties.getApi().getTimeoutMs()));

                                var response = responseMono.block();

                                if (response == null || response.getBody() == null) {
                                    sink.complete();
                                    return null;
                                }

                                sink.next(response.getBody());

                                // Parse Link header for next page
                                String linkHeader = response.getHeaders().getFirst("Link");
                                return extractNextLink(linkHeader);

                            } catch (GitHubApiException e) {
                                sink.error(e);
                                return null;
                            } catch (WebClientResponseException e) {
                                sink.error(new GitHubApiException(
                                        mapErrorMessage(e.getStatusCode().value(), e.getResponseBodyAsString()),
                                        e.getStatusCode().value(), e));
                                return null;
                            } catch (Exception e) {
                                sink.error(new GitHubApiException(
                                        "GitHub API call failed: " + e.getMessage(), 500, e));
                                return null;
                            }
                        })
                .flatMap(Flux::fromIterable)
                .collectList()
                .block();
    }

    /**
     * Extracts the "next" link from a GitHub Link header.
     *
     * @param linkHeader the Link header value
     * @return the next page URL, or null if there is no next page
     */
    private String extractNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        if (matcher.find()) {
            String nextUrl = matcher.group(1);
            // If the URL is absolute, strip the base URL to make it relative
            String baseUrl = properties.getApi().getBaseUrl();
            if (nextUrl.startsWith(baseUrl)) {
                return nextUrl.substring(baseUrl.length());
            }
            return nextUrl;
        }
        return null;
    }

    /**
     * Maps GitHub HTTP error codes to human-readable messages.
     */
    private String mapErrorMessage(int statusCode, String body) {
        switch (statusCode) {
            case 401:
                return "GitHub authentication failed. Please check your GITHUB_TOKEN. Details: " + body;
            case 403:
                return "GitHub API access forbidden. This may be a rate limit or insufficient token permissions. Details: " + body;
            case 404:
                return "GitHub resource not found. Please verify the organization/user name. Details: " + body;
            case 429:
                return "GitHub API rate limit exceeded. Please wait and try again. Details: " + body;
            default:
                return "GitHub API error (HTTP " + statusCode + "): " + body;
        }
    }
}
