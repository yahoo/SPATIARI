package com.yahoo.geoinformatics.polygon_lookup.reader;

import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;


/**
 * @author koushikm
 */
public class ShapeFileReaderTest {

    private static final String FILENAME = "Test_region.shp";
    private static final String ATTRIBUTES_INDEX_NAME = "BLOCKID10";
    private ShapeFileReader reader;

    @Test
    public void testPolygonCount() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);
        reader.process();
        Assert.assertEquals(10, reader.getPolygons().size());
        Assert.assertFalse(reader.getPolygons().get(0).getAttributeIndex() == -1);
        Assert.assertTrue(reader.getPolygons().get(0).getOuterBoundaryPoints().length > 0);
        Assert.assertFalse(reader.getPolygons().get(0).getBoundingBox() == null);
        Assert.assertFalse(reader.toString().isEmpty());
    }

    @Test
    public void testLoadingShapesFromValidDirectory() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("census_blocks");
        if (null == url) {
            throw new Exception("unable to read current directory.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);
        reader.process();
        Assert.assertTrue(reader.getPolygons().size() > 0);
        Assert.assertFalse(reader.getPolygons().get(0).getAttributeIndex() == -1);
        Assert.assertTrue(reader.getPolygons().get(0).getOuterBoundaryPoints().length > 0);
        Assert.assertFalse(reader.getPolygons().get(0).getBoundingBox() == null);
        Assert.assertFalse(reader.toString().isEmpty());
    }

    @Test(expectedExceptions = Exception.class)
    public void testLoadingShapesFromInvalidDirectory() throws Exception {
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, null);
        reader = new ShapeFileReader("./census_blocks_1", null, null, attributeIndexer);
        reader.process();
    }

    @Test(expectedExceptions = Exception.class)
    public void testWithInvalidIndexName() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest("dummy", null);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);
        reader.process();
    }

    @Test
    public void testLoadingShapesAndAttributes() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("census_blocks");
        if (null == url) {
            throw new Exception("unable to read current directory.");
        }
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("POP10");
        attributes.add("HOUSING10");

        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(ATTRIBUTES_INDEX_NAME, attributes);
        reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);

        reader.process();
        Assert.assertTrue(attributeIndexer.getAttributeValuesMap().size() > 0);
        Assert.assertTrue(reader.getPolygons().size() > 0);
        Assert.assertFalse(reader.getPolygons().get(0).getAttributeIndex() == -1);
        Assert.assertTrue(reader.getPolygons().get(0).getOuterBoundaryPoints().length > 0);
        Assert.assertFalse(reader.getPolygons().get(0).getBoundingBox() == null);
        Assert.assertFalse(reader.toString().isEmpty());
    }

    @Test
    public void testPolygonAndPointCount() throws Exception {
        String polygonShapeFile = "Wld_Town_Polygon_Subset.shp";
        String pointShapeFile = "Wld_Town_Point_Subset.shp";
        String radiusFile = "Wld_Town_Point_Subset_Radius_File.csv";
        String attributeIndexName = "WOE_ID";
        ClassLoader classLoader = getClass().getClassLoader();
        URL polygonUrl = classLoader.getResource(polygonShapeFile);
        if (null == polygonUrl) {
            throw new Exception("File " + polygonShapeFile + " Not Found.");
        }
        URL pointUrl = classLoader.getResource(pointShapeFile);
        if (null == pointUrl) {
            throw new Exception("File " + pointShapeFile + " Not Found.");
        }
        URL radiusFileUrl = classLoader.getResource(radiusFile);
        if (null == radiusFileUrl) {
            throw new Exception("File " + radiusFile + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(attributeIndexName, null);
        reader =
            new ShapeFileReader(polygonUrl.getFile(), pointUrl.getFile(), radiusFileUrl.getFile(), attributeIndexer);
        reader.process();
        Assert.assertEquals(38, reader.getPolygons().size());
        Assert.assertFalse(reader.getPolygons().get(0).getAttributeIndex() == -1);
        Assert.assertTrue(reader.getPolygons().get(0).getOuterBoundaryPoints().length > 0);
        Assert.assertFalse(reader.getPolygons().get(0).getBoundingBox() == null);
        Assert.assertFalse(reader.toString().isEmpty());
    }

    @Test
    public void testPointCount() throws Exception {
        /* testing a point file which does not have POINT geometry, so centroid lat/long are extracted from feature list */
        String pointShapeFile = "Wld_Town_Point_Subset.shp";
        String radiusFile = "Wld_Town_Point_Subset_Radius_File.csv";
        String attributeIndexName = "WOE_ID";
        ClassLoader classLoader = getClass().getClassLoader();

        URL pointUrl = classLoader.getResource(pointShapeFile);
        if (null == pointUrl) {
            throw new Exception("File " + pointShapeFile + " Not Found.");
        }
        URL radiusFileUrl = classLoader.getResource(radiusFile);
        if (null == radiusFileUrl) {
            throw new Exception("File " + radiusFile + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(attributeIndexName, null);
        reader =
                new ShapeFileReader(null, pointUrl.getFile(), radiusFileUrl.getFile(), attributeIndexer);
        reader.process();
        Assert.assertEquals(18, reader.getPolygons().size());
    }

    @Test
    public void testPointCountPointGeometry() throws Exception {
        /* testing a point shapefile which have point geometry */
        String pointShapeFile = "br.shp";
        String radiusFile = "Wld_Town_Point_Subset_Radius_File.csv";
        String attributeIndexName = "store zip";
        ClassLoader classLoader = getClass().getClassLoader();

        URL pointUrl = classLoader.getResource(pointShapeFile);
        if (null == pointUrl) {
            throw new Exception("File " + pointShapeFile + " Not Found.");
        }
        URL radiusFileUrl = classLoader.getResource(radiusFile);
        if (null == radiusFileUrl) {
            throw new Exception("File " + radiusFile + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(attributeIndexName, null);
        reader =
                new ShapeFileReader(null, pointUrl.getFile(), radiusFileUrl.getFile(), attributeIndexer);
        reader.process();
        Assert.assertEquals(56, reader.getPolygons().size());
    }

    @Test
    public void testPointFileWithRadiusAttributes() throws Exception {
        /* testing a point shapefile which have separate Radius attribute per location */
        String pointShapeFile = "pointFileRadius.shx";
        String radiusFile = "default_radius_file.csv";
        String attributeIndexName = "screenId";
        ClassLoader classLoader = getClass().getClassLoader();
        URL pointUrl = classLoader.getResource(pointShapeFile);
        if (null == pointUrl) {
            throw new Exception("File " + pointShapeFile + " Not Found.");
        }
        URL radiusFileUrl = classLoader.getResource(radiusFile);
        if (null == radiusFileUrl) {
            throw new Exception("File " + radiusFile + " Not Found.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(attributeIndexName, null);
        reader =
                new ShapeFileReader(null, pointUrl.getFile(), radiusFileUrl.getFile(), attributeIndexer);
        reader.process();
        Assert.assertEquals(16, reader.getPolygons().size());
    }
}


