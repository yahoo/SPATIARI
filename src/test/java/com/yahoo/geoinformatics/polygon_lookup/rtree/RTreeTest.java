package com.yahoo.geoinformatics.polygon_lookup.rtree;

import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexerTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
// import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
import com.yahoo.geoinformatics.polygon_lookup.reader.ShapeFileReader;
import com.yahoo.geoinformatics.polygon_lookup.reader.StorageDimensions;

public class RTreeTest {

    private static final String FILENAME = "Test_region.shp";
    private static final String ATTRIBUTES_INDEX_NAME = "BLOCKID10";
    private static final String INPUT_POLYGON_FILENAME = "Wld_Town_Polygon_Subset.shp";
    private static final String INPUT_POINT_FILENAME = "Wld_Town_Point_Subset.shp";
    private static final String INPUT_RADIUS_FILENAME = "Wld_Town_Point_Subset_Radius_File.csv";
    private ShapeFileReader reader;
    private RTree rtree;
    private AttributeIndexerTest attributeIndexer;

    public int getIndex(String value, HashMap<Integer, String> myMap) {
        int key = -1;
        for (Map.Entry<Integer, String> entry : myMap.entrySet()) {
            if (value.equals(entry.getValue())) {
                key = (int) entry.getKey();
                break; // breaking because its one to one map
            }
        }

        return key;
    }

    @BeforeClass
    public void setup() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);
        reader.process();
        int maxChildren = 3;
        rtree = new RTree(maxChildren);
        rtree.buildIndex(reader.getPolygons());
    }

    @Test
    public void testIndexCreation() {
        Assert.assertEquals(rtree.getRootId(), 16);
        Assert.assertEquals(rtree.getHeight(), 3);
    }

    @Test
    public void testNearby() {
        HashMap<Integer, String> attributesIndex = attributeIndexer.getAttributesIndexMap();
        for (Map.Entry<Integer, String> entry : attributesIndex.entrySet()) {
            System.out.println("key=" + entry.getKey() + " :: value=" + entry.getValue());
        }

        System.out.println(rtree.toString());

        // result should have index 6 at least (may have 7 and 9 depending on search radius)
        int[] results = new int[20];
        Arrays.fill(results, -1);
        double latitude = 37.5850; // 37.6517; //37.5744;
        double longitude = -121.9978; // -121.9500; //-122.0085;
        int radius = 1000; //in meters
        int accuracy = 10; //in meters
        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        Assert.assertTrue(results[0] > 0);
        int count = 0;
        for (int i = 0; i < results.length; ++i) {
            if (results[i] < 0) {
                break;
            }
            ++count;
            System.out.println("value =" + results[i]);
        }
        System.out.println("===== result size :" + count);

        // result should have index index 3 and 4
        Arrays.fill(results, -1);
        latitude = 37.6517;
        longitude = -121.9500;
        accuracy = 10;
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        Assert.assertTrue(results[0] > 0);
        count = 0;
        for (int i = 0; i < results.length; ++i) {
            if (results[i] < 0) {
                break;
            }
            ++count;
            System.out.println("value =" + results[i]);
        }
        System.out.println("===== result size :" + count);

        // result should have index 8
        Arrays.fill(results, -1);
        latitude = 37.5744;
        longitude = -122.0085;
        accuracy = 10;
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        Assert.assertTrue(results[0] > 0);
        count = 0;
        for (int i = 0; i < results.length; ++i) {
            if (results[i] < 0) {
                break;
            }
            ++count;
            System.out.println("value =" + results[i]);
        }
        System.out.println("===== result size :" + count);

        // result should have 5
        Arrays.fill(results, -1);
        latitude = 37.5990;
        longitude = -121.9796;
        accuracy = 5;
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        Assert.assertTrue(results[0] > 0);
        count = 0;
        for (int i = 0; i < results.length; ++i) {
            if (results[i] < 0) {
                break;
            }
            ++count;
            System.out.println("value =" + results[i]);
        }
        System.out.println("===== result size :" + count);

        // no result - negative test
        Arrays.fill(results, -1);
        latitude = 37.6276;
        longitude = -122.0230;
        accuracy = 5;
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        count = 0;
        for (int i = 0; i < results.length; ++i) {
            if (results[i] > 0) {
                ++count;
                System.out.println("value =" + results[i]);
            }
        }
        System.out.println("===== result size :" + count);
        Assert.assertTrue(count == 0);
    }

    @Test
    public void testPositiveLookupNoObject() {
        HashMap<Integer, String> attributesIndex = attributeIndexer.getAttributesIndexMap();
        int lat = 37638637;
        int lon = -121968290;
        String blockId = "060014401001000";
        int attributesId = getIndex(blockId, attributesIndex);
        int resultId = rtree.search(lon, lat, null);
        Assert.assertTrue(resultId != -1);
        Assert.assertEquals(resultId, attributesId);

        lat = 37583570;
        lon = -121980039;
        blockId = "060014411002013";
        attributesId = getIndex(blockId, attributesIndex);
        resultId = rtree.search(lon, lat, null);
        Assert.assertTrue(resultId != -1);
        Assert.assertEquals(resultId, attributesId);

        lat = 37585693;
        lon = -121999433;
        blockId = "060014412003013";
        attributesId = getIndex(blockId, attributesIndex);
        resultId = rtree.search(lon, lat, null);
        Assert.assertTrue(resultId != -1);
        Assert.assertEquals(resultId, attributesId);

        Assert.assertFalse(rtree.toString().isEmpty());
    }

    @Test
    public void testNegativeLookup() {
        int lat = 37597313;
        int lon = -121979401;
        int index = rtree.search(lon, lat, null);
        Assert.assertFalse(index != -1);
    }

    @Test
    public void testNegativeLookupNoObject() {
        int lat = 37597313;
        int lon = -121979401;
        int resultId = rtree.search(lon, lat, null);
        Assert.assertFalse(resultId != -1);
    }

    @Test
    public void testDonutLookup() {
        int lat = 3758104;
        int lon = -12200545;
        int index = rtree.search(lon, lat, null);
        Assert.assertFalse(index != -1);

        double latitude = 37.580980;
        double longitude = -122.00545;
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        int resultId = rtree.search(lon, lat, null);
        Assert.assertTrue(resultId == -1);
    }

    @Test
    public void testPointPolygonLookup() throws Exception {
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
        AttributeIndexerTest indexer = new AttributeIndexerTest("WOE_ID", null);
        ShapeFileReader
            reader =
            new ShapeFileReader(urlPolygon.getFile(), urlPoint.getFile(), radiusFileUrl.getFile(), indexer);
        StorageDimensions dimensions = reader.computeStorageDimensions();
        System.out.println("Total numbert of entries:" + dimensions.getTotalEntries());
        Assert.assertTrue(dimensions.getTotalEntries() > 0);
        indexer.init(dimensions.getTotalEntries());
        reader.process();
        List<Polygon> polygons = reader.getPolygons();
        HashMap<Integer, String> indexMap = indexer.getAttributesIndexMap();
        System.out.println("Total numbert of polygons:" + polygons.size());
        Assert.assertTrue(polygons.size() > 0);
        // Create the index
        RTree rtree = new RTree(3);
        rtree.buildIndex(polygons);
        // Do lookup
        int[] results = new int[10];
        Arrays.fill(results, -1);
        double latitude = 38.017;
        double longitude = -84.478;
        int radius = 5000; // in meters
        int accuracy = 10; //in meters
        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        Assert.assertTrue(results[1] > 0);

        latitude = 39.191;
        longitude = -84.494;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        Assert.assertTrue(results[1] > 0);

        latitude = 39.139;
        longitude = -84.626;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        Assert.assertTrue(results[1] > 0);

        latitude = 39.314;
        longitude = -84.448;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        Assert.assertTrue(results[1] > 0);

        //PiP result should have higher rank then nearby result
        // ,,2490328,2500120
        latitude = 44.486486486486484;
        longitude = -68.87307387439482;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(results, -1);
        rtree.searchNearby(lon, lat, accuracy, radius, results, null);
        //NOTE: result should  be 2490328 as the search point is inside this polygon, other possible nearby result is 2500120 
        //(but this nearby result 2500120 should be lower in rank then 2490328)
        System.out.println("result is =" + Integer.valueOf(indexMap.get(results[1])));
        Assert.assertTrue(results[1] > 0);
        Assert.assertTrue(2490328 == Integer.valueOf(indexMap.get(results[1])));

        //check with array odd size
        int[] resultsodd = new int[5];
        latitude = 39.314;
        longitude = -84.448;
        radius = 5000; // in meters
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        Arrays.fill(resultsodd, -1);
        rtree.searchNearby(lon, lat, accuracy, radius, resultsodd, null);
        Assert.assertTrue(resultsodd[1] > 0);
    }
}
