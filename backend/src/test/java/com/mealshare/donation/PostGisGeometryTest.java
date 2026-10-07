package com.mealshare.donation;

import com.mealshare.common.util.GeometryUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import static org.junit.jupiter.api.Assertions.*;

class PostGisGeometryTest {

    @Test
    void test22_postGisGeometryStoredWithSrid4326_andCorrectCoordinateOrder() {
        Double longitude = 77.5946; // X
        Double latitude = 12.9716;  // Y

        Point point = GeometryUtils.createPoint(longitude, latitude);

        assertNotNull(point, "Geometry Point must not be null");
        assertEquals(4326, point.getSRID(), "SRID must be 4326 for WGS84");
        assertEquals(longitude, point.getX(), 0.000001, "Longitude must map to coordinate X");
        assertEquals(latitude, point.getY(), 0.000001, "Latitude must map to coordinate Y");
        assertEquals("Point", point.getGeometryType());
    }

    @Test
    void testGeometryCreation_withNullCoordinates_returnsNull() {
        assertNull(GeometryUtils.createPoint(null, 12.9716));
        assertNull(GeometryUtils.createPoint(77.5946, null));
        assertNull(GeometryUtils.createPoint(null, null));
    }
}
