package com.example.githubaccessreportbackend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link GitHubProperties}.
 * Verifies default values and setter/getter behavior for configuration properties.
 */
@DisplayName("GitHubProperties Tests")
class test_GitHubPropertiesTest {

    private GitHubProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GitHubProperties();
    }

    @Test
    @DisplayName("Should have default API base URL")
    void shouldHaveDefaultApiBaseUrl() {
        assertEquals("https://api.github.com", properties.getApiBaseUrl());
    }

    @Test
    @DisplayName("Should have default page size of 100")
    void shouldHaveDefaultPageSize() {
        assertEquals(100, properties.getPageSize());
    }

    @Test
    @DisplayName("Should have default max concurrency of 10")
    void shouldHaveDefaultMaxConcurrency() {
        assertEquals(10, properties.getMaxConcurrency());
    }

    @Test
    @DisplayName("Should have null token by default")
    void shouldHaveNullTokenByDefault() {
        assertNull(properties.getToken());
    }

    @Test
    @DisplayName("Should have null org by default")
    void shouldHaveNullOrgByDefault() {
        assertNull(properties.getOrg());
    }

    @Test
    @DisplayName("Should set and get token")
    void shouldSetAndGetToken() {
        properties.setToken("ghp_test123");
        assertEquals("ghp_test123", properties.getToken());
    }

    @Test
    @DisplayName("Should set and get org")
    void shouldSetAndGetOrg() {
        properties.setOrg("my-organization");
        assertEquals("my-organization", properties.getOrg());
    }

    @Test
    @DisplayName("Should set and get custom API base URL")
    void shouldSetAndGetApiBaseUrl() {
        properties.setApiBaseUrl("https://github.enterprise.com/api/v3");
        assertEquals("https://github.enterprise.com/api/v3", properties.getApiBaseUrl());
    }

    @Test
    @DisplayName("Should set and get custom page size")
    void shouldSetAndGetPageSize() {
        properties.setPageSize(50);
        assertEquals(50, properties.getPageSize());
    }

    @Test
    @DisplayName("Should set and get custom max concurrency")
    void shouldSetAndGetMaxConcurrency() {
        properties.setMaxConcurrency(20);
        assertEquals(20, properties.getMaxConcurrency());
    }

    // --- sanitizeOrgValue tests ---

    @Test
    @DisplayName("sanitizeOrgValue should return plain org name unchanged")
    void sanitizeOrgValuePlainName() {
        assertEquals("myorg", GitHubProperties.sanitizeOrgValue("myorg"));
    }

    @Test
    @DisplayName("sanitizeOrgValue should extract org from https://github.com/myorg")
    void sanitizeOrgValueHttpsUrl() {
        assertEquals("myorg", GitHubProperties.sanitizeOrgValue("https://github.com/myorg"));
    }

    @Test
    @DisplayName("sanitizeOrgValue should extract org from https://github.com/myorg/")
    void sanitizeOrgValueHttpsUrlTrailingSlash() {
        assertEquals("myorg", GitHubProperties.sanitizeOrgValue("https://github.com/myorg/"));
    }

    @Test
    @DisplayName("sanitizeOrgValue should extract org from http://github.com/myorg")
    void sanitizeOrgValueHttpUrl() {
        assertEquals("myorg", GitHubProperties.sanitizeOrgValue("http://github.com/myorg"));
    }

    @Test
    @DisplayName("sanitizeOrgValue should extract org from github.com/myorg")
    void sanitizeOrgValueNoScheme() {
        assertEquals("myorg", GitHubProperties.sanitizeOrgValue("github.com/myorg"));
    }

    @Test
    @DisplayName("sanitizeOrgValue should handle URL with extra path segments")
    void sanitizeOrgValueExtraPath() {
        assertEquals("myorg", GitHubProperties.sanitizeOrgValue("https://github.com/myorg/some-repo"));
    }

    @Test
    @DisplayName("sanitizeOrgValue should return null for null input")
    void sanitizeOrgValueNull() {
        assertNull(GitHubProperties.sanitizeOrgValue(null));
    }

    @Test
    @DisplayName("sanitizeOrgValue should return blank for blank input")
    void sanitizeOrgValueBlank() {
        assertEquals("", GitHubProperties.sanitizeOrgValue("  "));
    }

    @Test
    @DisplayName("getOrg should sanitize URL values automatically")
    void getOrgShouldSanitize() {
        properties.setOrg("https://github.com/theakshatmishra");
        assertEquals("theakshatmishra", properties.getOrg());
    }

    @Test
    @DisplayName("getOrg should return plain org name unchanged")
    void getOrgPlainName() {
        properties.setOrg("my-organization");
        assertEquals("my-organization", properties.getOrg());
    }
}
