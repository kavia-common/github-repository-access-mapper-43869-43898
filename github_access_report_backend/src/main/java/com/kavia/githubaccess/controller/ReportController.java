package com.kavia.githubaccess.controller;

import com.kavia.githubaccess.model.AccessReport;
import com.kavia.githubaccess.service.AccessReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that exposes the GitHub access report API endpoints.
 * Provides the main report generation endpoint as well as health check
 * and API information endpoints.
 */
@RestController
@Tag(name = "GitHub Access Report", description = "Endpoints for generating and retrieving GitHub repository access reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final AccessReportService reportService;

    // PUBLIC_INTERFACE
    /**
     * Constructs the ReportController.
     *
     * @param reportService the service for generating access reports
     */
    public ReportController(AccessReportService reportService) {
        this.reportService = reportService;
    }

    // PUBLIC_INTERFACE
    /**
     * Generates and returns the GitHub access report for the configured
     * organization or user. Uses the GITHUB_ORG environment variable.
     *
     * @return JSON access report mapping users to repositories
     */
    @GetMapping(value = "/api/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Generate GitHub Access Report",
            description = "Generates a complete report showing which users have access to which "
                    + "repositories within the configured GitHub organization or user account. "
                    + "Uses GITHUB_ORG environment variable as the default target."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration (missing token or org)"),
            @ApiResponse(responseCode = "401", description = "GitHub authentication failed"),
            @ApiResponse(responseCode = "403", description = "Access forbidden or rate limited"),
            @ApiResponse(responseCode = "404", description = "Organization/user not found"),
            @ApiResponse(responseCode = "429", description = "GitHub API rate limit exceeded"),
            @ApiResponse(responseCode = "502", description = "GitHub API error")
    })
    public ResponseEntity<AccessReport> generateReport() {
        logger.info("Received request to generate access report");
        AccessReport report = reportService.generateReport();
        return ResponseEntity.ok(report);
    }

    // PUBLIC_INTERFACE
    /**
     * Generates and returns the GitHub access report for a specific
     * organization or user, overriding the configured default.
     *
     * @param owner the GitHub organization or user name
     * @return JSON access report mapping users to repositories
     */
    @GetMapping(value = "/api/report/{owner}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Generate GitHub Access Report for specific owner",
            description = "Generates a report for a specific GitHub organization or user, "
                    + "overriding the default GITHUB_ORG configuration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid owner name"),
            @ApiResponse(responseCode = "401", description = "GitHub authentication failed"),
            @ApiResponse(responseCode = "403", description = "Access forbidden or rate limited"),
            @ApiResponse(responseCode = "404", description = "Organization/user not found"),
            @ApiResponse(responseCode = "429", description = "GitHub API rate limit exceeded"),
            @ApiResponse(responseCode = "502", description = "GitHub API error")
    })
    public ResponseEntity<AccessReport> generateReportForOwner(
            @Parameter(description = "GitHub organization or user name")
            @PathVariable String owner) {
        logger.info("Received request to generate access report for owner: {}", owner);
        AccessReport report = reportService.generateReportForOwner(owner);
        return ResponseEntity.ok(report);
    }

    // PUBLIC_INTERFACE
    /**
     * Health check endpoint that returns service status.
     * This is a lightweight endpoint for monitoring.
     *
     * @return health status JSON
     */
    @GetMapping(value = "/healthz", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Health Check",
            description = "Returns the health status of the service. "
                    + "Use this for monitoring and load balancer health checks."
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "github-access-report",
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    // PUBLIC_INTERFACE
    /**
     * Root endpoint providing API information and available endpoints.
     *
     * @return API information JSON
     */
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "API Information",
            description = "Returns basic API information and links to available endpoints."
    )
    public ResponseEntity<Map<String, Object>> apiInfo() {
        return ResponseEntity.ok(Map.of(
                "service", "GitHub Access Report API",
                "version", "1.0.0",
                "description", "Service that generates reports showing user access to GitHub repositories",
                "endpoints", Map.of(
                        "report", "/api/report",
                        "report_for_owner", "/api/report/{owner}",
                        "health", "/healthz",
                        "swagger_ui", "/swagger-ui.html",
                        "api_docs", "/api-docs"
                )
        ));
    }
}
