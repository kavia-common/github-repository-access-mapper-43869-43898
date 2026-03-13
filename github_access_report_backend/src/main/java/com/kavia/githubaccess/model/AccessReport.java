package com.kavia.githubaccess.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * The complete access report returned by the API.
 * Contains metadata and the user-to-repository access mapping.
 */
public class AccessReport {

    /** Organization or user name the report was generated for */
    @JsonProperty("organization")
    private String organization;

    /** ISO-8601 timestamp of when the report was generated */
    @JsonProperty("generated_at")
    private String generatedAt;

    /** Total number of repositories found */
    @JsonProperty("total_repositories")
    private int totalRepositories;

    /** Total number of unique users found */
    @JsonProperty("total_users")
    private int totalUsers;

    /** List of users and their repository access */
    @JsonProperty("users")
    private List<UserAccess> users;

    // PUBLIC_INTERFACE
    /** Default constructor. */
    public AccessReport() {
    }

    // PUBLIC_INTERFACE
    /**
     * Constructs an AccessReport.
     *
     * @param organization      org/user name
     * @param totalRepositories total repo count
     * @param totalUsers        total unique user count
     * @param users             list of user access entries
     */
    public AccessReport(String organization, int totalRepositories, int totalUsers, List<UserAccess> users) {
        this.organization = organization;
        this.generatedAt = Instant.now().toString();
        this.totalRepositories = totalRepositories;
        this.totalUsers = totalUsers;
        this.users = users;
    }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public int getTotalRepositories() { return totalRepositories; }
    public void setTotalRepositories(int totalRepositories) { this.totalRepositories = totalRepositories; }
    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
    public List<UserAccess> getUsers() { return users; }
    public void setUsers(List<UserAccess> users) { this.users = users; }
}
