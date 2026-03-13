package com.example.githubaccessreportbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard error response DTO returned when the API encounters a problem.
 */
// PUBLIC_INTERFACE
@Schema(description = "Error response containing status code and error details")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "500")
    private int status;

    @Schema(description = "Error category", example = "Internal Server Error")
    private String error;

    @Schema(description = "Human-readable error message", example = "Failed to fetch repositories")
    private String message;

    @Schema(description = "ISO-8601 timestamp of when the error occurred")
    private String timestamp;

    public ErrorResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ErrorResponse(int status, String error, String message) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    // PUBLIC_INTERFACE
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    // PUBLIC_INTERFACE
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // PUBLIC_INTERFACE
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // PUBLIC_INTERFACE
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
