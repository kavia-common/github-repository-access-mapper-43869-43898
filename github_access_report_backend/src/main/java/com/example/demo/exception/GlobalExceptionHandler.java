package com.example.githubaccessreportbackend.exception;

import com.example.githubaccessreportbackend.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that translates exceptions into structured
 * JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // PUBLIC_INTERFACE
    /**
     * Handles GitHubApiException by mapping the upstream status code
     * to an appropriate HTTP response.
     *
     * @param ex the caught GitHubApiException
     * @return ResponseEntity with an ErrorResponse body
     */
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ErrorResponse> handleGitHubApiException(GitHubApiException ex) {
        logger.error("GitHub API error: {} (status {})", ex.getMessage(), ex.getStatusCode());

        HttpStatus status;
        if (ex.getStatusCode() == 401 || ex.getStatusCode() == 403) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex.getStatusCode() == 404) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getStatusCode() == 429) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    // PUBLIC_INTERFACE
    /**
     * Handles IllegalArgumentException (e.g., missing org parameter).
     *
     * @param ex the caught IllegalArgumentException
     * @return ResponseEntity with an ErrorResponse body
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // PUBLIC_INTERFACE
    /**
     * Catch-all handler for unexpected exceptions.
     *
     * @param ex the caught exception
     * @return ResponseEntity with an ErrorResponse body
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred: " + ex.getMessage()
        );
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}
