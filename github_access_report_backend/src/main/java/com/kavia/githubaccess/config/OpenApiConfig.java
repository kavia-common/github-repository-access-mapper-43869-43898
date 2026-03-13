package com.kavia.githubaccess.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger documentation configuration for the
 * GitHub Access Report API.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:3001}")
    private int serverPort;

    // PUBLIC_INTERFACE
    /**
     * Creates the OpenAPI metadata bean used by springdoc to
     * render Swagger UI and produce /api-docs.
     *
     * @return configured OpenAPI object
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GitHub Access Report API")
                        .version("1.0.0")
                        .description("Service that connects to GitHub and generates a report "
                                + "showing which users have access to which repositories "
                                + "within a given organization. Supports 100+ repos and 1000+ users.")
                        .contact(new Contact()
                                .name("Kavia")
                                .url("https://kavia.ai")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")));
    }
}
