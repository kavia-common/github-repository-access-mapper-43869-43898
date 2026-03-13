package com.example.githubaccessreportbackend.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for DTO classes: AccessReport, ErrorResponse, UserAccess, RepositoryPermission.
 * Verifies constructors, getters, setters, and default values.
 */
@DisplayName("DTO Tests")
class test_DtoTest {

    @Nested
    @DisplayName("AccessReport")
    class AccessReportTests {

        @Test
        @DisplayName("Should initialize with generatedAt timestamp on construction")
        void shouldInitializeGeneratedAt() {
            // Act
            AccessReport report = new AccessReport();

            // Assert
            assertNotNull(report.getGeneratedAt());
            // The timestamp should be an ISO-8601 format string
            assertTrue(report.getGeneratedAt().contains("T"));
        }

        @Test
        @DisplayName("Should set and get all fields correctly")
        void shouldSetAndGetFields() {
            // Arrange
            AccessReport report = new AccessReport();
            UserAccess user = new UserAccess("testuser");

            // Act
            report.setOrganization("myorg");
            report.setGeneratedAt("2025-01-15T10:00:00Z");
            report.setTotalRepositories(42);
            report.setTotalUsers(15);
            report.setUsers(List.of(user));

            // Assert
            assertEquals("myorg", report.getOrganization());
            assertEquals("2025-01-15T10:00:00Z", report.getGeneratedAt());
            assertEquals(42, report.getTotalRepositories());
            assertEquals(15, report.getTotalUsers());
            assertNotNull(report.getUsers());
            assertEquals(1, report.getUsers().size());
            assertEquals("testuser", report.getUsers().get(0).getUsername());
        }

        @Test
        @DisplayName("Should allow overriding generatedAt")
        void shouldAllowOverridingGeneratedAt() {
            // Arrange
            AccessReport report = new AccessReport();
            String customTimestamp = "2025-06-01T12:00:00Z";

            // Act
            report.setGeneratedAt(customTimestamp);

            // Assert
            assertEquals(customTimestamp, report.getGeneratedAt());
        }
    }

    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should initialize with timestamp on default constructor")
        void shouldInitializeTimestamp() {
            // Act
            ErrorResponse error = new ErrorResponse();

            // Assert
            assertNotNull(error.getTimestamp());
        }

        @Test
        @DisplayName("Should create with parameterized constructor")
        void shouldCreateWithParams() {
            // Act
            ErrorResponse error = new ErrorResponse(400, "Bad Request", "Missing parameter");

            // Assert
            assertEquals(400, error.getStatus());
            assertEquals("Bad Request", error.getError());
            assertEquals("Missing parameter", error.getMessage());
            assertNotNull(error.getTimestamp());
        }

        @Test
        @DisplayName("Should set and get all fields")
        void shouldSetAndGetFields() {
            // Arrange
            ErrorResponse error = new ErrorResponse();

            // Act
            error.setStatus(500);
            error.setError("Internal Server Error");
            error.setMessage("Something broke");
            error.setTimestamp("2025-01-15T10:00:00Z");

            // Assert
            assertEquals(500, error.getStatus());
            assertEquals("Internal Server Error", error.getError());
            assertEquals("Something broke", error.getMessage());
            assertEquals("2025-01-15T10:00:00Z", error.getTimestamp());
        }
    }

    @Nested
    @DisplayName("UserAccess")
    class UserAccessTests {

        @Test
        @DisplayName("Should initialize with empty repositories list on default constructor")
        void shouldInitializeWithEmptyRepos() {
            // Act
            UserAccess user = new UserAccess();

            // Assert
            assertNotNull(user.getRepositories());
            assertTrue(user.getRepositories().isEmpty());
        }

        @Test
        @DisplayName("Should initialize with username and empty repositories list")
        void shouldInitializeWithUsername() {
            // Act
            UserAccess user = new UserAccess("alice");

            // Assert
            assertEquals("alice", user.getUsername());
            assertNotNull(user.getRepositories());
            assertTrue(user.getRepositories().isEmpty());
        }

        @Test
        @DisplayName("Should add repository permission entries")
        void shouldAddRepositoryPermission() {
            // Arrange
            UserAccess user = new UserAccess("bob");

            // Act
            user.addRepository("org/repo1", "admin");
            user.addRepository("org/repo2", "read");

            // Assert
            assertEquals(2, user.getRepositories().size());
            assertEquals("org/repo1", user.getRepositories().get(0).getRepository());
            assertEquals("admin", user.getRepositories().get(0).getRole());
            assertEquals("org/repo2", user.getRepositories().get(1).getRepository());
            assertEquals("read", user.getRepositories().get(1).getRole());
        }

        @Test
        @DisplayName("Should set and get username")
        void shouldSetAndGetUsername() {
            // Arrange
            UserAccess user = new UserAccess();

            // Act
            user.setUsername("charlie");

            // Assert
            assertEquals("charlie", user.getUsername());
        }

        @Test
        @DisplayName("Should set repositories list directly")
        void shouldSetRepositoriesList() {
            // Arrange
            UserAccess user = new UserAccess("dave");
            List<RepositoryPermission> repos = List.of(
                    new RepositoryPermission("org/repo1", "write"),
                    new RepositoryPermission("org/repo2", "triage")
            );

            // Act
            user.setRepositories(repos);

            // Assert
            assertEquals(2, user.getRepositories().size());
        }
    }

    @Nested
    @DisplayName("RepositoryPermission")
    class RepositoryPermissionTests {

        @Test
        @DisplayName("Should create with default constructor")
        void shouldCreateWithDefaultConstructor() {
            // Act
            RepositoryPermission rp = new RepositoryPermission();

            // Assert (fields should be null by default)
            assertEquals(null, rp.getRepository());
            assertEquals(null, rp.getRole());
        }

        @Test
        @DisplayName("Should create with parameterized constructor")
        void shouldCreateWithParams() {
            // Act
            RepositoryPermission rp = new RepositoryPermission("org/my-repo", "admin");

            // Assert
            assertEquals("org/my-repo", rp.getRepository());
            assertEquals("admin", rp.getRole());
        }

        @Test
        @DisplayName("Should set and get fields")
        void shouldSetAndGetFields() {
            // Arrange
            RepositoryPermission rp = new RepositoryPermission();

            // Act
            rp.setRepository("org/updated-repo");
            rp.setRole("write");

            // Assert
            assertEquals("org/updated-repo", rp.getRepository());
            assertEquals("write", rp.getRole());
        }
    }
}
