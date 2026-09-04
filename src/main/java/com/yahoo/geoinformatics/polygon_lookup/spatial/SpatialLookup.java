package com.yahoo.geoinformatics.polygon_lookup.spatial;

import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
import com.yahoo.geoinformatics.polygon_lookup.rtree.RTree;
import com.yahoo.geoinformatics.polygon_lookup.datastore.Storage;


import java.util.Arrays;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;


/**
 * This class is responsible for search for a client. This hides the internal R-Tree based indexing details.
 *
 * @author koushikm
 */
public class SpatialLookup {

    private final RTree index;

    public SpatialLookup(RTree tree) {
        index = tree;
    }

    /**
     * Given the coordinates of a place, return the index of the shape containing the point
     *
     * @param latitude  latitude of the queried point
     * @param longitude longitude of the queried point
     * @param metrics  Internal metrics of RTree to get insights. Pass null to get nothing
     * @return index of the shape containing the given coordinates
     */
    public int search(double latitude, double longitude, int[] metrics) {
        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        return index.search(lon, lat, metrics);
    }

    /**
     * Given the id, return the polygon associated with it
     *
     * @param id  id to lookup polygon, this is not a gid but a Stored Id
     * @return Polygon associated with the given id
     */
    public Polygon getPolygon(int id) {
        return this.index.getPolygon(id);
    }

    /**
     * Given a circle of search, return the list of polygons overlapping with the circle.
     * Currently we will return only one result along with distance and confidence.
     * This is preferable search API for admin layers like town, zip etc with horizontal accuracy in input.
     *
     * @param latitude           latitude of the queried point
     * @param longitude          longitude longitude of the queried point
     * @param horizontalAccuracy horizontal accuracy of the GPS coordinate in meters
     * @param searchRadius       search radius in meters
     * @param searchResults      array of indices of shapes inside the search circle, array will be reset before the search
     * @param metrics            Internal metrics of RTree to get insights. Pass null to get nothing
     */
    public void searchNearby(double latitude, double longitude, int horizontalAccuracy, int searchRadius,
                             int[] searchResults, int[] metrics) {
        if (searchResults == null) {
            throw new IllegalArgumentException("searchResults not initialized");
        }

        for (int i = 0; i < searchResults.length; ++i) {
            searchResults[i] = -1;
        }

        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        index.searchNearby(lon, lat, horizontalAccuracy, searchRadius, searchResults, metrics);
    }

    /**
     * Select ranking-distance algorithm for single-result {@code searchNearby} (benchmark use only).
     */
    public void setNearbyRankingStrategyForBenchmark(Storage.NearbyRankingStrategy strategy) {
        index.setNearbyRankingStrategyForBenchmark(strategy);
    }

    public Storage.NearbyRankingStrategy getNearbyRankingStrategyForBenchmark() {
        return index.getNearbyRankingStrategyForBenchmark();
    }

    /**
     * Given a circle of search, return the list of polygons overlapping with the circle.
     * This is preferable search API for  layers like POI/Business listings etc with horizontal accuracy in input.
     * There will be multiple results, total number of that is determined by the user via the length of the result array.
     * The offset parameters decides the gap between two consecutive results in the result array.
     *
     * @param latitude           latitude of the queried point
     * @param longitude          longitude longitude of the queried point
     * @param horizontalAccuracy horizontal accuracy of the GPS coordinate in meters
     * @param searchRadius       search radius
     * @param searchResults      array of indices of results inside the search circle, array will be reset before the search
     * @param offset             distance between two entries in the result array, this allows some gaps to filled up in higher
     *                           layer, minimum value expected is five (distance, index, confidence, radius of influence, location-coverage)
     * @param metrics            Internal metrics of RTree to get insights. Pass null to get nothing
     */
    public void searchNearby(double latitude, double longitude, int horizontalAccuracy, int searchRadius,
                             int[] searchResults,
                             int offset, int[] metrics) {
        if ((searchResults == null) || (searchResults.length < offset)) {
            throw new IllegalArgumentException(
                "searchResults not initialized and/or not enough space to store one result");
        }

        if ((searchResults.length % offset) != 0) {
            throw new IllegalArgumentException("not sufficient space to store even one result");
        }

        Arrays.fill(searchResults, -1);

        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        index.searchNearby(lon, lat, horizontalAccuracy, searchRadius, searchResults, offset, metrics);
    }

    /**
     * Given a bounding-box of search, return the list of polygons overlapping with the circle.
     * This is preferable search API for  layers like POI/Business listings etc with horizontal accuracy in input.
     * There will be multiple results, total number of that is determined by the user via the length of the result array.
     * The offset parameters decides the gap between two consecutive results in the result array.
     *
     * @param rminX          longitude of bottom left of bounding box
     * @param rminY          latitude of bottom left of bounding box
     * @param rmaxX          longitude of top right of bounding box
     * @param rmaxY          latitude of top right of bounding box
     * @param searchResults  array of indices of results inside the search bounding box, array will be reset before the search
     * @param metrics        Internal metrics of RTree to get insights. Pass null to get nothing
     */
    public void search(int rminX, int rminY, int rmaxX, int rmaxY, int[] searchResults, int[] metrics) {
        if (searchResults == null) {
            throw new IllegalArgumentException("searchResults not initialized");
        }

        for (int i = 0; i < searchResults.length; ++i) {
            searchResults[i] = -1;
        }

        index.search(rminX, rminY, rmaxX, rmaxY, searchResults, metrics);
    }

}
