package com.example.githubaccessreportbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Top-level response DTO for the access report API endpoint.
 * Contains metadata about the scan and the aggregated user access data.
 */
// PUBLIC_INTERFACE
@Schema(description = "Full access report for a GitHub organization")
public class AccessReport {

    @Schema(description = "Name of the GitHub organization that was scanned", example = "myorg")
    private String organization;

    @Schema(description = "ISO-8601 timestamp when the report was generated")
    private String generatedAt;

    @Schema(description = "Total number of repositories scanned")
    private int totalRepositories;

    @Schema(description = "Total number of unique users found with access")
    private int totalUsers;

    @Schema(description = "List of users and their repository access details")
    private List<UserAccess> users;

    public AccessReport() {
        this.generatedAt = Instant.now().toString();
    }

    // PUBLIC_INTERFACE
    /** Returns the organization name. */
    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    // PUBLIC_INTERFACE
    /** Returns the ISO-8601 generation timestamp. */
    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    // PUBLIC_INTERFACE
    /** Returns the total repositories scanned. */
    public int getTotalRepositories() {
        return totalRepositories;
    }

    public void setTotalRepositories(int totalRepositories) {
        this.totalRepositories = totalRepositories;
    }

    // PUBLIC_INTERFACE
    /** Returns the total unique users found. */
    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    // PUBLIC_INTERFACE
    /** Returns the list of user access details. */
    public List<UserAccess> getUsers() {
        return users;
    }

    public void setUsers(List<UserAccess> users) {
        this.users = users;
    }
}
