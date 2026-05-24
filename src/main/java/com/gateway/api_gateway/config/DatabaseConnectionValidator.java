package com.gateway.api_gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
@Profile("!test")
public class DatabaseConnectionValidator {

    private final DataSource dataSource;

    @Value("${DB_URL:}")
    private String databaseUrl;

    public DatabaseConnectionValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void validateConnection() {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("Database connection is required. Set DB_URL, DB_USER, and DB_PWD; H2 fallback is disabled.");
        }

        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(2)) {
                throw new IllegalStateException("Database connection is required. The configured datasource is not valid.");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Database connection is required. The configured datasource could not be reached.", ex);
        }
    }
}