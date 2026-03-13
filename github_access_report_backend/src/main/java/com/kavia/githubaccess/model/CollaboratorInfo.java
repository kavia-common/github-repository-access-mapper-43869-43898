package com.kavia.githubaccess.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a GitHub collaborator (user) with their permission details.
 */
public class CollaboratorInfo {

    /** GitHub username (login) */
    private String login;

    /** User's avatar URL */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** User's profile URL */
    @JsonProperty("html_url")
    private String htmlUrl;

    /** Permission/role on the repository (e.g., admin, write, read) */
    private String role;

    /** Nested permissions object from GitHub API */
    private Permissions permissions;

    // PUBLIC_INTERFACE
    /** Default constructor. */
    public CollaboratorInfo() {
    }

    // PUBLIC_INTERFACE
    /**
     * Constructs a CollaboratorInfo with essential fields.
     *
     * @param login     GitHub username
     * @param avatarUrl avatar URL
     * @param htmlUrl   profile URL
     * @param role      permission role
     */
    public CollaboratorInfo(String login, String avatarUrl, String htmlUrl, String role) {
        this.login = login;
        this.avatarUrl = avatarUrl;
        this.htmlUrl = htmlUrl;
        this.role = role;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    /**
     * Determines the highest permission level from the permissions object.
     *
     * @return the highest role string (admin, maintain, push, triage, pull)
     */
    public String resolveRole() {
        if (this.role != null && !this.role.isBlank()) {
            return this.role;
        }
        if (permissions == null) {
            return "unknown";
        }
        if (Boolean.TRUE.equals(permissions.getAdmin())) return "admin";
        if (Boolean.TRUE.equals(permissions.getMaintain())) return "maintain";
        if (Boolean.TRUE.equals(permissions.getPush())) return "write";
        if (Boolean.TRUE.equals(permissions.getTriage())) return "triage";
        if (Boolean.TRUE.equals(permissions.getPull())) return "read";
        return "none";
    }

    /**
     * Nested permissions object matching GitHub API response.
     */
    public static class Permissions {
        private Boolean admin;
        private Boolean maintain;
        private Boolean push;
        private Boolean triage;
        private Boolean pull;

        public Boolean getAdmin() { return admin; }
        public void setAdmin(Boolean admin) { this.admin = admin; }
        public Boolean getMaintain() { return maintain; }
        public void setMaintain(Boolean maintain) { this.maintain = maintain; }
        public Boolean getPush() { return push; }
        public void setPush(Boolean push) { this.push = push; }
        public Boolean getTriage() { return triage; }
        public void setTriage(Boolean triage) { this.triage = triage; }
        public Boolean getPull() { return pull; }
        public void setPull(Boolean pull) { this.pull = pull; }
    }
}
