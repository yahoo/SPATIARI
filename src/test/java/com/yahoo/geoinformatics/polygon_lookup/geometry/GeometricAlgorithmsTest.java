package com.yahoo.geoinformatics.polygon_lookup.geometry;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;

public class GeometricAlgorithmsTest {

    @Test
    public void testDistance() {
        int x1 = -121289850; // lon = -121.289850
        int y1 = 44766880; // lat = 44.766880
        int x2 = -116182299; // lon = -116.182299
        int y2 = 41916600; // lat = 41.916600

        int distance = GeometricAlgorithms.distance(x1, y1, x2, y2);
        System.out.println("distance = " + distance);
        Assert.assertTrue(distance > 0);
    }

    @Test
    public void testHaversineDistance() {
        // a point in Austin Nevada (lon = -117.074089, lat = 39.497790)
        double lon1 = -117.074089;
        double lat1 = 39.497790;
        // a point in kingston Nevada (lon = -117.067930, lat = 39.210129)
        double lon2 = -117.067930;
        double lat2 = 39.210129;

        int haversineDistance = GeometricAlgorithms.haversineDistance(lat1, lon1, lat2, lon2);
        System.out.println("haversineDistance = " + haversineDistance);
        Assert.assertTrue(haversineDistance > 0);
    }

    @Test
    public void testPlaceProjectedDistance() {
        // a point in Austin Nevada (lon = -117.074089, lat = 39.497790)
        double lon1 = -117.074089;
        double lat1 = 39.497790;
        // a point in kingston Nevada (lon = -117.067930, lat = 39.210129)
        double lon2 = -117.067930;
        double lat2 = 39.210129;

        int planeProjectedDistance = GeometricAlgorithms.planeProjectedDistance(lat1, lon1, lat2, lon2);
        System.out.println("planeProjectedDistance = " + planeProjectedDistance);
        Assert.assertTrue(planeProjectedDistance > 0);
    }


    @Test
    public void testRelativeRankingDistancePreservesOrder() {
        int queryX = -86905842;
        int queryY = 35972681;
        int nearX = -86900000;
        int nearY = 35970000;
        int farX = -86950000;
        int farY = 35950000;

        int nearMeters = GeometricAlgorithms.distance(queryX, queryY, nearX, nearY);
        int farMeters = GeometricAlgorithms.distance(queryX, queryY, farX, farY);
        int nearRelative = GeometricAlgorithms.relativeRankingDistance(queryX, queryY, nearX, nearY);
        int farRelative = GeometricAlgorithms.relativeRankingDistance(queryX, queryY, farX, farY);

        Assert.assertTrue(nearMeters < farMeters);
        Assert.assertTrue(nearRelative < farRelative);
    }

    @Test
    public void testCircleOverlapInside() {
        // case 1 : one circle is inside another
        int x1 = -117772300; // lon = -117.7723
        int y1 = 70736190; // lat = 70.73619
        int r1 = 25;
        int x2 = -117772300; // lon = -117.7723
        int y2 = 70736190; // lat = 70.73619
        int r2 = 18;

        double overlappedArea = GeometricAlgorithms.getOverlappingArea(x1, y1, r1, x2, y2, r2);
        System.out.println("case 1 overlapped area =" + overlappedArea);
        double expectedArea = StorageConstants.PI * r2 * r2;
        System.out.println("Expected area =" + expectedArea);
        Assert.assertTrue(Double.compare(overlappedArea, expectedArea) == 0);
    }

    @Test
    public void testCircleOverlapOutside() {
        // case 2: circles do not overlap
        // lon = -117.7723, lat = 70.73619
        int x1 = -117772300;
        int y1 = 70736190;
        int r1 = 25;
        // lon = -123.42151, lat = 65.18523
        int x2 = -123421510;
        int y2 = 65185230;
        int r2 = 50;
        double overlappedArea = GeometricAlgorithms.getOverlappingArea(x1, y1, r1, x2, y2, r2);
        double expectedArea = 0.0;
        Assert.assertTrue(Double.compare(overlappedArea, expectedArea) == 0);
        System.out.println("case 2 overlapped area =" + overlappedArea);
    }

    @Test
    public void testCircleOverlapMinor() {
        // case 3: circles (minor) overlap : overlapped area is small
        // lon = -123.42151, lat = 65.18523
        int x1 = -123421510;
        int y1 = 65186230;
        int r1 = 30;
        // lon = -123.409548, lat = 63.206513
        int x2 = -123421548;
        int y2 = 65186513;
        int r2 = 20;
        int distance = GeometricAlgorithms.distance(x1, y1, x2, y2);
        double overlappedArea = GeometricAlgorithms.getOverlappingArea(x1, y1, r1, x2, y2, r2);
        double halfSmallerCircleArea = 0.5 * StorageConstants.PI * r2 * r2;
        System.out.println("distance between two circles =" + distance);
        System.out.println("case 3 half Smaller Circle Area =" + halfSmallerCircleArea);
        System.out.println("case 3 minor overlapped area =" + overlappedArea);
        Assert.assertTrue(overlappedArea > 0.0);
        // for a very little overlap, area of overlap should be smaller that half of area of smaller circle
        Assert.assertTrue(Double.compare(overlappedArea, halfSmallerCircleArea) < 0);
    }

    @Test
    public void testCircleOverlapMajor() {
        // case 4: circles (major) overlap : overlapped area is huge
        // lon = -123.42151, lat = 65.18523
        int x1 = -123421510;
        int y1 = 65186230;
        int r1 = 50;
        // lon = -123.409548, lat = 63.206513
        int x2 = -123421548;
        int y2 = 65186513;
        int r2 = 20;
        int distance = GeometricAlgorithms.distance(x1, y1, x2, y2);
        double overlappedArea = GeometricAlgorithms.getOverlappingArea(x1, y1, r1, x2, y2, r2);
        double halfSmallerCircleArea = 0.5 * StorageConstants.PI * r2 * r2;
        System.out.println("distance between two circles =" + distance);
        System.out.println("case 4 half Smaller Circle Area =" + halfSmallerCircleArea);
        System.out.println("case 4 major overlapped area =" + overlappedArea);
        Assert.assertTrue(overlappedArea > 0.0);
        // for a large overlap, area of overlap should be larger than half of area of smaller circle
        Assert.assertTrue(Double.compare(overlappedArea, halfSmallerCircleArea) > 0);
    }

    @Test
    public void testCircleOverlapLargeAccuracy() {
        // case 5: when we have large horizontal accuracy
        // large value of horizontal accuracy or radius of influence may lead to overflow which will lead to negative
        // confidence score
        //lat =  -8.54777236003428; lon = 116.076682219281;
        int x1 = 116076682; int y1 = -8547772; int r1 = 55527;
        int x2 = 116191657; int y2 = -8523282; int r2 = 53995;
        int distance = GeometricAlgorithms.distance(x1, y1, x2, y2);
        System.out.println("distance between two circles =" + distance);
        double overlappedArea = GeometricAlgorithms.getOverlappingArea(x1, y1, r1, x2, y2, r2);
        //in case of overflow, overlapped area will be negative
        Assert.assertTrue(overlappedArea > 0.0);
    }

}
