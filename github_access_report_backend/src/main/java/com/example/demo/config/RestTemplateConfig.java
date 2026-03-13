package com.example.githubaccessreportbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Configures a RestTemplate bean pre-loaded with the GitHub
 * Personal Access Token authorization header.
 */
@Configuration
public class RestTemplateConfig {

    private final GitHubProperties gitHubProperties;

    public RestTemplateConfig(GitHubProperties gitHubProperties) {
        this.gitHubProperties = gitHubProperties;
    }

    // PUBLIC_INTERFACE
    /**
     * Creates a RestTemplate with an interceptor that adds the
     * Authorization header and GitHub API version header to every request.
     *
     * @return pre-configured RestTemplate
     */
    @Bean
    public RestTemplate gitHubRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            String token = gitHubProperties.getToken();
            if (token != null && !token.isBlank()) {
                request.getHeaders().set("Authorization", "Bearer " + token);
            }
            request.getHeaders().set("Accept", "application/vnd.github+json");
            request.getHeaders().set("X-GitHub-Api-Version", "2022-11-28");
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(List.of(authInterceptor));
        return restTemplate;
    }
}
