package com.yahoo.geoinformatics.polygon_lookup.spatial;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;


/**
 * Temporary class to test BDAI credit-suisse data.
 */

public class BdaiDataTester {
    private static final String INPUT_POLYGON_FILENAME = "etlDictionaryPolygon.shp";
    private static final String INPUT_POINT_FILENAME = "etlDictionaryPoint.shp";
    private static final String INPUT_RADIUS_FILENAME = "CA_Radius_File.csv";
    private static final String OUTPUT_DATAPACK_FILENAME = "etlDictionary_bdai.dp";
    private static final String OUTPUT_ATTRIBUTES_FILENAME = "etlDictionary_bdai.attributes";
    private static final String TEST_DATA_FILE = "LOCUS1.csv";
    private HashMap<Integer, String> attributes;
    private Set<String> inputIds;


    public void buildDatapack() throws Exception {
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
        HashMapBasedAttributeIndexer attributeIndexer = new HashMapBasedAttributeIndexer("POI_ID");
        SpatialIndexer indexer = new SpatialIndexer();
        System.out.println("Going to build datapack");
        indexer.buildIndexFromShapes(urlPolygon.getFile(), urlPoint.getFile(), radiusFileUrl.getFile(),
                OUTPUT_DATAPACK_FILENAME, attributeIndexer, false);
        System.out.println("Done with shapefile processing");
        attributes = attributeIndexer.getIndex();
        try (ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(OUTPUT_ATTRIBUTES_FILENAME))) {
            outStream.writeObject(attributes);
        }
        System.out.println("Done with shapefile saving");
    }

    public void testDatapack() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        //read attributes
        URL urlAttributes = classLoader.getResource(OUTPUT_ATTRIBUTES_FILENAME);
        if (null == urlAttributes) {
            throw new Exception("File " + OUTPUT_ATTRIBUTES_FILENAME + " Not Found.");
        }
        try (ObjectInputStream inStream =
                     new ObjectInputStream(new BufferedInputStream(new FileInputStream(urlAttributes.getFile())))) {

            attributes = (HashMap<Integer, String>) inStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Couldn't understand file.", e);
        }

        // read from datapack
        SpatialIndexer indexer = new SpatialIndexer();
        URL urlDatapack = classLoader.getResource(OUTPUT_DATAPACK_FILENAME);
        if (null == urlDatapack) {
            throw new Exception("File " + OUTPUT_DATAPACK_FILENAME + " Not Found.");
        }
        SpatialLookup lookupHelper = indexer.buildIndexFromDatapack(urlDatapack.getFile(), false);

        // distance, id, confidence, radius_of_influence, poi_coverage
        int[] results = new int[200];
        int offset = 5;
        int searchRadius = 150;
        double lat = 26.70534; //40.77809;
        double lon = -80.13062; //-89.62208;
        int radius = 298;
        lookupHelper.searchNearby(lat, lon, radius, searchRadius, results, offset, null);
        int total = 0;
        for(int i=0; i < results.length; i += offset) {
            if(results[i+1] < 0) {
                break;
            }
            ++total;
            String poiId = attributes.get(results[i + 1]);
            int runningId = results[i+1];
            int distance = results[i];
            int confidence = results[i + 2];
            int roi = results[i + 3];
            int poiCoverage = results[i + 4];
            System.out.println(runningId + "," +lat +  "," + lon + "," + radius + "," + distance + "," + poiId + "," + confidence + "," + roi + "," + poiCoverage + "\n");
        }
        System.out.println("total =" + total);
    }
    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        BdaiDataTester tester = new BdaiDataTester();
        //tester.buildDatapack();
        System.out.println("Done with datapack building");
        System.out.println("Start testing datapack");
        tester.testDatapack();
        System.out.println("Done testing datapack");
    }
}
