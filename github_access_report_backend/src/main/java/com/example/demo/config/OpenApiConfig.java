package com.example.githubaccessreportbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration providing app-level metadata
 * for the auto-generated API documentation.
 */
@Configuration
public class OpenApiConfig {

    // PUBLIC_INTERFACE
    /**
     * Builds the OpenAPI metadata bean used by springdoc to render Swagger UI.
     *
     * @return configured OpenAPI instance with title, description, version and contact
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GitHub Repository Access Report API")
                        .description(
                                "A service that connects to GitHub, retrieves all repositories " +
                                "within a given organization, determines which users have access " +
                                "to each repository (including their permission level), and exposes " +
                                "an API endpoint returning a structured JSON access report.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("GitHub Access Report Team")));
    }
}
