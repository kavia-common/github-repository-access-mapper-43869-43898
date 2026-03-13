package com.example.githubaccessreportbackend.service;

import com.example.githubaccessreportbackend.client.GitHubApiClient;
import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.dto.AccessReport;
import com.example.githubaccessreportbackend.dto.RepositoryPermission;
import com.example.githubaccessreportbackend.dto.UserAccess;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GitHubAccessReportService}.
 * The GitHubApiClient is mocked to avoid real GitHub API calls.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubAccessReportService Tests")
class test_GitHubAccessReportServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    private GitHubProperties gitHubProperties;

    private GitHubAccessReportService reportService;

    @BeforeEach
    void setUp() {
        gitHubProperties = new GitHubProperties();
        gitHubProperties.setMaxConcurrency(2);
        gitHubProperties.setPageSize(100);
        reportService = new GitHubAccessReportService(gitHubApiClient, gitHubProperties);
    }

    @Nested
    @DisplayName("generateReport - successful scenarios")
    class GenerateReportSuccessTests {

        @Test
        @DisplayName("Should generate a report with users and repositories")
        void shouldGenerateReportSuccessfully() {
            // Arrange
            String org = "testorg";

            List<Map<String, Object>> repos = List.of(
                    Map.of("full_name", "testorg/repo1", "name", "repo1"),
                    Map.of("full_name", "testorg/repo2", "name", "repo2")
            );

            List<Map<String, Object>> repo1Collaborators = List.of(
                    createCollaborator("alice", Map.of("admin", true, "push", true, "pull", true)),
                    createCollaborator("bob", Map.of("admin", false, "push", true, "pull", true))
            );

            List<Map<String, Object>> repo2Collaborators = List.of(
                    createCollaborator("alice", Map.of("admin", false, "push", false, "pull", true)),
                    createCollaborator("charlie", Map.of("admin", true, "push", true, "pull", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos(org)).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("testorg/repo1")).thenReturn(repo1Collaborators);
            when(gitHubApiClient.fetchRepoCollaborators("testorg/repo2")).thenReturn(repo2Collaborators);

            // Act
            AccessReport report = reportService.generateReport(org);

            // Assert
            assertNotNull(report);
            assertEquals("testorg", report.getOrganization());
            assertNotNull(report.getGeneratedAt());
            assertEquals(2, report.getTotalRepositories());
            assertEquals(3, report.getTotalUsers()); // alice, bob, charlie

            // Users should be sorted alphabetically
            List<UserAccess> users = report.getUsers();
            assertEquals(3, users.size());
            assertEquals("alice", users.get(0).getUsername());
            assertEquals("bob", users.get(1).getUsername());
            assertEquals("charlie", users.get(2).getUsername());

            // Alice should have access to both repos
            assertEquals(2, users.get(0).getRepositories().size());

            // Bob should have access to repo1 only
            assertEquals(1, users.get(1).getRepositories().size());
            assertEquals("testorg/repo1", users.get(1).getRepositories().get(0).getRepository());

            // Charlie should have access to repo2 only
            assertEquals(1, users.get(2).getRepositories().size());
            assertEquals("testorg/repo2", users.get(2).getRepositories().get(0).getRepository());

            verify(gitHubApiClient).fetchOrganizationRepos(org);
            verify(gitHubApiClient).fetchRepoCollaborators("testorg/repo1");
            verify(gitHubApiClient).fetchRepoCollaborators("testorg/repo2");
        }

        @Test
        @DisplayName("Should generate empty report when organization has no repos")
        void shouldGenerateEmptyReport() {
            // Arrange
            when(gitHubApiClient.fetchOrganizationRepos("emptyorg")).thenReturn(List.of());

            // Act
            AccessReport report = reportService.generateReport("emptyorg");

            // Assert
            assertNotNull(report);
            assertEquals("emptyorg", report.getOrganization());
            assertEquals(0, report.getTotalRepositories());
            assertEquals(0, report.getTotalUsers());
            assertNotNull(report.getUsers());
            assertTrue(report.getUsers().isEmpty());

            verify(gitHubApiClient).fetchOrganizationRepos("emptyorg");
            verify(gitHubApiClient, never()).fetchRepoCollaborators(anyString());
        }

        @Test
        @DisplayName("Should generate report with single repo and single user")
        void shouldGenerateReportSingleRepoSingleUser() {
            // Arrange
            List<Map<String, Object>> repos = List.of(
                    Map.of("full_name", "org/solo-repo")
            );

            List<Map<String, Object>> collaborators = List.of(
                    createCollaborator("solouser", Map.of("admin", true, "push", true, "pull", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/solo-repo")).thenReturn(collaborators);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals(1, report.getTotalRepositories());
            assertEquals(1, report.getTotalUsers());
            assertEquals("solouser", report.getUsers().get(0).getUsername());
            assertEquals("admin", report.getUsers().get(0).getRepositories().get(0).getRole());
        }
    }

    @Nested
    @DisplayName("generateReport - permission extraction")
    class PermissionExtractionTests {

        @Test
        @DisplayName("Should extract admin role when admin permission is true")
        void shouldExtractAdminRole() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));
            List<Map<String, Object>> collabs = List.of(
                    createCollaborator("user1", Map.of("admin", true, "maintain", false, "push", true, "triage", false, "pull", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(collabs);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals("admin", report.getUsers().get(0).getRepositories().get(0).getRole());
        }

        @Test
        @DisplayName("Should extract maintain role when maintain is highest permission")
        void shouldExtractMaintainRole() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));
            List<Map<String, Object>> collabs = List.of(
                    createCollaborator("user1", Map.of("admin", false, "maintain", true, "push", true, "triage", false, "pull", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(collabs);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals("maintain", report.getUsers().get(0).getRepositories().get(0).getRole());
        }

        @Test
        @DisplayName("Should extract write role when push is highest permission")
        void shouldExtractWriteRole() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));
            List<Map<String, Object>> collabs = List.of(
                    createCollaborator("user1", Map.of("admin", false, "maintain", false, "push", true, "triage", false, "pull", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(collabs);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals("write", report.getUsers().get(0).getRepositories().get(0).getRole());
        }

        @Test
        @DisplayName("Should extract triage role when triage is highest permission")
        void shouldExtractTriageRole() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));
            List<Map<String, Object>> collabs = List.of(
                    createCollaborator("user1", Map.of("admin", false, "maintain", false, "push", false, "triage", true, "pull", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(collabs);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals("triage", report.getUsers().get(0).getRepositories().get(0).getRole());
        }

        @Test
        @DisplayName("Should extract read role when pull is the only permission")
        void shouldExtractReadRole() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));
            List<Map<String, Object>> collabs = List.of(
                    createCollaborator("user1", Map.of("admin", false, "maintain", false, "push", false, "triage", false, "pull", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(collabs);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals("read", report.getUsers().get(0).getRepositories().get(0).getRole());
        }

        @Test
        @DisplayName("Should use role_name field when present in collaborator data")
        void shouldUseRoleNameField() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));

            Map<String, Object> collaborator = new HashMap<>();
            collaborator.put("login", "user1");
            collaborator.put("role_name", "admin");
            collaborator.put("permissions", Map.of("admin", true));

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(List.of(collaborator));

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals("admin", report.getUsers().get(0).getRepositories().get(0).getRole());
        }

        @Test
        @DisplayName("Should return 'unknown' when no permissions map is present")
        void shouldReturnUnknownWhenNoPermissions() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));

            Map<String, Object> collaborator = new HashMap<>();
            collaborator.put("login", "user1");
            // No permissions or role_name

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(List.of(collaborator));

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals("unknown", report.getUsers().get(0).getRepositories().get(0).getRole());
        }
    }

    @Nested
    @DisplayName("generateReport - error resilience")
    class ErrorResilienceTests {

        @Test
        @DisplayName("Should skip repo with missing full_name and continue")
        void shouldSkipRepoWithMissingFullName() {
            // Arrange
            Map<String, Object> repoWithName = new HashMap<>();
            repoWithName.put("full_name", "org/good-repo");

            Map<String, Object> repoWithoutName = new HashMap<>();
            repoWithoutName.put("name", "bad-repo");
            // No full_name key

            List<Map<String, Object>> repos = List.of(repoWithName, repoWithoutName);

            List<Map<String, Object>> collaborators = List.of(
                    createCollaborator("alice", Map.of("admin", true, "push", true, "pull", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/good-repo")).thenReturn(collaborators);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals(2, report.getTotalRepositories()); // Both repos counted
            assertEquals(1, report.getTotalUsers()); // Only alice from good-repo
            assertEquals("alice", report.getUsers().get(0).getUsername());

            // Verify collaborators were only fetched for the good repo
            verify(gitHubApiClient).fetchRepoCollaborators("org/good-repo");
        }

        @Test
        @DisplayName("Should skip collaborator with null login and continue")
        void shouldSkipCollaboratorWithNullLogin() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));

            Map<String, Object> goodCollaborator = createCollaborator("alice", Map.of("admin", true));
            Map<String, Object> badCollaborator = new HashMap<>();
            badCollaborator.put("permissions", Map.of("pull", true));
            // No login key

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(List.of(goodCollaborator, badCollaborator));

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            assertEquals(1, report.getTotalUsers()); // Only alice
        }

        @Test
        @DisplayName("Should continue report generation when one repo's collaborator fetch fails")
        void shouldContinueWhenOneRepoFails() {
            // Arrange
            List<Map<String, Object>> repos = List.of(
                    Map.of("full_name", "org/good-repo"),
                    Map.of("full_name", "org/bad-repo")
            );

            List<Map<String, Object>> goodCollabs = List.of(
                    createCollaborator("alice", Map.of("admin", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/good-repo")).thenReturn(goodCollabs);
            when(gitHubApiClient.fetchRepoCollaborators("org/bad-repo"))
                    .thenThrow(new GitHubApiException("Forbidden", 403));

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert - report should still be generated with data from good-repo
            assertNotNull(report);
            assertEquals(2, report.getTotalRepositories());
            assertEquals(1, report.getTotalUsers());
            assertEquals("alice", report.getUsers().get(0).getUsername());
        }
    }

    @Nested
    @DisplayName("generateReport - sorting")
    class SortingTests {

        @Test
        @DisplayName("Should sort users alphabetically in the report")
        void shouldSortUsersAlphabetically() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "org/repo"));

            List<Map<String, Object>> collaborators = List.of(
                    createCollaborator("charlie", Map.of("pull", true)),
                    createCollaborator("alice", Map.of("admin", true)),
                    createCollaborator("bob", Map.of("push", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/repo")).thenReturn(collaborators);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            List<UserAccess> users = report.getUsers();
            assertEquals("alice", users.get(0).getUsername());
            assertEquals("bob", users.get(1).getUsername());
            assertEquals("charlie", users.get(2).getUsername());
        }

        @Test
        @DisplayName("Should sort repositories alphabetically per user")
        void shouldSortRepositoriesPerUser() {
            // Arrange
            List<Map<String, Object>> repos = List.of(
                    Map.of("full_name", "org/zebra-repo"),
                    Map.of("full_name", "org/alpha-repo")
            );

            List<Map<String, Object>> collabs = List.of(
                    createCollaborator("user1", Map.of("admin", true))
            );

            when(gitHubApiClient.fetchOrganizationRepos("org")).thenReturn(repos);
            when(gitHubApiClient.fetchRepoCollaborators("org/zebra-repo")).thenReturn(collabs);
            when(gitHubApiClient.fetchRepoCollaborators("org/alpha-repo")).thenReturn(collabs);

            // Act
            AccessReport report = reportService.generateReport("org");

            // Assert
            List<RepositoryPermission> userRepos = report.getUsers().get(0).getRepositories();
            assertEquals(2, userRepos.size());
            assertEquals("org/alpha-repo", userRepos.get(0).getRepository());
            assertEquals("org/zebra-repo", userRepos.get(1).getRepository());
        }
    }

    /**
     * Helper method to create a collaborator map for test data.
     *
     * @param login       the collaborator's login username
     * @param permissions the permissions map
     * @return a mutable map representing a collaborator
     */
    private Map<String, Object> createCollaborator(String login, Map<String, Boolean> permissions) {
        Map<String, Object> collaborator = new HashMap<>();
        collaborator.put("login", login);
        collaborator.put("permissions", new HashMap<>(permissions));
        return collaborator;
    }
}
