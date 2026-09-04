package com.yahoo.geoinformatics.polygon_lookup.spatial;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;

import com.yahoo.geoinformatics.polygon_lookup.reader.ShapeFileReader;
import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexerTest;

/**
 * @author koushikm
 */
public class SpatialIndexerTest {

    private static final String FILENAME = "Test_region.shp";
    private static final String ATTRIBUTES_INDEX_NAME = "BLOCKID10";
    private static final String OUTPUT_DATAPACK = "Test_region.dp";
    private static final String DATAPACK_NAME = "Test_region.datapack";
    private SpatialIndexer indexer;

    @BeforeClass
    public void setup() {
        indexer = new SpatialIndexer();
    }

    @Test
    public void testIndexing() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        indexer.buildIndexFromShapes(url.getFile(), null, null, OUTPUT_DATAPACK, attributeIndexer, false);
        Assert.assertEquals(attributeIndexer.getAttributesIndexMap().size(), 10);
    }

    @Test
    public void testIndexingWithAttributes() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("census_blocks");
        if (null == url) {
            throw new Exception("File census_blocks not Found.");
        }
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("POP10");
        attributes.add("HOUSING10");

        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, attributes);
        indexer.buildIndexFromShapes(url.getFile(), null, null, OUTPUT_DATAPACK, attributeIndexer, false);
        Assert.assertTrue(attributeIndexer.getAttributesIndexMap().size() > 0);
        Assert.assertTrue(attributeIndexer.getAttributeValuesMap().size() > 0);
    }

    @Test(expectedExceptions = Exception.class)
    public void testIndexingFailure() throws Exception {
        String nonExistentFile = "Non_Existent.shp";
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        indexer.buildIndexFromShapes(nonExistentFile, null, null, OUTPUT_DATAPACK, attributeIndexer, false);
        Assert.assertTrue(attributeIndexer.getAttributesIndexMap() == null);
    }

    @Test
    public void testLoading() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(DATAPACK_NAME);
        if (null == url) {
            throw new Exception("File " + DATAPACK_NAME + " Not Found.");
        }
        SpatialLookup lookupHelper = indexer.buildIndexFromDatapack(url.getFile(),false);
        Assert.assertTrue(lookupHelper != null);
    }

    @Test
    public void testLoadingLanguageNuetral(){
        String ff = "src/test/resources/languageAgnostistic.dp";
        SpatialIndexer indexer = new SpatialIndexer();
        SpatialLookup lookup = indexer.buildIndexFromDatapack(ff, true);
        Assert.assertTrue(lookup != null);
    }
}
