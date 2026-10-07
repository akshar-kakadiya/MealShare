package com.mealshare.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.credentials.json:}")
    private String credentialsJson;

    @PostConstruct
    public void initialize() {
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            try {
                java.io.InputStream stream = new java.io.ByteArrayInputStream(credentialsJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase Admin SDK initialized successfully from environment variable JSON.");
                }
                return;
            } catch (IOException e) {
                log.error("Failed to initialize Firebase Admin SDK from environment variable JSON: {}", e.getMessage());
            }
        }

        if (credentialsPath == null || credentialsPath.isBlank() || !new java.io.File(credentialsPath).exists()) {
            // Also check standard cloud secret mounts like /etc/secrets/
            java.io.File renderSecret = new java.io.File("/etc/secrets/mealshare-100-firebase-adminsdk.json");
            java.io.File renderSecret2 = new java.io.File("/etc/secrets/firebase-adminsdk.json");
            java.io.File directFile = new java.io.File("mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
            java.io.File backendFile = new java.io.File("backend/mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json");
            if (renderSecret.exists()) {
                credentialsPath = renderSecret.getAbsolutePath();
            } else if (renderSecret2.exists()) {
                credentialsPath = renderSecret2.getAbsolutePath();
            } else if (directFile.exists()) {
                credentialsPath = directFile.getAbsolutePath();
            } else if (backendFile.exists()) {
                credentialsPath = backendFile.getAbsolutePath();
            }
        }

        if (credentialsPath == null || credentialsPath.isBlank() || !new java.io.File(credentialsPath).exists()) {
            log.warn("Firebase credentials path not configured or file not found. Firebase Authentication will not verify tokens.");
            log.warn("Set 'firebase.credentials.path', FIREBASE_CREDENTIALS_PATH, or FIREBASE_CREDENTIALS_JSON environment variable.");
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully from: {}", credentialsPath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
            log.error("Ensure the file exists at: {}", credentialsPath);
        }
    }
}
