package com.yahoo.geoinformatics.polygon_lookup.spatial;

import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexer;

public class HashMapBasedAttributeIndexer implements AttributeIndexer  {
    private HashMap<Integer, String> index;
    private String dataIndexName;
    private int nextEntryId;
    BufferedWriter writer;

    public HashMapBasedAttributeIndexer(String dataIndexName) throws IOException {
        this.dataIndexName = dataIndexName;
        writer = new BufferedWriter(new FileWriter("BDAI_CS_DATA.csv"));
    }

    public void init(int size) {
        index = new HashMap<>();
        nextEntryId = 0;
    }

    @Override
    public int getNextIndex(SimpleFeature feature) {
        return nextEntryId;
    }

    public void addAttributeValues(int runningIndex, SimpleFeature feature) throws Exception {
        if (feature.getAttribute(dataIndexName) != null) {
            String dataId = feature.getAttribute(dataIndexName).toString();
            if (dataId != null) {
                index.put(runningIndex, dataId);
                nextEntryId++;
            }
        }
    }

    public void printIndex() throws IOException {
        for(int i=0; i < index.size(); ++i) {
            writer.write(i + " = " + index.get(i) + "\n");
        }
    }
    public HashMap<Integer, String> getIndex() {
        return index;
    }
}
