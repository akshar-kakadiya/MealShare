package com.mealshare.pickup.otp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class PickupOtpServiceTest {

    private PickupOtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new PickupOtpService(new BCryptPasswordEncoder(), 6, 10, 5);
    }

    @Test
    void testGenerateSecureOtp_generatesSixDigitNumericCode() {
        for (int i = 0; i < 50; i++) {
            String otp = otpService.generateSecureOtp();
            assertNotNull(otp);
            assertEquals(6, otp.length());
            assertTrue(otp.matches("^[0-9]{6}$"));
            int num = Integer.parseInt(otp);
            assertTrue(num >= 100000 && num <= 999999);
        }
    }

    @Test
    void testHashAndVerifyOtp_validMatchAndInvalidMismatch() {
        String rawOtp = "482915";
        String hash = otpService.hashOtp(rawOtp);

        assertNotNull(hash);
        assertNotEquals(rawOtp, hash);
        assertTrue(otpService.verifyOtp(rawOtp, hash));
        assertFalse(otpService.verifyOtp("000000", hash));
        assertFalse(otpService.verifyOtp(null, hash));
        assertFalse(otpService.verifyOtp(rawOtp, null));
    }

    @Test
    void testCalculateExpiryDefault_returnsNowPlusExpiryMinutes() {
        Instant before = Instant.now().plus(9, ChronoUnit.MINUTES);
        Instant expiry = otpService.calculateExpiry();
        Instant after = Instant.now().plus(11, ChronoUnit.MINUTES);

        assertTrue(expiry.isAfter(before));
        assertTrue(expiry.isBefore(after));
    }

    @Test
    void testCalculateExpiry_tiedToFutureScheduledAtWithBuffer() {
        Instant scheduledAt = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant pickupUntil = Instant.now().plus(5, ChronoUnit.HOURS);
        Instant consumeBefore = Instant.now().plus(6, ChronoUnit.HOURS);

        Instant expiry = otpService.calculateExpiry(scheduledAt, pickupUntil, consumeBefore);

        // Buffer is at least 30 minutes past scheduledAt
        Instant expectedMin = scheduledAt.plus(29, ChronoUnit.MINUTES);
        Instant expectedMax = scheduledAt.plus(31, ChronoUnit.MINUTES);
        assertTrue(expiry.isAfter(expectedMin));
        assertTrue(expiry.isBefore(expectedMax));
    }

    @Test
    void testCalculateExpiry_cappedByPickupAvailableUntil() {
        // scheduledAt is 10 minutes before pickupAvailableUntil, buffer of 30m would exceed pickupUntil
        Instant scheduledAt = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant pickupUntil = scheduledAt.plus(10, ChronoUnit.MINUTES);
        Instant consumeBefore = scheduledAt.plus(2, ChronoUnit.HOURS);

        Instant expiry = otpService.calculateExpiry(scheduledAt, pickupUntil, consumeBefore);

        assertEquals(pickupUntil, expiry);
    }

    @Test
    void testCalculateExpiry_cappedByConsumeBefore() {
        Instant scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant pickupUntil = scheduledAt.plus(2, ChronoUnit.HOURS);
        Instant consumeBefore = scheduledAt.plus(15, ChronoUnit.MINUTES);

        Instant expiry = otpService.calculateExpiry(scheduledAt, pickupUntil, consumeBefore);

        assertEquals(consumeBefore, expiry);
    }

    @Test
    void testCachedOtp_storeAndRetrieve_succeeds() {
        java.util.UUID pickupId = java.util.UUID.randomUUID();
        Instant expiry = Instant.now().plus(10, ChronoUnit.MINUTES);

        otpService.storeCachedOtp(pickupId, "123456", expiry);

        var cached = otpService.getCachedOtp(pickupId);
        assertTrue(cached.isPresent());
        assertEquals("123456", cached.get());
    }

    @Test
    void testCachedOtp_expired_returnsEmptyAndEvicts() {
        java.util.UUID pickupId = java.util.UUID.randomUUID();
        Instant expiredTime = Instant.now().minus(1, ChronoUnit.MINUTES);

        otpService.storeCachedOtp(pickupId, "123456", expiredTime);

        var cached = otpService.getCachedOtp(pickupId);
        assertTrue(cached.isEmpty());
    }

    @Test
    void testCachedOtp_invalidate_removesFromCache() {
        java.util.UUID pickupId = java.util.UUID.randomUUID();
        Instant expiry = Instant.now().plus(10, ChronoUnit.MINUTES);

        otpService.storeCachedOtp(pickupId, "123456", expiry);
        otpService.invalidateOtp(pickupId);

        var cached = otpService.getCachedOtp(pickupId);
        assertTrue(cached.isEmpty());
    }
}
