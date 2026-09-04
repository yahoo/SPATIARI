package com.yahoo.geoinformatics.polygon_lookup.spatial;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;


/**
 * @author koushikm
 */
public class SpatialLookupTest {

    private static final String DATAPACK_NAME = "Test_region.datapack";
    private SpatialIndexer indexer;
    private SpatialLookup lookupHelper;

    @BeforeClass
    public void setup() throws Exception {
        indexer = new SpatialIndexer();
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(DATAPACK_NAME);
        if (null == url) {
            throw new Exception("File " + DATAPACK_NAME + " Not Found.");
        }

        lookupHelper = indexer.buildIndexFromDatapack(url.getFile(),false);
    }

    @Test
    public void testBuildIndexFromDatapackWithNeutralFlag(){
        String ff = "src/test/resources/languageAgnostistic.dp";
        SpatialIndexer indexer = new SpatialIndexer();
        SpatialLookup lookup = indexer.buildIndexFromDatapack(ff, true);
        int num = lookup.search(31.9522,35.2332, null);
        Assert.assertTrue(num > 0);
    }

    @Test
    public void testSearch() {
        Assert.assertTrue(lookupHelper != null);
        int result = lookupHelper.search(37.5846, -121.9826, null);
        Assert.assertTrue(result > 0);
    }

    @Test
    public void testSearchFailure() {
        int result = lookupHelper.search(37.6079, -121.9567, null);
        Assert.assertFalse(result > 0);
    }

    @Test
    public void testNearbySearch() {
        int[] results = new int[100];
        lookupHelper.searchNearby(37.6517, -121.9500, 5, 100, results, null);
        Assert.assertTrue(results[0] > 0);
        for (int i = 0; i < results.length; ++i) {
            if ((results[i]) < 0) {
                break;
            }
            System.out.println("value =" + results[i]);
        }
    }


    @Test
    public void testDatapackWithRadius() throws Exception {
        /* This datapack is built from pointFileRadius.shp file which contains point data with Radius attribute */
        String datapackName = "pointFileRadius.dp";
        ClassLoader classLoader = getClass().getClassLoader();
        URL pointUrl = classLoader.getResource(datapackName);
        if (null == pointUrl) {
            throw new Exception("File " + datapackName + " Not Found.");
        }
        SpatialIndexer indexer = new SpatialIndexer();
        SpatialLookup lookup = indexer.buildIndexFromDatapack(pointUrl.getFile(), false);
        int[] results = new int[100];
        int offset = 10;
        lookup.searchNearby(36.05, -80.28, 300, 5000, results, offset, null);
        int count = 0;
        for(int i=0; i < results.length; i += 10, ++count) {
            if(results[i+1] < 0) {
                break;
            }
        }
        Assert.assertTrue(count == 1);
    }
}
