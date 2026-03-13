package com.kavia.githubaccess;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic test to verify the Spring application context loads correctly.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "github.token=test-token",
        "github.org=test-org"
})
class GithubAccessReportApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads without errors
    }
}
