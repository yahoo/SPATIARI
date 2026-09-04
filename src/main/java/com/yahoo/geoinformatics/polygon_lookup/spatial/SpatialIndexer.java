package com.yahoo.geoinformatics.polygon_lookup.spatial;

import java.io.IOException;
import java.util.List;

import com.yahoo.geoinformatics.polygon_lookup.datastore.Storage;
import com.yahoo.geoinformatics.polygon_lookup.reader.ShapeFileReader;
import com.yahoo.geoinformatics.polygon_lookup.reader.StorageDimensions;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
import com.yahoo.geoinformatics.polygon_lookup.reader.TextFileReader;
import com.yahoo.geoinformatics.polygon_lookup.rtree.RTree;
import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class is responsible for building index whether from spatial data or already stored datapack. Currently,it uses
 * R-Tree internally to build the index. It hides details about how the indexing is done from clients.
 *
 * @author koushikm
 */

public class SpatialIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpatialIndexer.class);

    // NOTE: 7 looks a good number now, but we don't have a science yet to quantify it
    private static final int MAX_CHILDREN = 7;
    //NOTE: 25 is the children count we use with business-listing data, so replicating it
    private static final int MAX_ZIP4_CHILDREN = 25;

    /**
     * Given a shapefile, this method index the shapes. It then stores the shapes, index, and a running number for each
     * shape.
     *
     * @param inputPolygonsFile Esri shapefile name containing polygon data
     * @param inputPointsFile   Esri shapefile name containing point data
     * @param radiusFile        file containing pre-calculated radius for points data
     * @param outputFile        datapack file name to store the resultant index and data
     * @param indexer           attribute indexer (entry-id sequencing and polygon attribute indices via
     *                          {@link AttributeIndexer#getNextIndex(org.opengis.feature.simple.SimpleFeature)})
     * @param storageNeutral    True if the user wants language-neutral way of storing the index and data
     * @throws Exception throws exception occured when building and storing index
     */
    public void buildIndexFromShapes(String inputPolygonsFile, String inputPointsFile, String radiusFile,
                                     String outputFile, AttributeIndexer indexer, boolean storageNeutral) throws Exception {
        ShapeFileReader reader = new ShapeFileReader(inputPolygonsFile, inputPointsFile, radiusFile, indexer);

        StorageDimensions dimensions = reader.computeStorageDimensions();
        indexer.init(dimensions.getTotalEntries());

        reader.process();
        List<Polygon> polygons = reader.getPolygons();

        RTree rtree = new RTree(MAX_CHILDREN);
        rtree.buildIndex(polygons);

        rtree.storeIndex(outputFile, storageNeutral);

    }

    public void buildIndexFromTextFiles(String inputPath, String extension, String outputFile,
                                        AttributeIndexer indexer, int startIndex) throws Exception {
        TextFileReader reader = new TextFileReader(inputPath, extension, indexer, startIndex);

        int entries = reader.computeEntries();
        indexer.init(entries);

        reader.process();
        List<Polygon> polygons = reader.getPolygons();

        RTree rtree = new RTree(MAX_ZIP4_CHILDREN);
        rtree.buildIndex(polygons);

        rtree.storeIndex(outputFile, false);

    }

    /**
     * @param datapackFile datapack file containing index,and data
     * @param storageNeutral : true for Java independent datapack creation, false to use Java technology to create datapack
     * @return an object which can be used for fast spatial lookup
     */
    public SpatialLookup buildIndexFromDatapack(final String datapackFile, final boolean storageNeutral) {
        // create R-Tree from storage file
        Storage newIndex;
        try {
            newIndex = Storage.loadFrom(datapackFile, storageNeutral);
        } catch (IOException e) {
            LOGGER.error("Exception occurred while building index from datapack {} ", datapackFile, e);
            return null;
        }
        RTree rtree = new RTree(newIndex);
        // create Lookup object from the indexed tree
        return new SpatialLookup(rtree);
    }
}
