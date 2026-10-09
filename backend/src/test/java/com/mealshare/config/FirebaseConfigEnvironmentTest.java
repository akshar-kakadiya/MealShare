package com.mealshare.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class FirebaseConfigEnvironmentTest {

    @Test
    public void testNormalizeCredentialsJson_RawJson() {
        String json = "{\"type\":\"service_account\",\"project_id\":\"mealshare-100\"}";
        String normalized = FirebaseConfig.normalizeCredentialsJson(json);
        assertEquals(json, normalized);
    }

    @Test
    public void testNormalizeCredentialsJson_SingleAndDoubleQuoted() {
        String json = "{\"type\":\"service_account\",\"project_id\":\"mealshare-100\"}";
        String singleQuoted = "'" + json + "'";
        String doubleQuoted = "\"" + json.replace("\"", "\\\"") + "\"";

        assertEquals(json, FirebaseConfig.normalizeCredentialsJson(singleQuoted));
        assertEquals(json.replace("\"", "\\\""), FirebaseConfig.normalizeCredentialsJson(doubleQuoted));
    }

    @Test
    public void testNormalizeCredentialsJson_Base64Encoded() {
        String json = "{\"type\":\"service_account\",\"project_id\":\"mealshare-100\"}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes());

        String normalized = FirebaseConfig.normalizeCredentialsJson(base64);
        assertEquals(json, normalized);
    }

    @Test
    public void testNormalizeCredentialsJson_DoubleEscapedNewlines() {
        String jsonWithEscapedNewlines = "{\"private_key\":\"-----BEGIN KEY-----\\\\nMIIEvg\\\\n-----END KEY-----\\\\n\"}";
        String normalized = FirebaseConfig.normalizeCredentialsJson(jsonWithEscapedNewlines);

        assertNotNull(normalized);
        assertFalse(normalized.contains("\\\\n"));
        assertTrue(normalized.contains("\\n"));
    }

    @Test
    public void testFirebaseConfig_MissingCredentialsInProdFailsSafely() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        FirebaseConfig config = new FirebaseConfig(env);
        // Should execute initialize without throwing uncaught exceptions to prevent crashing container
        assertDoesNotThrow(config::initialize);
    }

    @Test
    public void testFirebaseConfig_ResolvesFromEnvironmentJson() throws IOException {
        Path path = Path.of("mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        if (!Files.exists(path)) {
            path = Path.of("backend", "mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        }
        if (Files.exists(path)) {
            String rawJson = Files.readString(path);
            MockEnvironment env = new MockEnvironment();
            env.setProperty("FIREBASE_CREDENTIALS_JSON", rawJson);
            env.setActiveProfiles("test");

            FirebaseConfig config = new FirebaseConfig(env);
            assertDoesNotThrow(config::initialize);
        }
    }
}
