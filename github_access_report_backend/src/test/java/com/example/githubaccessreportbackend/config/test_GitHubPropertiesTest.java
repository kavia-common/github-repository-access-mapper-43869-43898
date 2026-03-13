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
}
