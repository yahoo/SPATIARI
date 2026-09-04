package com.yahoo.geoinformatics.polygon_lookup.spatial;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexerTest;
import com.yahoo.geoinformatics.polygon_lookup.reader.ShapeFileReader;
import com.yahoo.geoinformatics.polygon_lookup.reader.StorageDimensions;
import com.yahoo.geoinformatics.polygon_lookup.rtree.RTree;

/**
 * Test class to test the entire library end to end bypassing client facing APIs
 *
 * @author koushikm
 */
public class SpatialLibraryTester {

    private static final String INPUT_POLYGON_FILENAME = "Wld_Town_Polygon_Subset.shp";
    private static final String INPUT_POINT_FILENAME = "Wld_Town_Point_Subset.shp";
    private static final String INPUT_RADIUS_FILENAME = "Wld_Town_Point_Subset_Radius_File.csv";
    private static final String ATTRIBUTES_INDEX_NAME = "WOE_ID";
    private static final int MAX_CHILDREN = 3;

    public void testLibrary() throws Exception {
        // Read the polygon shape file and point shape file
        ClassLoader classLoader = getClass().getClassLoader();
        URL urlPolygon = classLoader.getResource(INPUT_POLYGON_FILENAME);
        if (null == urlPolygon) {
            throw new Exception("File " + INPUT_POLYGON_FILENAME + " Not Found.");
        }
        URL urlPoint = classLoader.getResource(INPUT_POINT_FILENAME);
        if (null == urlPoint) {
            throw new Exception("File " + INPUT_POINT_FILENAME + " Not Found.");
        }
        URL radiusFileUrl = classLoader.getResource(INPUT_RADIUS_FILENAME);
        if (null == radiusFileUrl) {
            throw new Exception("File " + INPUT_RADIUS_FILENAME + " Not Found.");
        }
        AttributeIndexerTest indexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        ShapeFileReader
            reader =
            new ShapeFileReader(urlPolygon.getFile(), urlPoint.getFile(), radiusFileUrl.getFile(), indexer);
        StorageDimensions dimensions = reader.computeStorageDimensions();
        System.out.println("Total numbert of entries:" + dimensions.getTotalEntries());
        indexer.init(dimensions.getTotalEntries());
        reader.process();
        List<Polygon> polygons = reader.getPolygons();
        HashMap<Integer, String> indexMap = indexer.getAttributesIndexMap();
        /*
        for (Map.Entry<Integer, String> entry : indexMap.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            System.out.println("key =" + key + " value=" + value);
        }*/
        //System.out.println("Total numbert of polygons:" + polygons.size());
        // Create the index
        RTree rtree = new RTree(MAX_CHILDREN);
        rtree.buildIndex(polygons);
        System.out.println("=========== Done with indexing ===============");
        // System.out.println(rtree.toString());
        // Do lookup
        int[] results = new int[10];
        Arrays.fill(results, -1);
        System.out.println("============Radial lookup follows=============");
        /*
        double latitude = 38.017;
        double longitude = -84.478;
        int radius = 5000; // in meters
        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.search(lon, lat, radius, results);
        for (int i = 0; i < results.length; i+=2) {
            if (results[i] >= 0) {
                System.out.println("result at index " + i + " = " + indexMap.get(results[i]));
            }
        }

        latitude = 39.284;
        longitude = -84.509;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.search(lon, lat, radius, results);
        for (int i = 0; i < results.length; i+=2) {
            if (results[i] >= 0) {
                System.out.println("result at index " + i + " = " + indexMap.get(results[i]));
            }
        }

        latitude = 39.191;
        longitude = -84.494;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.search(lon, lat, radius, results);
        for (int i = 0; i < results.length; i+=2) {
            if (results[i] >= 0) {
                System.out.println("result at index " + i + " = " + indexMap.get(results[i]));
            }
        }
        
        latitude = 39.139;
        longitude = -84.626;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.search(lon, lat, radius, results);
        for (int i = 0; i < results.length; i+=2) {
            if (results[i] >= 0) {
                System.out.println("result at index " + i + " = " + indexMap.get(results[i]));
            }
        }
        
        latitude = 39.314;
        longitude = -84.448;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.search(lon, lat, radius, results);
        for (int i = 0; i < results.length; i+=2) {
            if (results[i] >= 0) {
                System.out.println("result at index " + i + " = " + indexMap.get(results[i]));
            }
        }
        */

        System.out.println("============Radial lookup with nearby point=============");
        double latitude = 39.336;
        double longitude = -84.426;
        int radius = 10000; // in meters
        int accuracy = 10; //in meters
        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        for (int i = 0; i < results.length; i += 2) {
            if (results[i] >= 0) {
                System.out.println("Woeid at index " + i + " = " + indexMap.get(results[i]));
            }
        }

        System.out.println("============Radial lookup with inside polygon=============");
        latitude = 39.333;
        longitude = -84.546;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        for (int i = 0; i < results.length; i += 2) {
            if (results[i] >= 0) {
                System.out.println("result at index " + i + " = " + indexMap.get(results[i]));
            }
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SpatialLibraryTester tester = new SpatialLibraryTester();
        tester.testLibrary();

    }

}
