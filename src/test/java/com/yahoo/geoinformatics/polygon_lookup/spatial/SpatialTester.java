package com.yahoo.geoinformatics.polygon_lookup.spatial;

import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexerTest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manual helper to sanity-check a shapefile: readability, shape count, attribute dump,
 * datapack load latency, and a couple of sample lookups.
 *
 * <p>Not a TestNG suite — run via {@code main}. Shapefiles can live under
 * {@code src/test/resources} or be passed as an absolute/relative path.
 */
public class SpatialTester {

    // Change if your shapefile is non-WOEID based
    private static final String ATTRIBUTES_INDEX_NAME = "NAME";
    private static final String SHAPEDATA_DUMP_FILE = "shape_file_data.txt";
    private static final String DATAPACK_NAME = "test.dp";

    private final SpatialIndexer indexer;
    private HashMap<Integer, String> attributesIndex;

    SpatialTester() {
        indexer = new SpatialIndexer();
    }

    private void testIndexing(String shapeFile) throws Exception {
        System.out.println("Starting indexing");

        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        indexer.buildIndexFromShapes(shapeFile, null, null, DATAPACK_NAME, attributeIndexer, true);
        attributesIndex = attributeIndexer.getAttributesIndexMap();
        System.out.println("Total shapes counted in: " + attributesIndex.size());
        System.out.println("Dumping of attribute START");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SHAPEDATA_DUMP_FILE))) {
            for (Map.Entry<Integer, String> entry : attributesIndex.entrySet()) {
                writer.write(entry.getKey() + " : " + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Dumping of attribute DONE");
    }

    private SpatialLookup testDatapackLoadingSpeed() throws Exception {
        Instant startTime = Instant.now();
        SpatialLookup lookup = indexer.buildIndexFromDatapack(DATAPACK_NAME, true);
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        System.out.println(duration);
        System.out.println("Latency: " + duration.toMillis() + " milliseconds");
        System.out.println("Latency: " + duration.getSeconds() + " seconds");
        return lookup;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -cp <test-classpath> "
                    + "com.yahoo.geoinformatics.polygon_lookup.spatial.SpatialTester <shapefile>");
            System.exit(1);
        }
        String shapeFile = args[0];
        System.out.println("Shapefile to be processed:" + shapeFile);
        SpatialTester spatialTester = new SpatialTester();

        spatialTester.testIndexing(shapeFile);

        SpatialLookup lookup = spatialTester.testDatapackLoadingSpeed();

        System.out.println("Looking up 40.7128, -74.0060");
        int id = lookup.search(40.7128, -74.0060, null);
        System.out.println("Found id: " + id);

        System.out.println("Looking up 37.3212092,-122.041911");
        id = lookup.search(37.3212092, -122.041911, null);
        System.out.println("Found id: " + id);

        System.out.println("Test complete");
    }
}
