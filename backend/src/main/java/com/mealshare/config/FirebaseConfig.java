package com.mealshare.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final Environment environment;

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.credentials.json:}")
    private String credentialsJson;

    @Value("${firebase.database.url:https://mealshare-100-default-rtdb.firebaseio.com}")
    private String databaseUrl;

    private final CountDownLatch initLatch = new CountDownLatch(1);
    private final java.util.concurrent.atomic.AtomicBoolean isInitializing = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.ExecutorService initExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "firebase-init-thread");
        t.setDaemon(true);
        return t;
    });

    public FirebaseConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void startAsyncInitialization() {
        initExecutor.submit(this::initialize);
    }

    public void initialize() {
        if (!isInitializing.compareAndSet(false, true)) {
            // Already initializing or initialized, wait for completion if needed
            awaitInitialization(java.time.Duration.ofSeconds(10));
            return;
        }

        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase Admin SDK is already initialized.");
                return;
            }

            boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

            String dbUrl = resolveDatabaseUrl();

            // 1. Try to load from environment JSON (preferred in cloud deployment / Render)
            String rawJson = resolveCredentialsJson();
            if (rawJson != null && !rawJson.isBlank()) {
                String normalizedJson = normalizeCredentialsJson(rawJson);
                if (normalizedJson != null && !normalizedJson.isBlank()) {
                    try (InputStream stream = new ByteArrayInputStream(normalizedJson.getBytes(StandardCharsets.UTF_8))) {
                        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(stream))
                                .setConnectTimeout(10000)
                                .setReadTimeout(10000);

                        if (dbUrl != null && !dbUrl.isBlank()) {
                            optionsBuilder.setDatabaseUrl(dbUrl);
                        }

                        FirebaseOptions options = optionsBuilder.build();

                        if (FirebaseApp.getApps().isEmpty()) {
                            FirebaseApp.initializeApp(options);
                        }
                        log.info("Firebase Admin SDK initialized successfully from environment variable JSON (project: {}, rtdb: {}).",
                                extractProjectId(normalizedJson), dbUrl);
                        return;
                    } catch (Exception e) {
                        log.error("Failed to initialize Firebase Admin SDK from environment variable JSON: {}", e.getMessage());
                    }
                }
            }

            // 2. Try to load from secret file path
            String path = resolveCredentialsPath();
            if (path != null && !path.isBlank() && new File(path).exists()) {
                try (FileInputStream serviceAccount = new FileInputStream(path)) {
                    FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setConnectTimeout(10000)
                            .setReadTimeout(10000);

                    if (dbUrl != null && !dbUrl.isBlank()) {
                        optionsBuilder.setDatabaseUrl(dbUrl);
                    }

                    FirebaseOptions options = optionsBuilder.build();

                    if (FirebaseApp.getApps().isEmpty()) {
                        FirebaseApp.initializeApp(options);
                    }
                    log.info("Firebase Admin SDK initialized successfully from credentials file: {} (rtdb: {})", path, dbUrl);
                    return;
                } catch (Exception e) {
                    log.error("Failed to initialize Firebase Admin SDK from credentials file {}: {}", path, e.getMessage());
                }
            }

            // 3. Fallback warning / error
            if (isProd) {
                log.error("CRITICAL: Firebase Admin SDK credentials could not be loaded in production ('prod' profile). " +
                          "Protected endpoints will safely reject all token verification requests with HTTP 401 until valid credentials are provided. " +
                          "Verify FIREBASE_CREDENTIALS_JSON or a secret file at /etc/secrets/mealshare-100-firebase-adminsdk.json in Render.");
            } else {
                log.warn("Firebase Admin SDK credentials not configured. Firebase Authentication will not verify tokens in local development.");
                log.warn("Set FIREBASE_CREDENTIALS_JSON, FIREBASE_CREDENTIALS_PATH, or 'firebase.credentials.path' property.");
            }
        } finally {
            initLatch.countDown();
        }
    }

    public boolean awaitInitialization(java.time.Duration timeout) {
        try {
            return initLatch.await(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean isInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }

    private String resolveCredentialsJson() {
        // Highest priority: direct Environment variable lookup
        String val = environment.getProperty("FIREBASE_CREDENTIALS_JSON");
        if (val != null && !val.isBlank()) {
            return val;
        }

        val = System.getenv("FIREBASE_CREDENTIALS_JSON");
        if (val != null && !val.isBlank()) {
            return val;
        }

        // Base64-encoded alternative variable
        val = environment.getProperty("FIREBASE_CREDENTIALS_BASE64");
        if (val != null && !val.isBlank()) {
            return val;
        }

        val = System.getenv("FIREBASE_CREDENTIALS_BASE64");
        if (val != null && !val.isBlank()) {
            return val;
        }

        // Spring property source lookup
        val = environment.getProperty("firebase.credentials.json");
        if (val != null && !val.isBlank()) {
            return val;
        }

        // Injected @Value fallback
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            return credentialsJson;
        }

        return null;
    }

    private String resolveCredentialsPath() {
        String path = environment.getProperty("FIREBASE_CREDENTIALS_PATH");
        if (path != null && !path.isBlank() && new File(path).exists()) {
            return path;
        }

        path = System.getenv("FIREBASE_CREDENTIALS_PATH");
        if (path != null && !path.isBlank() && new File(path).exists()) {
            return path;
        }

        if (credentialsPath != null && !credentialsPath.isBlank() && new File(credentialsPath).exists()) {
            return credentialsPath;
        }

        // Standard cloud mounts on Render
        File renderSecret = new File("/etc/secrets/mealshare-100-firebase-adminsdk.json");
        if (renderSecret.exists()) {
            return renderSecret.getAbsolutePath();
        }

        File renderSecret2 = new File("/etc/secrets/firebase-adminsdk.json");
        if (renderSecret2.exists()) {
            return renderSecret2.getAbsolutePath();
        }

        // Local development files
        File directFile = new File("mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        if (directFile.exists()) {
            return directFile.getAbsolutePath();
        }

        File backendFile = new File("backend/mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
        if (backendFile.exists()) {
            return backendFile.getAbsolutePath();
        }

        return null;
    }

    /**
     * Sanitizes and normalizes Firebase service-account JSON:
     * - Strips outer wrapping quotes ('...' or "...")
     * - Decodes Base64-encoded JSON if applicable
     * - Normalizes double-escaped newlines in private key PEMs
     */
    public static String normalizeCredentialsJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String val = raw.trim();

        // 1. Strip outer single or double quotes
        if ((val.startsWith("'") && val.endsWith("'")) ||
            (val.startsWith("\"") && val.endsWith("\""))) {
            val = val.substring(1, val.length() - 1).trim();
        }

        // 2. Decode Base64 if not already a JSON object
        if (!val.startsWith("{") && !val.startsWith("[")) {
            try {
                byte[] decoded = Base64.getDecoder().decode(val);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8).trim();
                if (decodedStr.startsWith("{")) {
                    val = decodedStr;
                }
            } catch (IllegalArgumentException ignored) {
                // Not base64 encoded, treat as-is
            }
        }

        // 3. Fix double-escaped newlines in private keys
        if (val.contains("\\\\n")) {
            val = val.replace("\\\\n", "\\n");
        }

        return val;
    }

    /**
     * Safely extracts public project_id without logging or exposing private credentials.
     */
    private static String extractProjectId(String json) {
        try {
            int idx = json.indexOf("\"project_id\"");
            if (idx != -1) {
                int colon = json.indexOf(':', idx);
                if (colon != -1) {
                    int quote1 = json.indexOf('"', colon);
                    if (quote1 != -1) {
                        int quote2 = json.indexOf('"', quote1 + 1);
                        if (quote2 != -1) {
                            return json.substring(quote1 + 1, quote2);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "configured";
    }

    public String resolveDatabaseUrl() {
        String url = environment.getProperty("FIREBASE_DATABASE_URL");
        if (url != null && !url.isBlank()) {
            return url.trim();
        }
        url = System.getenv("FIREBASE_DATABASE_URL");
        if (url != null && !url.isBlank()) {
            return url.trim();
        }
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            return databaseUrl.trim();
        }
        return "https://mealshare-100-default-rtdb.firebaseio.com";
    }

    @org.springframework.context.annotation.Bean
    public com.google.firebase.database.FirebaseDatabase firebaseDatabase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            try {
                return com.google.firebase.database.FirebaseDatabase.getInstance();
            } catch (Exception e) {
                log.warn("FirebaseDatabase instance could not be retrieved from default FirebaseApp: {}", e.getMessage());
            }
        }
        return null;
    }
}

