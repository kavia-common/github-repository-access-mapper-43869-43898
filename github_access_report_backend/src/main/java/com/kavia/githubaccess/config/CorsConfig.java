package com.kavia.githubaccess.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration to allow cross-origin requests from
 * configured frontend origins.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:Content-Type,Authorization,X-Requested-With}")
    private String allowedHeaders;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    // PUBLIC_INTERFACE
    /**
     * Creates a WebMvcConfigurer bean that applies CORS settings
     * to all endpoints based on environment configuration.
     *
     * @return WebMvcConfigurer with CORS mappings
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins.split(","))
                        .allowedMethods(allowedMethods.split(","))
                        .allowedHeaders(allowedHeaders.split(","))
                        .maxAge(maxAge);
            }
        };
    }
}
