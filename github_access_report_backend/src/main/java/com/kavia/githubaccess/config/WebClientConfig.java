package com.kavia.githubaccess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for the WebClient used to communicate with the GitHub API.
 * Sets up authentication headers, content type, and buffer limits.
 */
@Configuration
public class WebClientConfig {

    // PUBLIC_INTERFACE
    /**
     * Creates a pre-configured WebClient for GitHub API calls.
     *
     * @param properties GitHub configuration properties
     * @return configured WebClient instance
     */
    @Bean
    public WebClient gitHubWebClient(GitHubProperties properties) {
        // Increase buffer size for large API responses (16 MB)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build();

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .exchangeStrategies(strategies);

        // Add Bearer token if available
        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken());
        }

        return builder.build();
    }
}
