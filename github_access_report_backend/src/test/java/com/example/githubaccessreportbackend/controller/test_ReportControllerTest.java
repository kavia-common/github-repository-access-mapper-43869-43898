package com.example.githubaccessreportbackend.controller;

import com.example.githubaccessreportbackend.config.GitHubProperties;
import com.example.githubaccessreportbackend.dto.AccessReport;
import com.example.githubaccessreportbackend.dto.UserAccess;
import com.example.githubaccessreportbackend.exception.GitHubApiException;
import com.example.githubaccessreportbackend.exception.GlobalExceptionHandler;
import com.example.githubaccessreportbackend.service.GitHubAccessReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc tests for {@link ReportController}.
 * Uses MockMvc to test HTTP request/response handling with mocked service layer.
 */
@WebMvcTest(ReportController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ReportController Tests")
class test_ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubAccessReportService reportService;

    @MockBean
    private GitHubProperties gitHubProperties;

    private AccessReport sampleReport;

    @BeforeEach
    void setUp() {
        sampleReport = new AccessReport();
        sampleReport.setOrganization("testorg");
        sampleReport.setGeneratedAt("2025-01-15T10:30:00.123Z");
        sampleReport.setTotalRepositories(2);
        sampleReport.setTotalUsers(1);

        UserAccess userAccess = new UserAccess("alice");
        userAccess.addRepository("testorg/repo1", "admin");
        userAccess.addRepository("testorg/repo2", "read");
        sampleReport.setUsers(List.of(userAccess));
    }

    @Nested
    @DisplayName("GET /api/report - success scenarios")
    class SuccessTests {

        @Test
        @DisplayName("Should return 200 with report when org query param is provided")
        void shouldReturnReportWithOrgParam() throws Exception {
            // Arrange
            when(reportService.generateReport("testorg")).thenReturn(sampleReport);

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "testorg")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.organization", is("testorg")))
                    .andExpect(jsonPath("$.generatedAt", notNullValue()))
                    .andExpect(jsonPath("$.totalRepositories", is(2)))
                    .andExpect(jsonPath("$.totalUsers", is(1)))
                    .andExpect(jsonPath("$.users", hasSize(1)))
                    .andExpect(jsonPath("$.users[0].username", is("alice")))
                    .andExpect(jsonPath("$.users[0].repositories", hasSize(2)))
                    .andExpect(jsonPath("$.users[0].repositories[0].repository", is("testorg/repo1")))
                    .andExpect(jsonPath("$.users[0].repositories[0].role", is("admin")));

            verify(reportService).generateReport("testorg");
        }

        @Test
        @DisplayName("Should use default org from properties when no query param provided")
        void shouldUseDefaultOrgFromProperties() throws Exception {
            // Arrange
            when(gitHubProperties.getOrg()).thenReturn("default-org");
            when(reportService.generateReport("default-org")).thenReturn(sampleReport);

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.organization", notNullValue()));

            verify(reportService).generateReport("default-org");
        }

        @Test
        @DisplayName("Should trim whitespace from org query parameter")
        void shouldTrimOrgParameter() throws Exception {
            // Arrange
            when(reportService.generateReport("trimmedorg")).thenReturn(sampleReport);

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "  trimmedorg  ")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(reportService).generateReport("trimmedorg");
        }

        @Test
        @DisplayName("Should return report with empty users list")
        void shouldReturnEmptyReport() throws Exception {
            // Arrange
            AccessReport emptyReport = new AccessReport();
            emptyReport.setOrganization("emptyorg");
            emptyReport.setTotalRepositories(0);
            emptyReport.setTotalUsers(0);
            emptyReport.setUsers(List.of());

            when(reportService.generateReport("emptyorg")).thenReturn(emptyReport);

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "emptyorg")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRepositories", is(0)))
                    .andExpect(jsonPath("$.totalUsers", is(0)))
                    .andExpect(jsonPath("$.users", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/report - error scenarios")
    class ErrorTests {

        @Test
        @DisplayName("Should return 400 when no org param and no default org configured")
        void shouldReturn400WhenNoOrg() throws Exception {
            // Arrange
            when(gitHubProperties.getOrg()).thenReturn(null);

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Bad Request")))
                    .andExpect(jsonPath("$.message", notNullValue()));

            verify(reportService, never()).generateReport(anyString());
        }

        @Test
        @DisplayName("Should return 400 when org param is blank")
        void shouldReturn400WhenOrgIsBlank() throws Exception {
            // Arrange
            when(gitHubProperties.getOrg()).thenReturn("");

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "   ")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(reportService, never()).generateReport(anyString());
        }

        @Test
        @DisplayName("Should return 403 when GitHub API returns 401 unauthorized")
        void shouldReturn403OnUnauthorized() throws Exception {
            // Arrange
            when(reportService.generateReport("testorg"))
                    .thenThrow(new GitHubApiException("Authentication failed", 401));

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "testorg")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")))
                    .andExpect(jsonPath("$.message", is("Authentication failed")));
        }

        @Test
        @DisplayName("Should return 404 when organization is not found")
        void shouldReturn404OnOrgNotFound() throws Exception {
            // Arrange
            when(reportService.generateReport("nonexistent"))
                    .thenThrow(new GitHubApiException("Resource not found", 404));

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "nonexistent")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }

        @Test
        @DisplayName("Should return 429 when rate limit is exceeded")
        void shouldReturn429OnRateLimit() throws Exception {
            // Arrange
            when(reportService.generateReport("testorg"))
                    .thenThrow(new GitHubApiException("Rate limit exceeded", 429));

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "testorg")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.status", is(429)))
                    .andExpect(jsonPath("$.error", is("Too Many Requests")));
        }

        @Test
        @DisplayName("Should return 502 for unexpected GitHub API errors")
        void shouldReturn502OnGitHubServerError() throws Exception {
            // Arrange
            when(reportService.generateReport("testorg"))
                    .thenThrow(new GitHubApiException("GitHub server error", 500));

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "testorg")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status", is(502)))
                    .andExpect(jsonPath("$.error", is("Bad Gateway")));
        }

        @Test
        @DisplayName("Should return 500 for unexpected exceptions")
        void shouldReturn500OnUnexpectedException() throws Exception {
            // Arrange
            when(reportService.generateReport("testorg"))
                    .thenThrow(new RuntimeException("Something went wrong"));

            // Act & Assert
            mockMvc.perform(get("/api/report")
                            .param("org", "testorg")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status", is(500)))
                    .andExpect(jsonPath("$.error", is("Internal Server Error")));
        }
    }
}
