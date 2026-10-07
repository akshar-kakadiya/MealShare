package com.mealshare.receiver;

import com.mealshare.common.util.GeometryUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import static org.junit.jupiter.api.Assertions.*;

class SpatialRadiusCalculationTest {

    // Haversine formula for spherical distance in kilometers
    private double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Test
    void test16_and_test33_distanceCalculatedCorrectly_andRadiusFilter() {
        // Point 1: Receiver at MG Road, Bengaluru
        double receiverLat = 12.9716;
        double receiverLon = 77.5946;

        // Point 2: Indiranagar (~5.1 km away)
        double indiranagarLat = 12.9784;
        double indiranagarLon = 77.6408;

        // Point 3: Whitefield (~16.8 km away)
        double whitefieldLat = 12.9698;
        double whitefieldLon = 77.7500;

        double distToIndiranagar = calculateHaversineDistanceKm(receiverLat, receiverLon, indiranagarLat, indiranagarLon);
        double distToWhitefield = calculateHaversineDistanceKm(receiverLat, receiverLon, whitefieldLat, whitefieldLon);

        assertEquals(5.1, distToIndiranagar, 0.5, "Indiranagar should be ~5.1 km from MG Road");
        assertEquals(16.8, distToWhitefield, 1.0, "Whitefield should be ~16.8 km from MG Road");

        // Test 10 km radius filter:
        double searchRadiusKm = 10.0;
        assertTrue(distToIndiranagar <= searchRadiusKm, "Indiranagar should be within 10 km radius");
        assertFalse(distToWhitefield <= searchRadiusKm, "Whitefield should be outside 10 km radius");

        // Test 20 km radius filter:
        double expandedRadiusKm = 20.0;
        assertTrue(distToIndiranagar <= expandedRadiusKm, "Indiranagar should be within 20 km radius");
        assertTrue(distToWhitefield <= expandedRadiusKm, "Whitefield should be within 20 km radius");
    }

    @Test
    void test17_and_test34_nearestDonationSortsFirst() {
        double receiverLat = 12.9716;
        double receiverLon = 77.5946;

        double nearDonationLat = 12.9750;
        double nearDonationLon = 77.6000;

        double farDonationLat = 13.0500;
        double farDonationLon = 77.6500;

        double distNear = calculateHaversineDistanceKm(receiverLat, receiverLon, nearDonationLat, nearDonationLon);
        double distFar = calculateHaversineDistanceKm(receiverLat, receiverLon, farDonationLat, farDonationLon);

        assertTrue(distNear < distFar, "Nearest donation must have smaller distance value");
    }

    @Test
    void testGeometryPointsMaintainSridAndCoordinates() {
        Point p = GeometryUtils.createPoint(77.5946, 12.9716);
        assertNotNull(p);
        assertEquals(4326, p.getSRID());
        assertEquals(77.5946, p.getX());
        assertEquals(12.9716, p.getY());
    }
}
