package com.yahoo.geoinformatics.polygon_lookup.reader;

import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URL;

public class TextFileReaderTester {
    private TextFileReader reader;
    @Test
    public void testReadingFromValidDirectory() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("zip_plus_four");
        if (null == url) {
            throw new Exception("unable to read current directory.");
        }
        AttributeIndexerTest attributeIndexer = new AttributeIndexerTest(null);
        reader = new TextFileReader(url.getFile(), ".txt", attributeIndexer, 0);
        reader.process();
        Assert.assertTrue(reader.getPolygons().size() > 0);
        Assert.assertFalse(reader.getPolygons().get(0).getAttributeIndex() == -1);
        Assert.assertTrue(reader.getPolygons().get(0).getOuterBoundaryPoints().length > 0);
        Assert.assertFalse(reader.getPolygons().get(0).getBoundingBox() == null);
        Assert.assertFalse(reader.toString().isEmpty());
    }
}
