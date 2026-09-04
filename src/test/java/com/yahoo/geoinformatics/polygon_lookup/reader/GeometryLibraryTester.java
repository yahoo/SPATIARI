package com.yahoo.geoinformatics.polygon_lookup.reader;

import java.io.File;
import java.net.URL;

import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Utility class to play around Geometry library we are using to read shape files
 *
 * @author koushikm
 */
public class GeometryLibraryTester {

    private static final String FILENAME = "US_TOWN_MULTIPLE_RINGS.shp";
    private static final String ATTRIBUTES_INDEX_NAME = "BLOCKID10";

    public void testMultipolygon() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(FILENAME);
        if (null == url) {
            throw new Exception("File " + FILENAME + " Not Found.");
        }
        File file = new File(url.getFile());
        DataStore dataStore = null;
        try {
            dataStore = new ShapefileDataStore(file.toURI().toURL());
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];
            FeatureIterator<SimpleFeature>
                iterator =
                dataStore.getFeatureSource(typeName).getFeatures().features();
            try {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    MultiPolygon multipolygon = (MultiPolygon) feature.getDefaultGeometry();
                    System.out.println("#polygons:" + multipolygon.getNumGeometries());
                    Geometry bb = multipolygon.getEnvelope();
                    System.out.println("#points in envelop:" + bb.getNumPoints());
                }
            } finally {
                iterator.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (dataStore != null) {
                dataStore.dispose();
            }
        }
    }

    /**
     * @throws Exception exception
     */
    public static void main(String[] args) throws Exception {
        GeometryLibraryTester libTester = new GeometryLibraryTester();
        libTester.testMultipolygon();
    }

}
