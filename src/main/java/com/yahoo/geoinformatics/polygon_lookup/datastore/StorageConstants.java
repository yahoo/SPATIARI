package com.yahoo.geoinformatics.polygon_lookup.datastore;

/**
 * @author koushikm Keeping all constants related to storage at the same place
 */
public final class StorageConstants {

    public static final int SIZE_INT = 4; // number of bytes to store an integer
    public static final int BOUNDING_BOX_POINTS_COUNT = 4; // we need four values to represent a bounding box
    public static final int CHILDREN_COUNT_STORE = 1; // number of bytes to store the number of children
    public static final int MAX_HEADER_SIZE = 7; // seven integers : root, depth, maxChildren, 4 sizes for the storage arrays
    public static final int POINT_SIZE_IN_INT = 2; // points needs two integers
    public static final double ACCURACY_FACTOR = 1e6; // multiplication factor to convert double to int
    // offset to location of children count in a node
    public static final int NODE_CHILD_COUNT_OFFSET = BOUNDING_BOX_POINTS_COUNT * SIZE_INT;
    //Must for each polygon: attribute index, total number of boundary points, total number of rings, area, centroid(latitude, and longitude), radius
    public static final int POLYGON_METADATA_SIZE = 7;
    public static final int AVERAGE_RADIUS_OF_EARTH_METERS = 6371000; //taken from wikipedia, in meters
    public static final double PI = 22.0 / 7.0;
    public static final String DEFAULT_ISO_NAME = "DEFAULT"; //default iso value
    public static final int ATTRIBUTES_PER_LOCATION = 5; //distance, index, confidence, radius of influence, location-coverage

    public static final String RADIUS_ATTRIBUTE_NAME = "Radius"; //radius attribtues expected in data
}
