package com.kavia.githubaccess.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a single user's aggregated access across all repositories.
 * Contains the username and a list of repositories they have access to.
 */
public class UserAccess {

    /** GitHub username */
    @JsonProperty("username")
    private String username;

    /** User's avatar URL */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** List of repositories this user has access to */
    @JsonProperty("repositories")
    private List<RepositoryAccess> repositories;

    // PUBLIC_INTERFACE
    /** Default constructor. */
    public UserAccess() {
    }

    // PUBLIC_INTERFACE
    /**
     * Constructs a UserAccess entry.
     *
     * @param username     GitHub username
     * @param avatarUrl    user's avatar URL
     * @param repositories list of repository access entries
     */
    public UserAccess(String username, String avatarUrl, List<RepositoryAccess> repositories) {
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.repositories = repositories;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public List<RepositoryAccess> getRepositories() { return repositories; }
    public void setRepositories(List<RepositoryAccess> repositories) { this.repositories = repositories; }
}
