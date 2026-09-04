package com.yahoo.geoinformatics.polygon_lookup.datastore;

import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yahoo.geoinformatics.polygon_lookup.rtree.RTree;
import com.yahoo.geoinformatics.polygon_lookup.reader.ShapeFileReader;
import com.yahoo.geoinformatics.polygon_lookup.rtree.Node;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;

public class StorageTest {

    @Test
    public void testStorageAndRetrieval() {
        List<Polygon> polygons = new ArrayList<Polygon>();
        List<Node> nodes = new ArrayList<Node>();
        int totalLeafNodes = 16;
        int maxChildren = 4;
        int root = 11;
        int depth = 4;
        int[]
            points =
            new int[]{-1221298, 3768244, -1221336, 3768090, -1221336, 3768095, -1221298, 3768250, -1221298, 3768245};
        int attributesId = 3001;
        int area = 100;
        Polygon polygon = new Polygon(attributesId, points, null, area, -1221319, 3768150, 10);
        polygons.add(polygon);
        int polygonId = 3; //same as leafNodeId

        int leafNodeId = 3; //same as polygonId
        Node leafNode = Node.createLeafNode(leafNodeId, polygon.getBoundingBox());
        List<Node> candidates = Arrays.asList(leafNode);
        int internalNodeId = 5;
        Node iNode = Node.createInternalNode(internalNodeId, candidates, 0, 3);
        nodes.add(iNode); //at index 0
        nodes.add(leafNode); //at index 1

        Storage storage = new Storage(totalLeafNodes, 0, 100, maxChildren);
        boolean result = storage.storePolygon(polygonId, polygons.get(0));
        Assert.assertTrue(result);

        result = storage.storeIndex(nodes.get(0).getId(), nodes.get(0));
        Assert.assertTrue(result);

        result = storage.storeIndex(nodes.get(1).getId(), nodes.get(1));
        Assert.assertTrue(result);

        result = storage.storeHeader(root, depth, maxChildren);
        Assert.assertTrue(result);

        Assert.assertEquals(storage.getRoot(), root);
        Assert.assertEquals(storage.getHeight(), depth);
        Assert.assertEquals(storage.getMaxChildren(), maxChildren);

        Node node0 = storage.getNode(internalNodeId);
        Assert.assertEquals(node0.getId(), internalNodeId);
        Assert.assertEquals(node0.getBoundingBox().getMinX(), iNode.getBoundingBox().getMinX());
        Assert.assertEquals(node0.getBoundingBox().getMinY(), iNode.getBoundingBox().getMinY());
        Assert.assertEquals(node0.getBoundingBox().getMaxX(), iNode.getBoundingBox().getMaxX());
        Assert.assertEquals(node0.getBoundingBox().getMaxY(), iNode.getBoundingBox().getMaxY());
        Assert.assertEquals(node0.getChildren().length, 1);
        Assert.assertEquals(node0.getChildren()[0], 3);

        Node node1 = storage.getNode(leafNodeId);
        Assert.assertEquals(node1.getId(), leafNodeId);
        Assert.assertEquals(node1.getBoundingBox().getMinX(), leafNode.getBoundingBox().getMinX());
        Assert.assertEquals(node1.getBoundingBox().getMinY(), leafNode.getBoundingBox().getMinY());
        Assert.assertEquals(node1.getBoundingBox().getMaxX(), leafNode.getBoundingBox().getMaxX());
        Assert.assertEquals(node1.getBoundingBox().getMaxY(), leafNode.getBoundingBox().getMaxY());
        Assert.assertTrue(node1.getChildren() == null);

        Polygon p = storage.getPolygon(polygonId);
        Assert.assertEquals(p.getAttributeIndex(), polygon.getAttributeIndex());
        Assert.assertEquals(p.getOuterBoundaryPoints().length, polygon.getOuterBoundaryPoints().length);
        Assert.assertEquals(p.getArea(), polygon.getArea());
        Assert.assertEquals(p.getCentroidX(), polygon.getCentroidX());
        Assert.assertEquals(p.getCentroidY(), polygon.getCentroidY());
        Assert.assertEquals(p.getBoundingBox().getMinX(), polygon.getBoundingBox().getMinX());
        Assert.assertEquals(p.getBoundingBox().getMinY(), polygon.getBoundingBox().getMinY());
        Assert.assertEquals(p.getBoundingBox().getMaxX(), polygon.getBoundingBox().getMaxX());
        Assert.assertEquals(p.getBoundingBox().getMaxY(), polygon.getBoundingBox().getMaxY());

    }

    @Test
    public void testStoreIndex() throws Exception {
        final String FILENAME = "Test_region.shp";
        final String ATTRIBUTES_INDEX_NAME = "BLOCKID10";
        final String DATAPACK_NAME = "Test_region.datapack";
        ShapeFileReader reader;
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);
        reader.process();

        int maxChildren = 3;
        RTree rtree = new RTree(maxChildren);
        rtree.buildIndex(reader.getPolygons());
        rtree.storeIndex(DATAPACK_NAME, false);

        Assert.assertEquals(rtree.getRootId(), 16);
        Assert.assertEquals(rtree.getHeight(), 3);
        Assert.assertEquals(rtree.getMaxChildren(), 3);
    }

    @Test
    public void testPolygonsWithRingsProcessing() throws Exception {
        final String FILENAME = "Test_postal.shp";
        final String ATTRIBUTES_INDEX_NAME = "WOE_ID";
        ShapeFileReader reader;
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);
        reader.process();

        List<Polygon> polygons = reader.getPolygons();
        HashMap<Integer, String> attributesIndexMap = attributeIndexer.getAttributesIndexMap();
        Assert.assertEquals(3, polygons.size());
        Assert.assertTrue(polygons.get(0).getTotalRings() > 0);
        Assert.assertEquals(attributesIndexMap.size(), 3);
    }

    @Test
    public void testLoadStorage() throws Exception {
        //this datapack is created from Test_region.shp
        final String DATAPACK_NAME = "Test_region.datapack";
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(DATAPACK_NAME);
        if (null == url) {
            throw new Exception("File " + DATAPACK_NAME + " Not Found.");
        }

        Storage storage = Storage.loadFrom(url.getFile(),false);
        Assert.assertTrue(storage != null);
        RTree rtree = new RTree(storage);

        Assert.assertEquals(rtree.getRootId(), 16);
        Assert.assertEquals(rtree.getHeight(), 3);
        Assert.assertEquals(rtree.getMaxChildren(), 3);

        //The following test very specific to data in the shapefile (Test_region.shp)
        int entryId = 8;
        Polygon p = storage.getPolygon(entryId);
        Assert.assertEquals(1, p.getTotalRings());
        Assert.assertEquals(8, p.getAttributeIndex());
        Assert.assertEquals(186, p.getOuterBoundaryPoints().length / 2);
        Assert.assertEquals(18, p.getRings()[0].length / 2);

        Node n = storage.getNode(entryId);
        Assert.assertTrue(n.getChildren() == null);

        entryId = 16;
        n = storage.getNode(entryId);
        Assert.assertEquals(2, n.getChildren().length);

    }

    @Test
    public void testPolygonsWithMultipleRings() throws Exception {
        final String FILENAME = "US_TOWN_MULTIPLE_RINGS.shp";
        final String ATTRIBUTES_INDEX_NAME = "WOE_ID";
        ShapeFileReader reader;
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);
        reader.process();

        List<Polygon> polygons = reader.getPolygons();
        HashMap<Integer, String> attributesIndexMap = attributeIndexer.getAttributesIndexMap();
        int maxChildren = 3;
        RTree rtree = new RTree(maxChildren);
        rtree.buildIndex(reader.getPolygons());

        //total number of polygons (a place is a multi-polygon and hence can contain more than one polygon)
        Assert.assertEquals(polygons.size(), 15);
        //mapping between running index and woeid, we have 11 multi-polygon in the test shapefile
        Assert.assertEquals(attributesIndexMap.size(), 11);

        //Following points are within polygons with inner rings but not inside any ring
        double latitude = 41.426895;
        double longitude = -97.344795;
        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        int index = rtree.search(lon, lat, null);
        Assert.assertTrue(index != -1);

        latitude = 28.0042980574761;
        longitude = -82.6911075183951;
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        index = rtree.search(lon, lat, null);
        Assert.assertTrue(index != -1);

        //Following points are inside a ring of a polygon with many rings
        latitude = 27.99831;
        longitude = -82.69227;
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        index = rtree.search(lon, lat, null);
        Assert.assertFalse(index != -1);

        latitude = 27.9989;
        longitude = -82.7035;
        lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        index = rtree.search(lon, lat, null);
        Assert.assertFalse(index != -1);

    }

    @Test
    public void testNearbyPolygonsWithHighAttributeIndex() throws Exception {
        final String FILENAME = "Wld_Town_Polygon_Subset.shp";
        final String ATTRIBUTES_INDEX_NAME = "WOE_ID";
        ShapeFileReader reader;
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        final int startIndex = 100;
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null, startIndex);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);
        reader.process();

        List<Polygon> polygons = reader.getPolygons();
        HashMap<Integer, String> attributesIndexMap = attributeIndexer.getAttributesIndexMap();
        int maxChildren = 3;
        RTree rtree = new RTree(maxChildren);
        rtree.buildIndex(reader.getPolygons());

        System.out.println("number of polygons:" + polygons.size());
        System.out.println("index size:" + attributesIndexMap.size());
        Assert.assertEquals(polygons.size(), 20);
        Assert.assertEquals(attributesIndexMap.size(), 19);
        int[] searchResults = new int[10];
        Arrays.fill(searchResults, -1);
        int radius = 5000; //in meters
        int accuracy = 10; //in meters

        double latitude = 38.27913;
        double longitude = -85.737470;
        int lon = (int) (longitude * StorageConstants.ACCURACY_FACTOR);
        int lat = (int) (latitude * StorageConstants.ACCURACY_FACTOR);
        rtree.searchNearby(lon, lat, accuracy, radius, searchResults, null);

        //attributeIndex returned should be greater than or equal to start index
        Assert.assertTrue(searchResults[1] >= startIndex);
    }
}
