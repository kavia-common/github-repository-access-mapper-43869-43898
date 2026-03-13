package com.example.githubaccessreportbackend.exception;

import com.example.githubaccessreportbackend.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * Verifies that each exception type is mapped to the correct HTTP status and error response.
 */
@DisplayName("GlobalExceptionHandler Tests")
class test_GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should map GitHubApiException with 401 to FORBIDDEN status")
    void shouldMapGitHubApiException401ToForbidden() {
        // Arrange
        GitHubApiException ex = new GitHubApiException("Auth failed", 401);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGitHubApiException(ex);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("Auth failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Should map GitHubApiException with 403 to FORBIDDEN status")
    void shouldMapGitHubApiException403ToForbidden() {
        // Arrange
        GitHubApiException ex = new GitHubApiException("Insufficient permissions", 403);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGitHubApiException(ex);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Should map GitHubApiException with 404 to NOT_FOUND status")
    void shouldMapGitHubApiException404ToNotFound() {
        // Arrange
        GitHubApiException ex = new GitHubApiException("Org not found", 404);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGitHubApiException(ex);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Org not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should map GitHubApiException with 429 to TOO_MANY_REQUESTS status")
    void shouldMapGitHubApiException429ToTooManyRequests() {
        // Arrange
        GitHubApiException ex = new GitHubApiException("Rate limit exceeded", 429);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGitHubApiException(ex);

        // Assert
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(429, response.getBody().getStatus());
        assertEquals("Too Many Requests", response.getBody().getError());
    }

    @Test
    @DisplayName("Should map GitHubApiException with 500 to BAD_GATEWAY status")
    void shouldMapGitHubApiException500ToBadGateway() {
        // Arrange
        GitHubApiException ex = new GitHubApiException("GitHub server error", 500);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGitHubApiException(ex);

        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(502, response.getBody().getStatus());
        assertEquals("Bad Gateway", response.getBody().getError());
    }

    @Test
    @DisplayName("Should map unknown status codes to BAD_GATEWAY")
    void shouldMapUnknownStatusToBadGateway() {
        // Arrange
        GitHubApiException ex = new GitHubApiException("Unknown error", 418);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGitHubApiException(ex);

        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Should map IllegalArgumentException to BAD_REQUEST status")
    void shouldMapIllegalArgumentToBadRequest() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Organization name is required.");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Organization name is required.", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Should map generic Exception to INTERNAL_SERVER_ERROR status")
    void shouldMapGenericExceptionToInternalServerError() {
        // Arrange
        Exception ex = new RuntimeException("Unexpected failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("An unexpected error occurred: Unexpected failure", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Should handle GitHubApiException with cause")
    void shouldHandleGitHubApiExceptionWithCause() {
        // Arrange
        RuntimeException cause = new RuntimeException("root cause");
        GitHubApiException ex = new GitHubApiException("Wrapped error", 503, cause);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGitHubApiException(ex);

        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("Wrapped error", response.getBody().getMessage());
    }
}
