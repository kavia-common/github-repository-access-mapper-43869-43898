package com.kavia.githubaccess.service;

import com.kavia.githubaccess.config.GitHubProperties;
import com.kavia.githubaccess.exception.GitHubApiException;
import com.kavia.githubaccess.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service responsible for generating the GitHub access report.
 * Orchestrates fetching repositories and collaborators with concurrent
 * API calls for scalability with 100+ repos and 1000+ users.
 */
@Service
public class AccessReportService {

    private static final Logger logger = LoggerFactory.getLogger(AccessReportService.class);

    private final GitHubApiClient gitHubApiClient;
    private final GitHubProperties properties;

    // PUBLIC_INTERFACE
    /**
     * Constructs the AccessReportService.
     *
     * @param gitHubApiClient client for GitHub API calls
     * @param properties      GitHub configuration properties
     */
    public AccessReportService(GitHubApiClient gitHubApiClient, GitHubProperties properties) {
        this.gitHubApiClient = gitHubApiClient;
        this.properties = properties;
    }

    // PUBLIC_INTERFACE
    /**
     * Generates the complete access report for the configured organization/user.
     * Fetches all repositories, then concurrently fetches collaborators for each,
     * and aggregates into a user-centric view.
     *
     * @return the complete AccessReport
     * @throws GitHubApiException       if GitHub API calls fail
     * @throws IllegalArgumentException if configuration is invalid
     */
    public AccessReport generateReport() {
        String owner = properties.resolveOwnerName();
        if (owner.isBlank()) {
            throw new IllegalArgumentException(
                    "GITHUB_ORG environment variable is not set or is empty. "
                            + "Please set it to a GitHub organization or user name (or URL).");
        }

        // Validate token
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw new IllegalArgumentException(
                    "GITHUB_TOKEN environment variable is not set. "
                            + "Please provide a valid GitHub Personal Access Token.");
        }

        logger.info("Generating access report for owner: {}", owner);

        // Step 1: Fetch all repositories
        List<RepositoryInfo> repositories = gitHubApiClient.fetchAllRepositories(owner);
        logger.info("Found {} repositories for {}", repositories.size(), owner);

        // Step 2: Concurrently fetch collaborators for each repository
        Map<String, List<CollaboratorWithRepo>> repoCollaborators = fetchCollaboratorsConcurrently(
                owner, repositories);

        // Step 3: Aggregate into user-centric view
        return aggregateReport(owner, repositories, repoCollaborators);
    }

    // PUBLIC_INTERFACE
    /**
     * Generates the access report for a specific organization/user name,
     * overriding the configured default.
     *
     * @param owner the organization or user name to generate the report for
     * @return the complete AccessReport
     */
    public AccessReport generateReportForOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner name must not be empty.");
        }

        // Validate token
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw new IllegalArgumentException(
                    "GITHUB_TOKEN environment variable is not set. "
                            + "Please provide a valid GitHub Personal Access Token.");
        }

        logger.info("Generating access report for owner: {}", owner);

        List<RepositoryInfo> repositories = gitHubApiClient.fetchAllRepositories(owner);
        logger.info("Found {} repositories for {}", repositories.size(), owner);

        Map<String, List<CollaboratorWithRepo>> repoCollaborators = fetchCollaboratorsConcurrently(
                owner, repositories);

        return aggregateReport(owner, repositories, repoCollaborators);
    }

    /**
     * Fetches collaborators for all repositories concurrently using a thread pool.
     * Limits concurrency to avoid hitting GitHub rate limits.
     */
    private Map<String, List<CollaboratorWithRepo>> fetchCollaboratorsConcurrently(
            String owner, List<RepositoryInfo> repositories) {

        int maxConcurrent = properties.getApi().getMaxConcurrentRequests();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(maxConcurrent, repositories.size()));

        Map<String, List<CollaboratorWithRepo>> result = new ConcurrentHashMap<>();

        try {
            // Submit concurrent tasks for fetching collaborators
            List<CompletableFuture<Void>> futures = repositories.stream()
                    .map(repo -> CompletableFuture.runAsync(() -> {
                        try {
                            List<CollaboratorInfo> collaborators =
                                    gitHubApiClient.fetchCollaborators(owner, repo.getName());

                            List<CollaboratorWithRepo> entries = collaborators.stream()
                                    .map(collab -> new CollaboratorWithRepo(collab, repo))
                                    .collect(Collectors.toList());

                            result.put(repo.getName(), entries);

                            logger.debug("Fetched {} collaborators for {}",
                                    collaborators.size(), repo.getName());
                        } catch (Exception e) {
                            logger.warn("Failed to fetch collaborators for {}/{}: {}",
                                    owner, repo.getName(), e.getMessage());
                            result.put(repo.getName(), List.of());
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // Wait for all tasks to complete with timeout
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(properties.getApi().getTimeoutMs() * 2L, TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Collaborator fetch was interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            logger.error("Error during concurrent collaborator fetch: {}", e.getMessage());
        } finally {
            executor.shutdown();
        }

        return result;
    }

    /**
     * Aggregates repository-level collaborator data into a user-centric report.
     * Maps each user to the list of repositories they have access to.
     */
    private AccessReport aggregateReport(String owner, List<RepositoryInfo> repositories,
                                          Map<String, List<CollaboratorWithRepo>> repoCollaborators) {

        // Build a map: username -> list of RepositoryAccess
        Map<String, UserBuilder> userMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<CollaboratorWithRepo>> entry : repoCollaborators.entrySet()) {
            for (CollaboratorWithRepo cwi : entry.getValue()) {
                CollaboratorInfo collab = cwi.collaborator;
                RepositoryInfo repo = cwi.repository;

                String username = collab.getLogin();
                UserBuilder builder = userMap.computeIfAbsent(username, k ->
                        new UserBuilder(username, collab.getAvatarUrl()));

                builder.addRepo(new RepositoryAccess(
                        repo.getName(),
                        repo.getFullName(),
                        collab.resolveRole(),
                        repo.isPrivate()
                ));
            }
        }

        // Convert to sorted list of UserAccess
        List<UserAccess> users = userMap.values().stream()
                .map(UserBuilder::build)
                .sorted(Comparator.comparing(UserAccess::getUsername, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        logger.info("Report generated: {} repos, {} users", repositories.size(), users.size());

        return new AccessReport(owner, repositories.size(), users.size(), users);
    }

    /**
     * Helper record to pair a collaborator with its repository.
     */
    private static class CollaboratorWithRepo {
        final CollaboratorInfo collaborator;
        final RepositoryInfo repository;

        CollaboratorWithRepo(CollaboratorInfo collaborator, RepositoryInfo repository) {
            this.collaborator = collaborator;
            this.repository = repository;
        }
    }

    /**
     * Helper class to build UserAccess objects incrementally.
     */
    private static class UserBuilder {
        private final String username;
        private final String avatarUrl;
        private final List<RepositoryAccess> repos = new ArrayList<>();

        UserBuilder(String username, String avatarUrl) {
            this.username = username;
            this.avatarUrl = avatarUrl;
        }

        void addRepo(RepositoryAccess access) {
            repos.add(access);
        }

        UserAccess build() {
            // Sort repos alphabetically
            repos.sort(Comparator.comparing(RepositoryAccess::getRepository,
                    String.CASE_INSENSITIVE_ORDER));
            return new UserAccess(username, avatarUrl, repos);
        }
    }
}
