package com.example.githubaccessreportbackend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc tests for {@link HelloController}.
 * Tests basic health, welcome, info, and docs endpoints.
 */
@WebMvcTest(HelloController.class)
@DisplayName("HelloController Tests")
class test_HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / should return welcome message")
    void shouldReturnWelcomeMessage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello, Spring Boot!")))
                .andExpect(content().string(containsString("githubaccessreportbackend")));
    }

    @Test
    @DisplayName("GET /health should return OK")
    void shouldReturnHealthOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    @DisplayName("GET /api/info should return application info")
    void shouldReturnAppInfo() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Spring Boot Application")))
                .andExpect(content().string(containsString("githubaccessreportbackend")));
    }

    @Test
    @DisplayName("GET /docs should redirect to swagger-ui")
    void shouldRedirectToSwaggerUi() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection());
    }
}
