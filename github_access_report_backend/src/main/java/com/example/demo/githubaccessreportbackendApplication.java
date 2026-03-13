package com.example.githubaccessreportbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main entry point for the GitHub Access Report Backend application.
 * <p>
 * This Spring Boot application connects to the GitHub API, retrieves all
 * repositories within a given organization, determines which users have
 * access to each repository (including permission levels), and exposes
 * a REST API endpoint returning a structured JSON access report.
 * <p>
 * JPA and DataSource auto-configurations are excluded because this service
 * does not use a database — all data is fetched live from the GitHub API.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties
public class githubaccessreportbackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(githubaccessreportbackendApplication.class, args);
    }
}
