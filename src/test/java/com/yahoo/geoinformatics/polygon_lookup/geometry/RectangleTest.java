package com.yahoo.geoinformatics.polygon_lookup.geometry;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author koushikm
 */
public class RectangleTest {

    @Test
    public void testRectangleConstruction() {
        Rectangle r = new Rectangle(-122012529, 37579692, -122006639, 37584467);

        Assert.assertTrue(r.getMinX() == -122012529);
        Assert.assertTrue(r.getMinY() == 37579692);
        Assert.assertTrue(r.getMaxX() == -122006639);
        Assert.assertTrue(r.getMaxY() == 37584467);
        Assert.assertFalse(r.toString().isEmpty());
    }

    @Test
    public void testRectangleExtension() {
        Rectangle r = new Rectangle(-122012529, 37579692, -122006639, 37584467);

        Rectangle r1 = r.extend(-120020262, 37465717);
        int[] p = new int[]{-121988369, 37684467};
        Rectangle r2 = r1.extend(p);

        Assert.assertTrue(r2.getMinX() == -122012529);
        Assert.assertTrue(r2.getMinY() == 37465717);
        Assert.assertTrue(r2.getMaxX() == -120020262);
        Assert.assertTrue(r2.getMaxY() == 37684467);
    }

    @Test
    public void testContainment() {
        Rectangle r = new Rectangle(10, 10, 20, 20);

        Assert.assertTrue(r.contains(15, 15)); //inside
        Assert.assertFalse(r.contains(30, 30)); //outside
    }

    @Test
    public void testUnion() {
        Rectangle r = new Rectangle(10, 10, 20, 20);

        Rectangle result = r.union(new Rectangle(15, 15, 30, 30));

        Assert.assertEquals(result.getMinX(), 10);
        Assert.assertEquals(result.getMinY(), 10);
        Assert.assertEquals(result.getMaxX(), 30);
        Assert.assertEquals(result.getMaxY(), 30);
    }
}
