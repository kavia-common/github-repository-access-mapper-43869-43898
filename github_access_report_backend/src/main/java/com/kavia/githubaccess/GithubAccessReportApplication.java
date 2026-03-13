package com.kavia.githubaccess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the GitHub Access Report Backend application.
 *
 * This service connects to GitHub, retrieves repository and collaborator
 * data for a given organization or user, and exposes an API endpoint
 * that returns a structured JSON access report.
 */
@SpringBootApplication
public class GithubAccessReportApplication {

    // PUBLIC_INTERFACE
    /**
     * Main method to launch the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GithubAccessReportApplication.class, args);
    }
}
