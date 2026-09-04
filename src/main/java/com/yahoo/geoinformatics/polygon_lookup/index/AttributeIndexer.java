package com.yahoo.geoinformatics.polygon_lookup.index;

import org.opengis.feature.simple.SimpleFeature;
import java.util.Map;

/**
 * Interface for indexing attribute data associated with spatial features.
 * Implementations of this interface should provide methods to initialize the indexer
 * and to add attribute values from spatial features.
 *
 */
public interface AttributeIndexer {

    void init(int totalItems);

    void addAttributeValues(int index, SimpleFeature feature) throws Exception;

    /**
     * Polygon attribute index stored in the spatial index for this shapefile feature (tree node / polygon record).
     * Typically the sequential entry id; implementations may instead return a value read from the feature (e.g.
     * {@code WOE_ID}). Call once per feature before {@link #addAttributeValues(int, SimpleFeature)} for that feature.
     */
    int getNextIndex(SimpleFeature feature);

    //default method for backward compatibility
    //This way we don't need to implement this new method for all existing implementation
    default void addAttributeValues(int index, Map<String, String > attributes) {
        System.out.println("This is default implementation, doing nothing");
    }
}
