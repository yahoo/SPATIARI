package com.yahoo.geoinformatics.polygon_lookup.index;

import com.yahoo.geoinformatics.polygon_lookup.reader.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.*;

public class AttributeIndexerTest implements AttributeIndexer {

    private HashMap<Integer, String> attributesIndexMap;

    private HashMap<Integer, Object[]> attributeValuesMap;

    private String dataIndexName;

    private List<String> attributesToExtract;

    int totalItems;

    private final int entryIdStart;
    private int nextEntryId;

    public AttributeIndexerTest() {
        // Default constructor required for testng class instantiations
        this.entryIdStart = 0;
        this.nextEntryId = 0;
    }

    public AttributeIndexerTest(List<String> attributes) {
        this.attributesToExtract = attributes;
        this.entryIdStart = 0;
        this.nextEntryId = 0;
    }

    public AttributeIndexerTest(String dataIndexName, List<String> attributesToExtract) {
        this(dataIndexName, attributesToExtract, 0);
    }

    public AttributeIndexerTest(String dataIndexName, List<String> attributesToExtract, int entryIdStart) {
        this.dataIndexName = dataIndexName;
        this.attributesToExtract = attributesToExtract;
        this.entryIdStart = entryIdStart;
        this.nextEntryId = entryIdStart;
        this.setAttributeValuesMap(new HashMap<>());
        this.setAttributesIndexMap(new HashMap<>());
    }

    public void init(int totalItems) {
        this.totalItems = totalItems;
        this.nextEntryId = entryIdStart;
    }

    @Override
    public int getNextIndex(SimpleFeature feature) {
        return nextEntryId;
    }

    public void addAttributeValues(int index, SimpleFeature feature) throws Exception {
        if (index != nextEntryId) {
            throw new IllegalStateException("expected entry id " + nextEntryId + " but got " + index);
        }
        Object featureAttribute = feature.getAttribute(dataIndexName);
        if (featureAttribute == null) {
            throw new Exception("index attribute name has null value " + index);
        }

        String dataId = featureAttribute.toString();
        getAttributesIndexMap().put(index, dataId);

        if (attributesToExtract != null) {
            int attributeIndex = 0;
            Object[] values = new Object[attributesToExtract.size()];
            for (String attributeName : attributesToExtract) {
                values[attributeIndex++] = feature.getAttribute(attributeName);
            }
            getAttributeValuesMap().put(index, values);
        }

        nextEntryId++;
    }

    //Used for TextFileReader
    public void addAttributeValues(int index, Map<String, String > attributes) {
        System.out.println("index = " + index );
        attributes.forEach((key, value) -> System.out.println("key:" + key + " value:" + value));
    }

    public HashMap<Integer, String> getAttributesIndexMap() {
        return attributesIndexMap;
    }

    public void setAttributesIndexMap(HashMap<Integer, String> attributesIndexMap) {
        this.attributesIndexMap = attributesIndexMap;
    }

    public HashMap<Integer, Object[]> getAttributeValuesMap() {
        return attributeValuesMap;
    }

    public void setAttributeValuesMap(HashMap<Integer, Object[]> attributeValuesMap) {
        this.attributeValuesMap = attributeValuesMap;
    }

    @Test
    public static void testLoadingShapesAndAttributes() throws Exception {
        ClassLoader classLoader = AttributeIndexerTest.class.getClassLoader();
        URL url = classLoader.getResource("census_blocks");
        if (null == url) {
            throw new Exception("unable to read current directory.");
        }
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("POP10");
        attributes.add("HOUSING10");

        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest("BLOCKID10", attributes);
        ShapeFileReader reader = new ShapeFileReader(url.getFile(), null, null, attributeIndexer);

        reader.process();
        Assert.assertTrue(attributeIndexer.getAttributeValuesMap().size() > 0);
        Assert.assertTrue(attributeIndexer.getAttributesIndexMap().size() > 0);
    }

}
