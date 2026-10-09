package com.mealshare.config;

import org.junit.jupiter.api.Test;
import org.postgresql.Driver;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class HikariJdbcOverrideTest {

    @Test
    public void testSanitizeJdbcUrlRemovesEmbeddedUser() throws SQLException {
        String urlWithUser = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?user=postgres&sslmode=require";
        String sanitized = DataSourceConfig.sanitizeJdbcUrl(urlWithUser);

        assertEquals("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require", sanitized);

        Driver driver = new Driver();
        Properties props = new Properties();
        props.setProperty("user", "postgres.ztbjtkonfpdlqznjkvgj");
        Properties parsed = driver.parseURL(sanitized, props);

        assertEquals("postgres.ztbjtkonfpdlqznjkvgj", parsed.getProperty("user"));
    }

    @Test
    public void testConvertPostgresUriFormat() {
        String uri = "postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String sanitized = DataSourceConfig.sanitizeJdbcUrl(uri);
        assertEquals("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require", sanitized);
    }

    @Test
    public void testCleanUrlUnaffected() {
        String cleanUrl = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
        assertEquals(cleanUrl, DataSourceConfig.sanitizeJdbcUrl(cleanUrl));
    }
}
