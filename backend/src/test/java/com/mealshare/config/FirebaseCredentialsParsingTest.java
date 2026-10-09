package com.mealshare.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class FirebaseCredentialsParsingTest {

    @Test
    public void testCleanLocalCredentialsLoad() throws IOException {
        Path path = Path.of("mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        if (!Files.exists(path)) {
            path = Path.of("backend", "mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        }
        assertTrue(Files.exists(path), "Local credentials file must exist for testing");

        String rawJson = Files.readString(path);
        assertNotNull(rawJson);
        assertFalse(rawJson.isBlank());

        // Test normal load
        try (InputStream is = new ByteArrayInputStream(rawJson.getBytes(StandardCharsets.UTF_8))) {
            GoogleCredentials creds = GoogleCredentials.fromStream(is);
            assertNotNull(creds);
        }
    }

    @Test
    public void testQuotedJsonStripping() {
        String json = "{\"type\": \"service_account\", \"project_id\": \"test\"}";
        String singleQuoted = "'" + json + "'";
        String doubleQuoted = "\"" + json.replace("\"", "\\\"") + "\"";

        // If wrapped in single quotes, normal parser would fail
        assertThrows(IOException.class, () -> {
            try (InputStream is = new ByteArrayInputStream(singleQuoted.getBytes(StandardCharsets.UTF_8))) {
                GoogleCredentials.fromStream(is);
            }
        });

        // But after stripping outer quotes:
        String stripped = stripOuterQuotes(singleQuoted);
        assertEquals(json, stripped);
    }

    @Test
    public void testBase64Decoding() throws IOException {
        Path path = Path.of("mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        if (!Files.exists(path)) {
            path = Path.of("backend", "mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        }
        String rawJson = Files.readString(path);
        String base64 = Base64.getEncoder().encodeToString(rawJson.getBytes(StandardCharsets.UTF_8));

        // Direct stream on base64 fails
        assertThrows(IOException.class, () -> {
            try (InputStream is = new ByteArrayInputStream(base64.getBytes(StandardCharsets.UTF_8))) {
                GoogleCredentials.fromStream(is);
            }
        });

        // Normalized resolves it cleanly
        String normalized = normalizeCredentialsJson(base64);
        try (InputStream is = new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8))) {
            GoogleCredentials creds = GoogleCredentials.fromStream(is);
            assertNotNull(creds);
        }
    }

    @Test
    public void testEscapedNewlinesInPrivateKey() throws IOException {
        Path path = Path.of("mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        if (!Files.exists(path)) {
            path = Path.of("backend", "mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        }
        String rawJson = Files.readString(path);
        // If someone pasted the JSON into an env var where \\n got double-escaped to \\\\n:
        String doubleEscaped = rawJson.replace("\\n", "\\\\n");

        String normalized = normalizeCredentialsJson(doubleEscaped);
        try (InputStream is = new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8))) {
            GoogleCredentials creds = GoogleCredentials.fromStream(is);
            assertNotNull(creds);
        }
    }

    public static String stripOuterQuotes(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if ((trimmed.startsWith("'") && trimmed.endsWith("'")) ||
            (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    public static String normalizeCredentialsJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String val = raw.trim();

        // 1. Strip outer single or double quotes
        val = stripOuterQuotes(val);

        // 2. Check if Base64 encoded
        if (!val.startsWith("{") && !val.startsWith("[")) {
            try {
                byte[] decoded = Base64.getDecoder().decode(val);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8).trim();
                if (decodedStr.startsWith("{")) {
                    val = decodedStr;
                }
            } catch (IllegalArgumentException ignored) {
                // Not valid base64, proceed as raw
            }
        }

        // 3. Fix double-escaped newlines in private key if present
        if (val.contains("\\\\n")) {
            val = val.replace("\\\\n", "\\n");
        }

        return val;
    }
}
