package com.mealshare.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Component;

@Component
public class FirebaseTokenValidator {

    public FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase Admin SDK is not initialized. Token verification is unavailable.");
        }
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
}
