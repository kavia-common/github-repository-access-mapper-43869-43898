package com.example.githubaccessreportbackend.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link GitHubApiException}.
 * Verifies constructor behavior and field access.
 */
@DisplayName("GitHubApiException Tests")
class test_GitHubApiExceptionTest {

    @Test
    @DisplayName("Should create exception with message and status code")
    void shouldCreateExceptionWithMessageAndStatusCode() {
        // Act
        GitHubApiException ex = new GitHubApiException("Test error", 404);

        // Assert
        assertEquals("Test error", ex.getMessage());
        assertEquals(404, ex.getStatusCode());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("Should create exception with message, status code, and cause")
    void shouldCreateExceptionWithCause() {
        // Arrange
        RuntimeException cause = new RuntimeException("root cause");

        // Act
        GitHubApiException ex = new GitHubApiException("Wrapped error", 500, cause);

        // Assert
        assertEquals("Wrapped error", ex.getMessage());
        assertEquals(500, ex.getStatusCode());
        assertNotNull(ex.getCause());
        assertSame(cause, ex.getCause());
        assertEquals("root cause", ex.getCause().getMessage());
    }

    @Test
    @DisplayName("Should preserve various HTTP status codes")
    void shouldPreserveStatusCodes() {
        assertEquals(401, new GitHubApiException("Unauthorized", 401).getStatusCode());
        assertEquals(403, new GitHubApiException("Forbidden", 403).getStatusCode());
        assertEquals(429, new GitHubApiException("Rate limit", 429).getStatusCode());
        assertEquals(502, new GitHubApiException("Bad gateway", 502).getStatusCode());
    }

    @Test
    @DisplayName("Should be a RuntimeException subclass")
    void shouldBeRuntimeException() {
        // Act
        GitHubApiException ex = new GitHubApiException("Test", 500);

        // Assert - verify it is a RuntimeException (unchecked)
        assertEquals(true, ex instanceof RuntimeException);
    }
}
