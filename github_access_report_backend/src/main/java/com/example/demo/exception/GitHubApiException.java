package com.example.githubaccessreportbackend.exception;

/**
 * Custom exception thrown when a GitHub API call fails.
 * Carries the HTTP status code from the upstream response.
 */
// PUBLIC_INTERFACE
public class GitHubApiException extends RuntimeException {

    private final int statusCode;

    /**
     * Constructs a new GitHubApiException.
     *
     * @param message    human-readable error description
     * @param statusCode the HTTP status code from the GitHub API response
     */
    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs a new GitHubApiException with a cause.
     *
     * @param message    human-readable error description
     * @param statusCode the HTTP status code from the GitHub API response
     * @param cause      the underlying cause
     */
    public GitHubApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    // PUBLIC_INTERFACE
    /** Returns the HTTP status code from the failed GitHub API call. */
    public int getStatusCode() {
        return statusCode;
    }
}
