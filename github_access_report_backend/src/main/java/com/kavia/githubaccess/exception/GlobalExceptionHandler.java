package com.kavia.githubaccess.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler that catches all exceptions thrown by
 * controllers and returns structured JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // PUBLIC_INTERFACE
    /**
     * Handles GitHubApiException and maps it to appropriate HTTP status codes.
     *
     * @param ex the GitHubApiException
     * @return structured error response
     */
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubApiException(GitHubApiException ex) {
        logger.error("GitHub API error: {} (status: {})", ex.getMessage(), ex.getStatusCode());

        HttpStatus status;
        switch (ex.getStatusCode()) {
            case 401:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case 403:
                status = HttpStatus.FORBIDDEN;
                break;
            case 404:
                status = HttpStatus.NOT_FOUND;
                break;
            case 429:
                status = HttpStatus.TOO_MANY_REQUESTS;
                break;
            default:
                status = HttpStatus.BAD_GATEWAY;
                break;
        }

        return ResponseEntity.status(status).body(buildErrorBody(status, ex.getMessage()));
    }

    // PUBLIC_INTERFACE
    /**
     * Handles IllegalArgumentException for bad configuration or input.
     *
     * @param ex the exception
     * @return structured error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    // PUBLIC_INTERFACE
    /**
     * Catch-all handler for unexpected exceptions.
     *
     * @param ex the exception
     * @return structured error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred: " + ex.getMessage()));
    }

    /**
     * Builds a standard error response body.
     */
    private Map<String, Object> buildErrorBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
