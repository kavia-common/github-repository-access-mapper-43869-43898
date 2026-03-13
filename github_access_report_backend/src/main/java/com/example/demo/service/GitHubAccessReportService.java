package com.example.githubaccessreportbackend.service;

import com.example.githubaccessreportbackend.client.GitHubApiClient;
import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.dto.AccessReport;
import com.example.githubaccessreportbackend.dto.UserAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service responsible for aggregating GitHub repository access data.
 * For each repository in an organization, fetches collaborators concurrently
 * and builds a user-centric access report.
 */
// PUBLIC_INTERFACE
@Service
public class GitHubAccessReportService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubAccessReportService.class);

    private final GitHubApiClient gitHubApiClient;
    private final GitHubProperties gitHubProperties;

    public GitHubAccessReportService(GitHubApiClient gitHubApiClient, GitHubProperties gitHubProperties) {
        this.gitHubApiClient = gitHubApiClient;
        this.gitHubProperties = gitHubProperties;
    }

    // PUBLIC_INTERFACE
    /**
     * Generates the full access report for a given GitHub organization.
     * <p>
     * Steps:
     * 1. Fetch all repositories for the organization.
     * 2. For each repository, concurrently fetch collaborators and their permissions.
     * 3. Aggregate into a user-centric view mapping each user to the repos they can access.
     *
     * @param org the GitHub organization name
     * @return AccessReport containing the aggregated user-repository access data
     */
    public AccessReport generateReport(String org) {
        logger.info("Starting access report generation for organization: {}", org);
        long startTime = System.currentTimeMillis();

        // Step 1: Fetch all repositories
        List<Map<String, Object>> repos = gitHubApiClient.fetchOrganizationRepos(org);
        logger.info("Found {} repositories in organization '{}'", repos.size(), org);

        // Step 2: Concurrently fetch collaborators for each repository
        // Use a thread-safe map to collect user -> repo permissions
        ConcurrentHashMap<String, List<RepoPermissionEntry>> userRepoMap = new ConcurrentHashMap<>();

        // Create a bounded executor to control concurrency
        int poolSize = repos.isEmpty() ? 1 : Math.min(gitHubProperties.getMaxConcurrency(), repos.size());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<Void>> futures = repos.stream()
                    .map(repo -> CompletableFuture.runAsync(
                            () -> processRepository(repo, userRepoMap), executor))
                    .collect(Collectors.toList());

            // Wait for all tasks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Step 3: Build the response DTO
        AccessReport report = buildReport(org, repos.size(), userRepoMap);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Report generation completed in {}ms - {} repos, {} users",
                elapsed, report.getTotalRepositories(), report.getTotalUsers());

        return report;
    }

    /**
     * Processes a single repository by fetching its collaborators and adding
     * them to the shared user-repo map.
     *
     * @param repo       the repository JSON map from the GitHub API
     * @param userRepoMap thread-safe map accumulating user permissions
     */
    private void processRepository(Map<String, Object> repo,
                                   ConcurrentHashMap<String, List<RepoPermissionEntry>> userRepoMap) {
        String repoFullName = (String) repo.get("full_name");
        if (repoFullName == null) {
            logger.warn("Skipping repository with missing full_name");
            return;
        }

        try {
            List<Map<String, Object>> collaborators = gitHubApiClient.fetchRepoCollaborators(repoFullName);
            logger.debug("Repository '{}' has {} collaborators", repoFullName, collaborators.size());

            for (Map<String, Object> collaborator : collaborators) {
                String username = (String) collaborator.get("login");
                if (username == null) {
                    continue;
                }

                String role = extractHighestRole(collaborator);
                RepoPermissionEntry entry = new RepoPermissionEntry(repoFullName, role);

                userRepoMap.computeIfAbsent(username, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(entry);
            }
        } catch (Exception e) {
            // Log and continue - don't let one repo failure crash the entire report
            logger.error("Failed to fetch collaborators for repository '{}': {}",
                    repoFullName, e.getMessage());
        }
    }

    /**
     * Extracts the highest permission role from the collaborator's permissions map.
     * The GitHub API returns a permissions object like:
     * {"admin": true, "maintain": false, "push": true, "triage": false, "pull": true}
     * We return the highest privilege level that is true.
     *
     * @param collaborator the collaborator JSON map
     * @return the highest permission role as a string
     */
    @SuppressWarnings("unchecked")
    private String extractHighestRole(Map<String, Object> collaborator) {
        // Check if "role_name" field exists (newer API responses)
        Object roleName = collaborator.get("role_name");
        if (roleName instanceof String && !((String) roleName).isBlank()) {
            return (String) roleName;
        }

        // Fall back to parsing the permissions map
        Object permissionsObj = collaborator.get("permissions");
        if (permissionsObj instanceof Map) {
            Map<String, Boolean> permissions = (Map<String, Boolean>) permissionsObj;
            // Check from highest to lowest privilege
            if (Boolean.TRUE.equals(permissions.get("admin"))) {
                return "admin";
            }
            if (Boolean.TRUE.equals(permissions.get("maintain"))) {
                return "maintain";
            }
            if (Boolean.TRUE.equals(permissions.get("push"))) {
                return "write";
            }
            if (Boolean.TRUE.equals(permissions.get("triage"))) {
                return "triage";
            }
            if (Boolean.TRUE.equals(permissions.get("pull"))) {
                return "read";
            }
        }

        return "unknown";
    }

    /**
     * Builds the final AccessReport DTO from the aggregated data.
     *
     * @param org           the organization name
     * @param totalRepos    total number of repositories scanned
     * @param userRepoMap   aggregated user -> repo permission entries
     * @return the constructed AccessReport
     */
    private AccessReport buildReport(String org, int totalRepos,
                                     ConcurrentHashMap<String, List<RepoPermissionEntry>> userRepoMap) {
        List<UserAccess> userAccessList = userRepoMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Sort users alphabetically
                .map(entry -> {
                    UserAccess ua = new UserAccess(entry.getKey());
                    entry.getValue().stream()
                            .sorted(Comparator.comparing(RepoPermissionEntry::repoFullName))
                            .forEach(rpe -> ua.addRepository(rpe.repoFullName(), rpe.role()));
                    return ua;
                })
                .collect(Collectors.toList());

        AccessReport report = new AccessReport();
        report.setOrganization(org);
        report.setGeneratedAt(Instant.now().toString());
        report.setTotalRepositories(totalRepos);
        report.setTotalUsers(userAccessList.size());
        report.setUsers(userAccessList);

        return report;
    }

    /**
     * Internal record to hold repository permission data while aggregating.
     */
    private record RepoPermissionEntry(String repoFullName, String role) {
    }
}
