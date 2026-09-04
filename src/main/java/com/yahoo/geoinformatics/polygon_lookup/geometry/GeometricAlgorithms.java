package com.yahoo.geoinformatics.polygon_lookup.geometry;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;

/**
 * This class represents all algorithms which can be used independent of data store. With these algorithms separated
 * out, we can reuse them independent of a storage object.
 * 
 * @author koushikm
 */
public class GeometricAlgorithms {

    /**
     * @param x1 x-coordinate of first point
     * @param y1 y-coordinate of first point
     * @param x2 x-coordinate of second point
     * @param y2 y-coordinate of second point
     * @return geospatial distance between two points on Earth
     */
    public static int distance(int x1, int y1, int x2, int y2) {
        double lat1 = (double) (y1 / StorageConstants.ACCURACY_FACTOR);
        double lon1 = (double) (x1 / StorageConstants.ACCURACY_FACTOR);

        double lat2 = (double) (y2 / StorageConstants.ACCURACY_FACTOR);
        double lon2 = (double) (x2 / StorageConstants.ACCURACY_FACTOR);

        return planeProjectedDistance(lat1, lon1, lat2, lon2);
    }

    /**
     * Fast relative metric on fixed-point coordinates ({@link StorageConstants#ACCURACY_FACTOR}).
     * Returns squared Euclidean distance in degree-units; no trigonometry or square root.
     * Used to select the closest boundary vertex; callers should compute meter distance separately.
     */
    public static int relativeRankingDistance(int x1, int y1, int x2, int y2) {
        long dx = (long) x1 - x2;
        long dy = (long) y1 - y2;
        long squared = dx * dx + dy * dy;
        if (squared > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) squared;
    }

    /**
     * @param lat1 latitude of the first coordinate
     * @param lon1 longitude of the first coordinate
     * @param lat2 latitude of the second coordinate
     * @param lon2 longitude of the second coordinate
     * @return Distance between two points on Earth using Haversine formula
     *         (https://en.wikipedia.org/wiki/Haversine_formula)
     */
    public static int haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDiff = Math.toRadians(lat2 - lat1);
        double lonDiff = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);

        double angle = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) (Math.round(angle * StorageConstants.AVERAGE_RADIUS_OF_EARTH_METERS));
    }

    /**
     * @param lat1 latitude of the first coordinate
     * @param lon1 longitude of the first coordinate
     * @param lat2 latitude of the second coordinate
     * @param lon2 longitude of the second coordinate
     * @return Distance between two points on Earth using plane projection formula (less accurate then Haversine but
     *         faster) : http://en.wikipedia.org/wiki/Geographical_distance#Spherical_Earth_projected_to_a_plane
     */
    public static int planeProjectedDistance(double lat1, double lon1, double lat2, double lon2) {
        double deltaLatRadian = Math.toRadians(lat1 - lat2);
        double deltaLonRadian = Math.toRadians(lon1 - lon2);
        double latMedianRadian = Math.toRadians((lat1 + lat2) / 2);

        double part1 = deltaLatRadian;
        double part2 = Math.cos(latMedianRadian) * deltaLonRadian;

        double distance = StorageConstants.AVERAGE_RADIUS_OF_EARTH_METERS * Math.sqrt(part1 * part1 + part2 * part2);
        return (int) (Math.round(distance));
    }

    /**
     * This method returns area of overlap between two circles
     * @param x1 x-coordinate of the center of first circle
     * @param y1 y-coordinate of the center of first circle
     * @param r1 radius of the first circle
     * @param x2 x-coordinate of the center of second circle
     * @param y2 y-coordinate of the center of second circle
     * @param r2 radius of the second circle
     * @return area of overlap between two circles if any
     */
    public static double getOverlappingArea(int x1, int y1, int r1, int x2, int y2, int r2) {
        double c = distance(x1, y1, x2, y2);
        if (c >= (r1 + r2)) {
            // No overlap
            return 0.0;
        } else if (c <= Math.abs(r1 - r2)) {
            //One circle is completely inside another: area of the smaller circle is area of overlap
            if (r1 >= r2) {
                return (StorageConstants.PI * r2 * r2);
            } else {
                return (StorageConstants.PI * r1 * r1);
            }
        } else { // (r1 + r2) > c > Math.abs(r1 - r2)
            // One circle overlap with another
            long rad1 = (long) r1; //to avoid overflow
            long rad2 = (long) r2; //to avoid overflow
            long r1squared = rad1 * rad1 ;
            long r2squared = rad2 * rad2;
            double csquared = c * c;
            double thetaOne = 2 * Math.acos((r1squared + csquared - r2squared) / (2 * r1 * c)); //in radians
            double thetaTwo = 2 * Math.acos((r2squared + csquared - r1squared) / (2 * r2 * c)); //in radians
            double areaOne = 0.5 * r1squared * (thetaOne - Math.sin(thetaOne));
            double areaTwo = 0.5 * r2squared * (thetaTwo - Math.sin(thetaTwo));
            return (areaOne + areaTwo);
        }
    }

}
