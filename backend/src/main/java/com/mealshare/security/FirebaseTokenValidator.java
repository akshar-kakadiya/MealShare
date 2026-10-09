package com.mealshare.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.mealshare.config.FirebaseConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class FirebaseTokenValidator {

    private final FirebaseConfig firebaseConfig;

    public FirebaseTokenValidator(FirebaseConfig firebaseConfig) {
        this.firebaseConfig = firebaseConfig;
    }

    public FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException {
        if (!firebaseConfig.isInitialized()) {
            firebaseConfig.awaitInitialization(Duration.ofSeconds(5));
        }

        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase Admin SDK is not initialized. Token verification is unavailable.");
        }
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
}
