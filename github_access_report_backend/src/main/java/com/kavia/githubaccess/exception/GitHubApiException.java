package com.kavia.githubaccess.exception;

/**
 * Custom exception for GitHub API related errors such as
 * authentication failures, rate limits, and API errors.
 */
public class GitHubApiException extends RuntimeException {

    private final int statusCode;

    // PUBLIC_INTERFACE
    /**
     * Constructs a GitHubApiException.
     *
     * @param message    error message
     * @param statusCode HTTP status code from GitHub API
     */
    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    // PUBLIC_INTERFACE
    /**
     * Constructs a GitHubApiException with a cause.
     *
     * @param message    error message
     * @param statusCode HTTP status code
     * @param cause      root cause
     */
    public GitHubApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /** Get the HTTP status code. */
    public int getStatusCode() {
        return statusCode;
    }
}
