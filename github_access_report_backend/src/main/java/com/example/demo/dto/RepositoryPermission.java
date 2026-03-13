package com.example.githubaccessreportbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single repository and the permission / role a user holds on it.
 */
// PUBLIC_INTERFACE
@Schema(description = "A repository and the permission level a user has on it")
public class RepositoryPermission {

    @Schema(description = "Full name of the repository (org/repo)", example = "myorg/my-repo")
    private String repository;

    @Schema(description = "Permission role on the repository", example = "admin")
    private String role;

    public RepositoryPermission() {
    }

    public RepositoryPermission(String repository, String role) {
        this.repository = repository;
        this.role = role;
    }

    // PUBLIC_INTERFACE
    /** Returns the repository full name. */
    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    // PUBLIC_INTERFACE
    /** Returns the permission role. */
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
