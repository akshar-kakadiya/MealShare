package com.mealshare.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProdDatasourceConfigTest {

    private StandardEnvironment createEnvironment(Map<String, Object> envVars) throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("prod");

        if (envVars != null && !envVars.isEmpty()) {
            env.getPropertySources().addFirst(new MapPropertySource("renderEnvVars", envVars));
        }

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> appYaml = loader.load("application.yml", new ClassPathResource("application.yml"));
        for (PropertySource<?> ps : appYaml) {
            env.getPropertySources().addLast(ps);
        }

        List<PropertySource<?>> prodYaml = loader.load("application-prod.yml", new ClassPathResource("application-prod.yml"));
        for (PropertySource<?> ps : prodYaml) {
            env.getPropertySources().addLast(ps);
        }

        return env;
    }

    @Test
    public void testProdProfileWithAllEnvVars() throws IOException {
        Map<String, Object> envVars = Map.of(
                "SPRING_PROFILES_ACTIVE", "prod",
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require",
                "SPRING_DATASOURCE_USERNAME", "postgres.ztbjtkonfpdlqznjkvgj",
                "SPRING_DATASOURCE_PASSWORD", "secret_pass_123"
        );
        StandardEnvironment env = createEnvironment(envVars);

        String username = env.getProperty("spring.datasource.username");
        String url = env.getProperty("spring.datasource.url");

        System.out.println("[Case 1] USERNAME: " + username);
        assertEquals("postgres.ztbjtkonfpdlqznjkvgj", username);
        assertEquals("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require", url);
    }

    @Test
    public void testProdProfileWithoutUsernameEnvVar() throws IOException {
        // What if SPRING_DATASOURCE_USERNAME is NOT set or missing in Render environment?
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("SPRING_PROFILES_ACTIVE", "prod");
        envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require");
        envVars.put("SPRING_DATASOURCE_PASSWORD", "secret_pass_123");
        // No SPRING_DATASOURCE_USERNAME set!

        StandardEnvironment env = createEnvironment(envVars);

        String username = env.getProperty("spring.datasource.username");
        System.out.println("[Case 2 Missing Username] USERNAME: " + username);
        // If SPRING_DATASOURCE_USERNAME is missing, what does it resolve to?
        assertEquals("postgres", username);
    }
}
