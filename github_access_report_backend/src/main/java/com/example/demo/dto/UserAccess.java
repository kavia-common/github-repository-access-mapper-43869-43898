package com.example.githubaccessreportbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated access view for a single GitHub user, listing all
 * repositories the user can access along with permission levels.
 */
// PUBLIC_INTERFACE
@Schema(description = "A user and the list of repositories they can access with permission details")
public class UserAccess {

    @Schema(description = "GitHub login / username", example = "octocat")
    private String username;

    @Schema(description = "List of repositories and associated permission roles")
    private List<RepositoryPermission> repositories;

    public UserAccess() {
        this.repositories = new ArrayList<>();
    }

    public UserAccess(String username) {
        this.username = username;
        this.repositories = new ArrayList<>();
    }

    // PUBLIC_INTERFACE
    /** Returns the GitHub username. */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // PUBLIC_INTERFACE
    /** Returns the list of repository permissions for this user. */
    public List<RepositoryPermission> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryPermission> repositories) {
        this.repositories = repositories;
    }

    /**
     * Adds a repository permission entry to this user's access list.
     *
     * @param repoFullName full name of the repository (org/repo)
     * @param role         permission role (e.g. admin, push, pull)
     */
    public void addRepository(String repoFullName, String role) {
        this.repositories.add(new RepositoryPermission(repoFullName, role));
    }
}
