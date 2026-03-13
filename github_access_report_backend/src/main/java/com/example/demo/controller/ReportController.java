package com.example.githubaccessreportbackend.controller;

import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.dto.AccessReport;
import com.example.githubaccessreportbackend.dto.ErrorResponse;
import com.example.githubaccessreportbackend.service.GitHubAccessReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the GitHub organization access report endpoint.
 * Generates a JSON report mapping users to the repositories they can access,
 * including their permission levels.
 */
// PUBLIC_INTERFACE
@RestController
@RequestMapping("/api")
@Tag(name = "Access Report", description = "Endpoints for generating GitHub organization repository access reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final GitHubAccessReportService reportService;
    private final GitHubProperties gitHubProperties;

    public ReportController(GitHubAccessReportService reportService, GitHubProperties gitHubProperties) {
        this.reportService = reportService;
        this.gitHubProperties = gitHubProperties;
    }

    // PUBLIC_INTERFACE
    /**
     * Generates and returns a JSON access report for the specified GitHub organization.
     * <p>
     * The report maps each user to the repositories they have access to, along with
     * their permission level (admin, write, read, etc.).
     * <p>
     * If no organization is specified via the query parameter, the default organization
     * configured via the GITHUB_ORG environment variable is used.
     *
     * @param org optional organization name; overrides the default GITHUB_ORG config
     * @return ResponseEntity containing the AccessReport as JSON
     */
    @GetMapping(value = "/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Generate access report",
            description = "Returns a JSON report mapping each user to the GitHub repositories " +
                    "they can access within the specified organization, including permission levels. " +
                    "If no 'org' query parameter is provided, the default GITHUB_ORG is used."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully",
                    content = @Content(schema = @Schema(implementation = AccessReport.class))),
            @ApiResponse(responseCode = "400", description = "Organization name is missing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authentication failed or insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "GitHub API rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AccessReport> getAccessReport(
            @Parameter(description = "GitHub organization name (overrides default GITHUB_ORG)",
                    example = "spring-projects")
            @RequestParam(value = "org", required = false) String org) {

        // Resolve the organization: query param takes precedence over config default
        String targetOrg = resolveOrganization(org);

        logger.info("Access report requested for organization: {}", targetOrg);
        AccessReport report = reportService.generateReport(targetOrg);
        return ResponseEntity.ok(report);
    }

    /**
     * Resolves the target organization name from the query parameter or configuration.
     *
     * @param orgParam the optional query parameter value
     * @return the resolved organization name
     * @throws IllegalArgumentException if no organization can be determined
     */
    private String resolveOrganization(String orgParam) {
        if (orgParam != null && !orgParam.isBlank()) {
            return orgParam.trim();
        }
        String defaultOrg = gitHubProperties.getOrg();
        if (defaultOrg != null && !defaultOrg.isBlank()) {
            return defaultOrg.trim();
        }
        throw new IllegalArgumentException(
                "Organization name is required. Provide it via the 'org' query parameter " +
                "or set the GITHUB_ORG environment variable.");
    }
}
