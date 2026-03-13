package com.kavia.githubaccess.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single repository access entry for a user,
 * containing the repository name and the user's role/permission level.
 */
public class RepositoryAccess {

    /** Repository name */
    @JsonProperty("repository")
    private String repository;

    /** Full repository name (owner/repo) */
    @JsonProperty("full_name")
    private String fullName;

    /** User's permission/role on this repository */
    @JsonProperty("role")
    private String role;

    /** Whether the repository is private */
    @JsonProperty("private")
    private boolean isPrivate;

    // PUBLIC_INTERFACE
    /** Default constructor. */
    public RepositoryAccess() {
    }

    // PUBLIC_INTERFACE
    /**
     * Constructs a RepositoryAccess entry.
     *
     * @param repository repository name
     * @param fullName   full name (owner/repo)
     * @param role       permission role
     * @param isPrivate  whether the repo is private
     */
    public RepositoryAccess(String repository, String fullName, String role, boolean isPrivate) {
        this.repository = repository;
        this.fullName = fullName;
        this.role = role;
        this.isPrivate = isPrivate;
    }

    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }
}
