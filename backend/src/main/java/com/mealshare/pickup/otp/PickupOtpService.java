package com.mealshare.pickup.otp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PickupOtpService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    private final int otpLength;
    private final int expiryMinutes;
    private final int maxAttempts;

    public record CachedOtp(String otp, Instant expiresAt) {}
    private final ConcurrentHashMap<UUID, CachedOtp> otpCache = new ConcurrentHashMap<>();

    public PickupOtpService(
            PasswordEncoder passwordEncoder,
            @Value("${mealshare.pickup.otp.length:6}") int otpLength,
            @Value("${mealshare.pickup.otp.expiry-minutes:10}") int expiryMinutes,
            @Value("${mealshare.pickup.otp.max-attempts:5}") int maxAttempts) {
        this.passwordEncoder = passwordEncoder;
        this.otpLength = otpLength;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Stores ephemeral plaintext OTP in memory for secure retrieval by authorized donor only.
     * Never persisted to database.
     */
    public void storeCachedOtp(UUID pickupId, String rawOtp, Instant expiresAt) {
        if (pickupId != null && rawOtp != null && expiresAt != null) {
            otpCache.put(pickupId, new CachedOtp(rawOtp, expiresAt));
        }
    }

    /**
     * Retrieves ephemeral plaintext OTP for authorized donor if active and unexpired.
     */
    public Optional<String> getCachedOtp(UUID pickupId) {
        if (pickupId == null) {
            return Optional.empty();
        }
        CachedOtp entry = otpCache.get(pickupId);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            otpCache.remove(pickupId);
            return Optional.empty();
        }
        return Optional.of(entry.otp());
    }

    /**
     * Invalidates and evicts the OTP from memory upon verification, completion, or cancellation.
     */
    public void invalidateOtp(UUID pickupId) {
        if (pickupId != null) {
            otpCache.remove(pickupId);
        }
    }

    /**
     * Clears all cached OTPs (used in test tearDown).
     */
    public void clearCache() {
        otpCache.clear();
    }

    /**
     * Generates a cryptographically secure numeric OTP using SecureRandom.
     */
    public String generateSecureOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        int code = min + secureRandom.nextInt(max - min + 1);
        return String.valueOf(code);
    }

    /**
     * Hashes the raw OTP using BCryptPasswordEncoder.
     */
    public String hashOtp(String rawOtp) {
        if (rawOtp == null || rawOtp.isBlank()) {
            throw new IllegalArgumentException("Raw OTP cannot be null or blank.");
        }
        return passwordEncoder.encode(rawOtp);
    }

    /**
     * Verifies raw OTP against the stored one-way hash.
     */
    public boolean verifyOtp(String rawOtp, String storedHash) {
        if (rawOtp == null || storedHash == null) {
            return false;
        }
        return passwordEncoder.matches(rawOtp, storedHash);
    }

    /**
     * Calculates the expiration timestamp for a newly generated OTP.
     */
    public Instant calculateExpiry() {
        return Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);
    }

    /**
     * Calculates the expiration timestamp for a newly generated OTP tied to the scheduled pickup time.
     * Ensures OTP does not expire prematurely before scheduledAt, with a buffer window (at least expiryMinutes, min 30m),
     * and strictly capped by pickupAvailableUntil and consumeBefore.
     */
    public Instant calculateExpiry(Instant scheduledAt, Instant pickupAvailableUntil, Instant consumeBefore) {
        int bufferMinutes = Math.max(expiryMinutes, 30);
        Instant base = (scheduledAt != null && scheduledAt.isAfter(Instant.now()))
                ? scheduledAt.plus(bufferMinutes, ChronoUnit.MINUTES)
                : Instant.now().plus(bufferMinutes, ChronoUnit.MINUTES);

        Instant cap = null;
        if (pickupAvailableUntil != null && consumeBefore != null) {
            cap = pickupAvailableUntil.isBefore(consumeBefore) ? pickupAvailableUntil : consumeBefore;
        } else if (pickupAvailableUntil != null) {
            cap = pickupAvailableUntil;
        } else if (consumeBefore != null) {
            cap = consumeBefore;
        }

        if (cap != null && base.isAfter(cap)) {
            return cap;
        }
        return base;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getExpiryMinutes() {
        return expiryMinutes;
    }

    public int getOtpLength() {
        return otpLength;
    }
}
