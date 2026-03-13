package com.kavia.githubaccess.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a GitHub repository with basic metadata.
 */
public class RepositoryInfo {

    /** Repository name */
    private String name;

    /** Full repository name (owner/repo) */
    @JsonProperty("full_name")
    private String fullName;

    /** Whether the repository is private */
    @JsonProperty("private")
    private boolean isPrivate;

    /** Repository HTML URL */
    @JsonProperty("html_url")
    private String htmlUrl;

    /** Repository description */
    private String description;

    /** Whether the repository is a fork */
    private boolean fork;

    // PUBLIC_INTERFACE
    /** Default constructor. */
    public RepositoryInfo() {
    }

    // PUBLIC_INTERFACE
    /**
     * Constructs a RepositoryInfo with essential fields.
     *
     * @param name      repository name
     * @param fullName  full name (owner/repo)
     * @param isPrivate whether the repo is private
     * @param htmlUrl   HTML URL of the repo
     */
    public RepositoryInfo(String name, String fullName, boolean isPrivate, String htmlUrl) {
        this.name = name;
        this.fullName = fullName;
        this.isPrivate = isPrivate;
        this.htmlUrl = htmlUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }
}
