package com.example.githubaccessreportbackend.client;

import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GitHubApiClient}.
 * All GitHub API interactions are mocked via RestTemplate to prevent real network calls.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubApiClient Tests")
class test_GitHubApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private GitHubProperties properties;

    private GitHubApiClient gitHubApiClient;

    @BeforeEach
    void setUp() {
        properties = new GitHubProperties();
        properties.setApiBaseUrl("https://api.github.com");
        properties.setPageSize(100);
        properties.setMaxConcurrency(10);
        properties.setToken("test-token");
        gitHubApiClient = new GitHubApiClient(restTemplate, properties);
    }

    @Nested
    @DisplayName("fetchOrganizationRepos")
    class FetchOrganizationReposTests {

        @Test
        @DisplayName("Should fetch repos for a given organization successfully")
        void shouldFetchReposSuccessfully() {
            // Arrange
            List<Map<String, Object>> repos = List.of(
                    Map.of("full_name", "myorg/repo1", "name", "repo1"),
                    Map.of("full_name", "myorg/repo2", "name", "repo2")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RateLimit-Remaining", "4999");
            headers.set("X-RateLimit-Limit", "5000");

            ResponseEntity<List<Map<String, Object>>> responseEntity =
                    new ResponseEntity<>(repos, headers, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(responseEntity);

            // Act
            List<Map<String, Object>> result = gitHubApiClient.fetchOrganizationRepos("myorg");

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("myorg/repo1", result.get(0).get("full_name"));
            assertEquals("myorg/repo2", result.get(1).get("full_name"));

            verify(restTemplate, times(1)).exchange(
                    eq("https://api.github.com/orgs/myorg/repos?per_page=100&type=all"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            );
        }

        @Test
        @DisplayName("Should return empty list when organization has no repos")
        void shouldReturnEmptyListWhenNoRepos() {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RateLimit-Remaining", "4999");
            headers.set("X-RateLimit-Limit", "5000");

            ResponseEntity<List<Map<String, Object>>> responseEntity =
                    new ResponseEntity<>(List.of(), headers, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(responseEntity);

            // Act
            List<Map<String, Object>> result = gitHubApiClient.fetchOrganizationRepos("emptyorg");

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle pagination via Link headers")
        void shouldHandlePagination() {
            // Arrange - Page 1
            List<Map<String, Object>> page1 = List.of(
                    Map.of("full_name", "myorg/repo1")
            );
            HttpHeaders headers1 = new HttpHeaders();
            headers1.set("X-RateLimit-Remaining", "4998");
            headers1.set("X-RateLimit-Limit", "5000");
            headers1.add("Link", "<https://api.github.com/orgs/myorg/repos?page=2>; rel=\"next\"");

            ResponseEntity<List<Map<String, Object>>> response1 =
                    new ResponseEntity<>(page1, headers1, HttpStatus.OK);

            // Arrange - Page 2 (last page, no next link)
            List<Map<String, Object>> page2 = List.of(
                    Map.of("full_name", "myorg/repo2")
            );
            HttpHeaders headers2 = new HttpHeaders();
            headers2.set("X-RateLimit-Remaining", "4997");
            headers2.set("X-RateLimit-Limit", "5000");

            ResponseEntity<List<Map<String, Object>>> response2 =
                    new ResponseEntity<>(page2, headers2, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq("https://api.github.com/orgs/myorg/repos?per_page=100&type=all"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(response1);

            when(restTemplate.exchange(
                    eq("https://api.github.com/orgs/myorg/repos?page=2"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(response2);

            // Act
            List<Map<String, Object>> result = gitHubApiClient.fetchOrganizationRepos("myorg");

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("myorg/repo1", result.get(0).get("full_name"));
            assertEquals("myorg/repo2", result.get(1).get("full_name"));

            verify(restTemplate, times(2)).exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            );
        }

        @Test
        @DisplayName("Should handle null body in response gracefully")
        void shouldHandleNullBody() {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RateLimit-Remaining", "4999");
            headers.set("X-RateLimit-Limit", "5000");

            ResponseEntity<List<Map<String, Object>>> responseEntity =
                    new ResponseEntity<>(null, headers, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(responseEntity);

            // Act
            List<Map<String, Object>> result = gitHubApiClient.fetchOrganizationRepos("myorg");

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("fetchRepoCollaborators")
    class FetchRepoCollaboratorsTests {

        @Test
        @DisplayName("Should fetch collaborators for a given repository")
        void shouldFetchCollaboratorsSuccessfully() {
            // Arrange
            List<Map<String, Object>> collaborators = List.of(
                    Map.of("login", "alice", "permissions", Map.of("admin", true, "push", true, "pull", true)),
                    Map.of("login", "bob", "permissions", Map.of("admin", false, "push", false, "pull", true))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RateLimit-Remaining", "4999");
            headers.set("X-RateLimit-Limit", "5000");

            ResponseEntity<List<Map<String, Object>>> responseEntity =
                    new ResponseEntity<>(collaborators, headers, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(responseEntity);

            // Act
            List<Map<String, Object>> result = gitHubApiClient.fetchRepoCollaborators("myorg/repo1");

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("alice", result.get(0).get("login"));
            assertEquals("bob", result.get(1).get("login"));

            verify(restTemplate).exchange(
                    eq("https://api.github.com/repos/myorg/repo1/collaborators?per_page=100&affiliation=all"),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            );
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw GitHubApiException with 401 for authentication failure")
        void shouldThrowExceptionOn401() {
            // Arrange
            HttpClientErrorException exception = HttpClientErrorException.create(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    HttpHeaders.EMPTY,
                    "Bad credentials".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(exception);

            // Act & Assert
            GitHubApiException thrown = assertThrows(GitHubApiException.class,
                    () -> gitHubApiClient.fetchOrganizationRepos("myorg"));

            assertEquals(401, thrown.getStatusCode());
            assertTrue(thrown.getMessage().contains("Authentication failed"));
        }

        @Test
        @DisplayName("Should throw GitHubApiException with 429 for rate limit exceeded via 403")
        void shouldThrowExceptionOnRateLimitVia403() {
            // Arrange
            HttpClientErrorException exception = HttpClientErrorException.create(
                    HttpStatus.FORBIDDEN,
                    "Forbidden",
                    HttpHeaders.EMPTY,
                    "API rate limit exceeded".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(exception);

            // Act & Assert
            GitHubApiException thrown = assertThrows(GitHubApiException.class,
                    () -> gitHubApiClient.fetchOrganizationRepos("myorg"));

            assertEquals(429, thrown.getStatusCode());
            assertTrue(thrown.getMessage().contains("rate limit"));
        }

        @Test
        @DisplayName("Should throw GitHubApiException with 403 for insufficient permissions")
        void shouldThrowExceptionOn403Permissions() {
            // Arrange
            HttpClientErrorException exception = HttpClientErrorException.create(
                    HttpStatus.FORBIDDEN,
                    "Forbidden",
                    HttpHeaders.EMPTY,
                    "Resource not accessible".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(exception);

            // Act & Assert
            GitHubApiException thrown = assertThrows(GitHubApiException.class,
                    () -> gitHubApiClient.fetchOrganizationRepos("myorg"));

            assertEquals(403, thrown.getStatusCode());
            assertTrue(thrown.getMessage().contains("insufficient permissions"));
        }

        @Test
        @DisplayName("Should throw GitHubApiException with 404 for resource not found")
        void shouldThrowExceptionOn404() {
            // Arrange
            HttpClientErrorException exception = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    HttpHeaders.EMPTY,
                    "Not Found".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(exception);

            // Act & Assert
            GitHubApiException thrown = assertThrows(GitHubApiException.class,
                    () -> gitHubApiClient.fetchOrganizationRepos("nonexistent"));

            assertEquals(404, thrown.getStatusCode());
            assertTrue(thrown.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should throw GitHubApiException for generic 4xx client errors")
        void shouldThrowExceptionOnGeneric4xx() {
            // Arrange
            HttpClientErrorException exception = HttpClientErrorException.create(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unprocessable Entity",
                    HttpHeaders.EMPTY,
                    "Validation Failed".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(exception);

            // Act & Assert
            GitHubApiException thrown = assertThrows(GitHubApiException.class,
                    () -> gitHubApiClient.fetchOrganizationRepos("myorg"));

            assertEquals(422, thrown.getStatusCode());
        }

        @Test
        @DisplayName("Should throw GitHubApiException for 5xx server errors")
        void shouldThrowExceptionOn5xx() {
            // Arrange
            HttpServerErrorException exception = HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error",
                    HttpHeaders.EMPTY,
                    "Server Error".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(exception);

            // Act & Assert
            GitHubApiException thrown = assertThrows(GitHubApiException.class,
                    () -> gitHubApiClient.fetchOrganizationRepos("myorg"));

            assertEquals(500, thrown.getStatusCode());
            assertTrue(thrown.getMessage().contains("GitHub server error"));
        }

        @Test
        @DisplayName("Should throw GitHubApiException with 502 for network errors")
        void shouldThrowExceptionOnNetworkError() {
            // Arrange
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new RestClientException("Connection refused"));

            // Act & Assert
            GitHubApiException thrown = assertThrows(GitHubApiException.class,
                    () -> gitHubApiClient.fetchOrganizationRepos("myorg"));

            assertEquals(502, thrown.getStatusCode());
            assertTrue(thrown.getMessage().contains("Failed to communicate"));
        }
    }

    @Nested
    @DisplayName("Rate limit handling")
    class RateLimitTests {

        @Test
        @DisplayName("Should throw GitHubApiException when rate limit is exhausted")
        void shouldThrowExceptionWhenRateLimitExhausted() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "myorg/repo1"));
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RateLimit-Remaining", "0");
            headers.set("X-RateLimit-Limit", "5000");
            headers.set("X-RateLimit-Reset", "1700000000");

            ResponseEntity<List<Map<String, Object>>> responseEntity =
                    new ResponseEntity<>(repos, headers, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(responseEntity);

            // Act & Assert
            GitHubApiException thrown = assertThrows(GitHubApiException.class,
                    () -> gitHubApiClient.fetchOrganizationRepos("myorg"));

            assertEquals(429, thrown.getStatusCode());
            assertTrue(thrown.getMessage().contains("rate limit exhausted"));
        }

        @Test
        @DisplayName("Should continue successfully when rate limit is healthy")
        void shouldContinueWhenRateLimitHealthy() {
            // Arrange
            List<Map<String, Object>> repos = List.of(Map.of("full_name", "myorg/repo1"));
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RateLimit-Remaining", "4500");
            headers.set("X-RateLimit-Limit", "5000");

            ResponseEntity<List<Map<String, Object>>> responseEntity =
                    new ResponseEntity<>(repos, headers, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(responseEntity);

            // Act
            List<Map<String, Object>> result = gitHubApiClient.fetchOrganizationRepos("myorg");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }
}
