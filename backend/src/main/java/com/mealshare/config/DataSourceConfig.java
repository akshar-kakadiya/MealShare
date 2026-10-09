package com.mealshare.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Production and runtime DataSource configuration.
 * Ensures the effective JDBC URL is sanitized (stripping any conflicting embedded user/password parameters)
 * and guarantees that SPRING_DATASOURCE_USERNAME is authoritative without being shadowed by hardcoded defaults.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties, Environment environment) {
        String rawUrl = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        // 1. Explicitly retrieve environment variable if present (highest priority)
        String envUser = environment.getProperty("SPRING_DATASOURCE_USERNAME");
        if (envUser != null && !envUser.isBlank()) {
            username = envUser.trim();
        }

        // 2. Sanitize JDBC URL: strip embedded query params that could override Hikari properties
        String sanitizedUrl = sanitizeJdbcUrl(rawUrl);

        // 3. Diagnostics and safe logging (strictly no password exposure)
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        log.info("Configuring DataSource: Target URL = {}, Effective Username = {}", sanitizedUrl, username);

        if (isProd && sanitizedUrl != null && sanitizedUrl.contains("pooler.supabase.com") && "postgres".equalsIgnoreCase(username)) {
            log.warn("Detected Supabase connection pooler with username 'postgres'. " +
                     "Supabase Connection Pooler typically requires 'postgres.<project-ref>' (e.g. postgres.ztbjtkonfpdlqznjkvgj). " +
                     "Verify SPRING_DATASOURCE_USERNAME in your cloud environment variables.");
        }

        // 4. Initialize HikariDataSource
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        if (sanitizedUrl != null) {
            dataSource.setJdbcUrl(sanitizedUrl);
        }
        if (username != null) {
            dataSource.setUsername(username);
        }
        if (password != null) {
            dataSource.setPassword(password);
        }

        return dataSource;
    }

    /**
     * Normalizes postgresql:// URI format to jdbc:postgresql:// and strips conflicting query params.
     */
    public static String sanitizeJdbcUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String url = rawUrl.trim();
        if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
            url = "jdbc:" + url;
        }
        // Remove user and password query parameters if present (e.g. ?user=postgres or &user=postgres)
        url = url.replaceAll("([?&])user=[^&]*(&?)", "$1")
                 .replaceAll("([?&])password=[^&]*(&?)", "$1")
                 .replaceAll("\\?&", "?")
                 .replaceAll("\\?$", "");
        if (url.endsWith("&")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
