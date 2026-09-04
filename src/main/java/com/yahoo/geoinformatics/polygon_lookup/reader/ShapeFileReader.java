package com.yahoo.geoinformatics.polygon_lookup.reader;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;
import com.yahoo.geoinformatics.polygon_lookup.geometry.GeometricAlgorithms;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Rectangle;
import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexer;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class for reading shape files and save them as list of polygons
 *
 * @author koushikm
 */
public final class ShapeFileReader {

    private static final String SHP_EXTENSION = ".shp";
    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeFileReader.class);

    private final String shapeFile;
    private final String pointShapeFile;
    private final String radiusFile;
    private final List<Polygon> polygons;
    private AttributeIndexer indexer;
    private int totalPolygons;
    private int totalRadius;

    public ShapeFileReader(String polygonShapeFile, String pointShapeFile, String radiusFile, AttributeIndexer indexer) {
        this.shapeFile = polygonShapeFile;
        this.pointShapeFile = pointShapeFile;
        this.radiusFile = radiusFile;
        this.indexer = indexer;
        this.polygons = new ArrayList<>();
        totalPolygons = 0;
        totalRadius = 0;
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }


    /**
     * Given a shape file, compute storage dimensions for indexing
     *
     * @return StorageDimensions computed storage dimensions for indexing
     * @throws IOException throws IOException when reading shape files
     */
    public StorageDimensions computeStorageDimensions() throws IOException {
        StorageDimensions dimensions = new StorageDimensions();
        if (shapeFile != null) {
            Files.find(Paths.get(shapeFile), Integer.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(SHP_EXTENSION))
                    .forEach(path -> {
                        LOGGER.info("Processing Shape File : {}", path);
                        DataStore dataStore = null;
                        try {
                            dataStore = new ShapefileDataStore(path.toUri().toURL());
                            String[] typeNames = dataStore.getTypeNames();
                            String typeName = typeNames[0];
                            FeatureIterator<SimpleFeature> iterator =
                                    dataStore.getFeatureSource(typeName).getFeatures().features();
                            try {
                                while (iterator.hasNext()) {
                                    SimpleFeature feature = iterator.next();
                                    dimensions.incrementTotalEntries(1);
                                    MultiPolygon multipolygon = (MultiPolygon) feature.getDefaultGeometry();
                                    dimensions.incrementTotalPolygons(multipolygon.getNumGeometries());
                                    for (int polygonIndex = 0; polygonIndex < multipolygon
                                            .getNumGeometries(); ++polygonIndex) {
                                        org.locationtech.jts.geom.Polygon polygon =
                                                (org.locationtech.jts.geom.Polygon) multipolygon
                                                        .getGeometryN(polygonIndex);
                                        dimensions.incrementTotalPoints(polygon.getNumPoints());
                                        dimensions.incrementTotalRings(polygon.getNumInteriorRing());
                                    }
                                }
                            } finally {
                                iterator.close();
                            }
                        } catch (Throwable e) {
                            LOGGER.error("Exception occurred when processing shape file {}", path, e);
                            throw new RuntimeException(e);
                        } finally {
                            if (dataStore != null) {
                                dataStore.dispose();
                            }
                        }
                    });
        }
        if (pointShapeFile != null) {
            File file = new File(pointShapeFile);
            DataStore dataStore = null;
            try {
                dataStore = new ShapefileDataStore(file.toURI().toURL());
                String[] typeNames = dataStore.getTypeNames();
                String typeName = typeNames[0];
                FeatureIterator<SimpleFeature> iterator = dataStore.getFeatureSource(typeName).getFeatures().features();
                try {
                    while (iterator.hasNext()) {
                        iterator.next();
                        dimensions.incrementTotalEntries(1);
                    }
                } finally {
                    iterator.close();
                }
            } catch (Throwable e) {
                e.printStackTrace();
                LOGGER.error("Exception occurred when processing shape file(s) {}", pointShapeFile, e);
                throw new RuntimeException(e);
            } finally {
                if (dataStore != null) {
                    dataStore.dispose();
                }
            }
        }
        return dimensions;
    }

    /**
     * Given a shape file, process them into internal data structure before indexing
     */
    private void processShapeFile(Path path) {
        LOGGER.info("Processing Shape File : {}", path);
        DataStore dataStore = null;
        try {
            dataStore = new ShapefileDataStore(path.toUri().toURL());
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];
            FeatureIterator<SimpleFeature> iterator = dataStore.getFeatureSource(typeName).getFeatures().features();
            try {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    MultiPolygon multipolygon = (MultiPolygon) feature.getDefaultGeometry();
                    processMultiPolygon(multipolygon, feature);
                }
            } finally {
                iterator.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            LOGGER.error("Exception occurred when processing shape file(s) {}", path, e);
            throw new RuntimeException(e);
        } finally {
            if (dataStore != null) {
                dataStore.dispose();
            }
        }
    }

    /**
     * Given a shapefile / directory, process them into internal data structure before indexing
     *
     * @throws Exception throws an exception if unsuccessful
     */
    public void process() throws Exception {
        // process polygon shapefiles first - this can be a single file or a directory
        if (shapeFile != null) {
            try {
                Files.find(Paths.get(shapeFile), Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile()
                                && filePath.toString().endsWith(SHP_EXTENSION))
                        .forEach(path -> this.processShapeFile(path));
            } catch (Exception ex) {
                LOGGER.error("Exception occurred when processing shape file(s) {}", shapeFile, ex);
                throw ex;
            }

            if (getPolygons().isEmpty()) {
                throw new Exception("No polygons were extracted from shape file(s) " + shapeFile);
            }
        }
        // process point shape file next
        processPointShapeFile();
    }

    private void processPointShapeFile() throws IOException {
        if ((pointShapeFile == null) || (radiusFile == null)) {
            return;
        }
        int pointCount = 0;
        int missingPoint = 0;
        // get the average radius per iso
        BufferedReader br = Files.newBufferedReader(Paths.get(radiusFile), StandardCharsets.UTF_8);
        Map<String, Integer> isoToRadius = new HashMap<>();
        boolean firstline = true;
        String line;
        while ((line = br.readLine()) != null) {
            if (firstline) {
                firstline = false;
                continue;
            }
            String[] tokens = line.split(",");
            String iso = tokens[0];
            int radius = Integer.valueOf(tokens[1]);
            isoToRadius.put(iso, radius);
        }

        File file = new File(pointShapeFile);
        DataStore dataStore = null;
        try {
            dataStore = new ShapefileDataStore(file.toURI().toURL());
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];
            FeatureIterator<SimpleFeature> iterator = dataStore.getFeatureSource(typeName).getFeatures().features();
            try {
                while (iterator.hasNext()) {
                    ++pointCount;
                    int[] boundaryPoints = new int[4];
                    SimpleFeature feature = iterator.next();
                    int x = 0, y = 0;
                    if (feature.getDefaultGeometry() instanceof  org.locationtech.jts.geom.Point) {
                        /* if point geometry is available, that's first choice to get the geometry data */
                        org.locationtech.jts.geom.Point point = (org.locationtech.jts.geom.Point) feature.getDefaultGeometry();
                        if (point == null) {
                            ++missingPoint;
                            continue;
                        }
                        x = (int) (point.getX() * StorageConstants.ACCURACY_FACTOR);
                        y = (int) (point.getY() * StorageConstants.ACCURACY_FACTOR);
                    } else {
                        /* sometime we don't have POINT object in geometry, in such case we take centroid lat/long, this is particularly true from point data from wherehause */

                                x =
                                (int) (Float.parseFloat(feature.getAttribute("Long").toString())
                                        * StorageConstants.ACCURACY_FACTOR);

                                y =
                                (int) (Float.parseFloat(feature.getAttribute("Lat").toString())
                                        * StorageConstants.ACCURACY_FACTOR);
                    }
                    boundaryPoints[0] = boundaryPoints[2] = x;
                    boundaryPoints[1] = boundaryPoints[3] = y;

                    int radius = 0;
                    if (feature.getAttribute(StorageConstants.RADIUS_ATTRIBUTE_NAME) != null) {
                        radius = Integer.parseInt(feature.getAttribute(StorageConstants.RADIUS_ATTRIBUTE_NAME).toString());
                    } else if (feature.getAttribute("ISO") != null) {
                        String iso = feature.getAttribute("ISO").toString();
                        if (isoToRadius.get(iso) != null) {
                            radius = isoToRadius.get(iso);
                        } else {
                            radius = isoToRadius.get(StorageConstants.DEFAULT_ISO_NAME);
                        }
                    } else {
                        radius = isoToRadius.get(StorageConstants.DEFAULT_ISO_NAME);
                    }
                    int pointPolygonIndex = indexer.getNextIndex(feature);
                    polygons.add(new Polygon(pointPolygonIndex, boundaryPoints, null, 0, x, y, radius));
                    indexer.addAttributeValues(pointPolygonIndex, feature);
                }
            } finally {
                iterator.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            LOGGER.error("Exception occurred when processing shape file(s) {}", pointShapeFile, e);
            throw new RuntimeException(e);
        } finally {
            if (dataStore != null) {
                dataStore.dispose();
            }
        }
        System.out.println("point count =" + pointCount + " missing points=" + missingPoint);
    }

    /**
     * A multi-polygon may contain multiple parts and even holes in them
     *
     * @param multipolygon input multiple polygon
     * @param feature attributes of the multi-polygon
     */
    private void processMultiPolygon(MultiPolygon multipolygon, SimpleFeature feature) throws Exception {

        int attributeIndexForPolygon = indexer.getNextIndex(feature);
        indexer.addAttributeValues(attributeIndexForPolygon, feature);

        int polygonParts = multipolygon.getNumGeometries();
        for (int i = 0; i < polygonParts; ++i) {
            org.locationtech.jts.geom.Polygon polygon =
                            (org.locationtech.jts.geom.Polygon) multipolygon.getGeometryN(i);
            int[] points = processPolygon(polygon); // outer boundary
            int[][] rings = processPolygonRings(polygon); // inner rings
            //Note: for area only relative value matters for comparison, however value has to be non-zero for effective comparison
            //area returned is in angle degrees, which leads to value zero, so we convert this to meter squared.
            //Given the unit in degrees, we convert it into radians and then multiply with Earth average radius to ge meter squared.
            int area = (int) (Math.toRadians(polygon.getArea()) * StorageConstants.AVERAGE_RADIUS_OF_EARTH_METERS);
            int centroidLon = (int) (polygon.getCentroid().getCoordinate().x * StorageConstants.ACCURACY_FACTOR);
            int centroidLat = (int) (polygon.getCentroid().getCoordinate().y * StorageConstants.ACCURACY_FACTOR);
            //if radius is available from data, then pick that up first, else calculate
            boolean radiusFromData = false; int radius = 0;
            if (feature.getAttribute(StorageConstants.RADIUS_ATTRIBUTE_NAME) != null) {
                radius = Integer.parseInt(feature.getAttribute(StorageConstants.RADIUS_ATTRIBUTE_NAME).toString());
                if (radius > 0) {
                    radiusFromData = true;
                }
            }
            if(!radiusFromData) {
                Rectangle r = Polygon.createBoundingBoxPolygon(points);
                radius = GeometricAlgorithms.distance(r.getMaxX(), r.getMaxY(), r.getMinX(), r.getMinY()) / 2;
            }
            polygons.add(new Polygon(attributeIndexForPolygon, points, rings, area, centroidLon, centroidLat, radius));
            ++totalPolygons;
            totalRadius += radius;
        }
    }

    /**
     * A polygon may contain one or more inner rings, in this method we are only processing the outer boundary
     *
     * @param polygon input polygon.
     * @return list of points that defines the boundary of this polygon
     */
    private int[] processPolygon(org.locationtech.jts.geom.Polygon polygon) {
        int[] boundaryPoints = new int[polygon.getExteriorRing().getNumPoints() * 2];
        int index = 0;
        for (Coordinate coordinate : polygon.getExteriorRing().getCoordinates()) {
            boundaryPoints[index++] = (int) (coordinate.x * StorageConstants.ACCURACY_FACTOR);
            boundaryPoints[index++] = (int) (coordinate.y * StorageConstants.ACCURACY_FACTOR);
        }
        return boundaryPoints;
    }

    /**
     * @param polygon input polygon.
     * @return list of inner rings if any, each inner ring is a list of point
     */
    private int[][] processPolygonRings(org.locationtech.jts.geom.Polygon polygon) {
        int ringCount = polygon.getNumInteriorRing();
        int[][] rings = null;
        if (ringCount > 0) {
            rings = new int[ringCount][];
        }
        for (int i = 0; i < ringCount; ++i) {
            int index = 0;
            rings[i] = new int[polygon.getInteriorRingN(i).getNumPoints() * 2];
            for (Coordinate coordinate : polygon.getInteriorRingN(i).getCoordinates()) {
                rings[i][index++] = (int) (coordinate.x * StorageConstants.ACCURACY_FACTOR);
                rings[i][index++] = (int) (coordinate.y * StorageConstants.ACCURACY_FACTOR);
            }
        }
        return rings;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Details of the processed data:\n");
        result.append("Shape file: ");
        result.append(shapeFile);
        result.append("\nNumber of polygons:");
        result.append(polygons.size());
        result.append("\n");
        for (Polygon p : polygons) {
            result.append(p);
            result.append("\n");
        }

        return result.toString();
    }
}
