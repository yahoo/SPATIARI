package com.yahoo.geoinformatics.polygon_lookup.geometry;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;

/**
 * A polygon contains the list of points, zero or more rings, bounding box, and index to other attributes A polygon may
 * also contain one or more rings, each ring is a list of point
 *
 * @author koushikm
 */
public final class Polygon {

    private final int[] outerBoundaryPoints;
    // outer boundary coordinates of the polygon represented as a flattened array (x1,y1,x2,y2,...)
    private final int[][] rings; // each ring is a list of points
    private final Rectangle mbr; // Minimum bounding region for the polygon
    private final int attributeIndex; // Some index for attributes this polygon is associated with
    private final int area; // approximate area of this polygon, used for ranking

    // centroid of this polygon, used for ranking
    private final int centroidX;
    private final int centroidY;

    //radius of influence for point data
    private final int radiusOfInfluence;

    public Polygon(int attributeId, int[] inputPoints, int[][] inputRings, int area, int centroidX, int centroidY,
                   int radius) {
        attributeIndex = attributeId;
        outerBoundaryPoints = inputPoints;
        rings = inputRings;
        mbr = createBoundingBox(outerBoundaryPoints, radius);
        this.area = area;
        this.centroidX = centroidX;
        this.centroidY = centroidY;
        this.radiusOfInfluence = radius;
    }

    public int getAttributeIndex() {
        return attributeIndex;
    }

    /**
     * Given the vector of points, create the MBR Only outer boundary points define a bounding box
     * For point data, generate bounding box from the manufactured circle.
     *
     * @param points : List of points from a polygon to create a bounding box.
     * @param radius : radius of the point data or polygon. This is provided by the client data.
     * @return the new Rectangle object
     */
    public static Rectangle createBoundingBox(int[] points, int radius) {
        if (points == null || points.length == 0) {
            return null;
        }

        //check if it is point data
        if( (points.length == 4) && (points[0] == points[2]) && (points[1] == points[3])) {
            return createBoundingBoxPoint(points, radius);
        }

        //if it is not point data, find bounding box for polygon
        return createBoundingBoxPolygon(points);
    }

    /**
     * Given a point and radius, creat bounding box from the circle.
     *
     * @param points : List of points from a polygon to create a bounding box.
     * @param radius : radius of the point data. This is provided by the client data.
     * @return the new Rectangle object that bounds the circle
     */
    public static Rectangle createBoundingBoxPoint(int[] points, int radius) {
        double lat = Math.toRadians(points[1] / StorageConstants.ACCURACY_FACTOR);
        double lon = Math.toRadians(points[0] / StorageConstants.ACCURACY_FACTOR);

        // Radius of the parallel at given latitude
        double pradius = StorageConstants.AVERAGE_RADIUS_OF_EARTH_METERS * Math.cos(lat);

        double radiusLat = (double) radius / StorageConstants.AVERAGE_RADIUS_OF_EARTH_METERS;


        int minX = (int) (Math.toDegrees(lon - radius / pradius) * StorageConstants.ACCURACY_FACTOR);
        int maxX = (int) (Math.toDegrees(lon + radius / pradius) * StorageConstants.ACCURACY_FACTOR);
        int minY = (int) (Math.toDegrees(lat - radiusLat) * StorageConstants.ACCURACY_FACTOR);
        int maxY = (int) (Math.toDegrees(lat + radiusLat) * StorageConstants.ACCURACY_FACTOR);

        return new Rectangle(minX, minY, maxX, maxY);
    }

    /**
     * Given the vector of points, create the MBR Only outer boundary points define a bounding box
     *
     * @param points : List of points from a polygon to create a bounding box.
     * @return the new Rectangle object
     */
    public static Rectangle createBoundingBoxPolygon(int[] points) {
        int minX = points[0];
        int minY = points[1];
        int maxX = points[0];
        int maxY = points[1];

        for (int i = 0; i < points.length; i = i + 2) {
            int testX = points[i];
            int testY = points[i + 1];

            if (testX < minX) {
                minX = testX;
            }

            if (testY < minY) {
                minY = testY;
            }

            if (testX > maxX) {
                maxX = testX;
            }

            if (testY > maxY) {
                maxY = testY;
            }
        }
        return new Rectangle(minX, minY, maxX, maxY);
    }

    public int[] getOuterBoundaryPoints() {
        return outerBoundaryPoints;
    }

    public int[][] getRings() {
        return rings;
    }

    public Rectangle getBoundingBox() {
        return mbr;
    }

    public int getArea() {
        return area;
    }

    public int getCentroidX() {
        return centroidX;
    }

    public int getCentroidY() {
        return centroidY;
    }

    public int getRadiusOfInfluence() {
        return radiusOfInfluence;
    }

    /**
     * @return total number of points in this polygon including points in inner rings
     */
    public int getTotalPoints() {
        int totalPoints = 0;
        totalPoints += outerBoundaryPoints.length / 2;
        int ringCount = 0;
        if (rings != null) {
            ringCount = rings.length;
        }
        for (int i = 0; i < ringCount; ++i) {
            totalPoints += rings[i].length / 2;
        }

        return totalPoints;
    }

    /**
     * @return total number of rings in this polygon
     */
    public int getTotalRings() {
        int totalRings = 0;
        if (rings != null) {
            totalRings += rings.length;
        }

        return totalRings;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.getClass().getName());
        result.append(" Boundary Points: [");
        for(int i=0; i < outerBoundaryPoints.length; i=i+2) {
            result.append(outerBoundaryPoints[i] + "," + outerBoundaryPoints[i+1] + ";");
        }
        result.append("]");
        result.append("\n attributeIndex: " + attributeIndex + " number of boundary points:"
                      + outerBoundaryPoints.length);
        if (rings != null) {
            for (int i = 0; i < rings.length; ++i) {
                result.append("\n " + i + "th inner ring size: " + rings[i].length);
                result.append("\n " + i + "th inner ring points: ");
                for (int j = 0; j < rings[i].length; j += 2) {
                    result.append(" (");
                    result.append(rings[i][j]);
                    result.append(",");
                    result.append(rings[i][j + 1]);
                    result.append(")");
                }
            }
        }
        return result.toString();
    }
}
