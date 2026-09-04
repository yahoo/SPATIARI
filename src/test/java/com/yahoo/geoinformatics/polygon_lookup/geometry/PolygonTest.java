package com.yahoo.geoinformatics.polygon_lookup.geometry;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class PolygonTest {

    private Polygon polygon;

    @BeforeClass
    public void setup() {
        int[]
            points =
            new int[]{-1221298, 3768244, -1221336, 3768090, -1221336, 3768095, -1221298, 3768250, -1221298, 3768245};
        int attributesId = 3001;
        int area = 100;
        int radius = 10;
        polygon = new Polygon(attributesId, points, null, area, -1221319, 3768150, radius);
    }

    @Test
    public void testPolygonConstruction() {
        Assert.assertEquals(5, polygon.getOuterBoundaryPoints().length / 2);
        Assert.assertEquals(3001, polygon.getAttributeIndex());
        Assert.assertFalse(polygon.toString().isEmpty());
        Assert.assertEquals(polygon.getArea(), 100);
        Assert.assertEquals(polygon.getCentroidX(), -1221319);
        Assert.assertEquals(polygon.getCentroidY(), 3768150);
    }

    @Test
    public void testBoundingBox() {
        Assert.assertEquals(polygon.getBoundingBox().getMinX(), -1221336);
        Assert.assertEquals(polygon.getBoundingBox().getMinY(), 3768090);
        Assert.assertEquals(polygon.getBoundingBox().getMaxX(), -1221298);
        Assert.assertEquals(polygon.getBoundingBox().getMaxY(), 3768250);
    }

    @Test
    public void testPolygon() {
        int[] points = new int[]{5, 10, 7, 5, 15, 6, 20, 13, 10, 20};
        int attributesId = 3001;
        Polygon p = new Polygon(attributesId, points, null, 100, 10, 10, 10);

        Assert.assertTrue(p.getOuterBoundaryPoints().length == 10);
    }

    @Test
    public void testRadius() {
        //double[] dpoints = new double[]{-84.436549, 42.250803, -84.436551, 42.250744, -84.436033, 42.250746, -84.436033, 42.250507, -84.435971, 42.250508, -84.435969, 42.250416, -84.436033, 42.250416, -84.436041, 42.250176, -84.436241, 42.250175, -84.436266, 42.250163, -84.436272, 42.25014, -84.435972, 42.250137, -84.43597, 42.250211, -84.43567, 42.250209, -84.435669, 42.250135, -84.435576, 42.250137, -84.435575, 42.250348, -84.435156, 42.250345, -84.435156, 42.25037, -84.435108, 42.250371, -84.435108, 42.250341, -84.434945, 42.250341, -84.434948, 42.25051, -84.434805, 42.250511, -84.4348, 42.250339, -84.433669, 42.250338, -84.433674, 42.250455, -84.433536, 42.250455, -84.433529, 42.249419, -84.434026, 42.249423, -84.434025, 42.249282, -84.434145, 42.249284, -84.434149, 42.249248, -84.434199, 42.249241, -84.434255, 42.249245, -84.434282, 42.249281, -84.435003, 42.249284, -84.434999, 42.249217, -84.435134, 42.249214, -84.435138, 42.249227, -84.435581, 42.249232, -84.435582, 42.249207, -84.43567, 42.249197, -84.435671, 42.249172, -84.435899, 42.249176, -84.435908, 42.249233, -84.436413, 42.249232, -84.436413, 42.249092, -84.436579, 42.249094, -84.436579, 42.249032, -84.436756, 42.249027, -84.436757, 42.24909, -84.436941, 42.249091, -84.436931, 42.24923, -84.437647, 42.249233, -84.437651, 42.249344, -84.437745, 42.249343, -84.437747, 42.249109, -84.438047, 42.249106, -84.43805, 42.249029, -84.438371, 42.249035, -84.438371, 42.249113, -84.43868, 42.249117, -84.438685, 42.249567, -84.438785, 42.249569, -84.438779, 42.24981, -84.438679, 42.24981, -84.438675, 42.250116, -84.438263, 42.250112, -84.438263, 42.250135, -84.438158, 42.250135, -84.438157, 42.250115, -84.437958, 42.250114, -84.437958, 42.250192, -84.437736, 42.250189, -84.437736, 42.250142, -84.437665, 42.250141, -84.437664, 42.250433, -84.437368, 42.250431, -84.43737, 42.250138, -84.436623, 42.250141, -84.436634, 42.250161, -84.43666, 42.250178, -84.437114, 42.250175, -84.437114, 42.250207, -84.437262, 42.250209, -84.437264, 42.250273, -84.43711, 42.250275, -84.437107, 42.250748, -84.436668, 42.250744, -84.436669, 42.250805, -84.436549, 42.250803};
       //[{"lon":[-89.622072],"lat":[40.778103]},{"lon":[-89.622073],"lat":[40.778089]},{"lon":[-89.622089],"lat":[40.778089]},{"lon":[-89.622089],"lat":[40.778093]},{"lon":[-89.622088],"lat":[40.778111]},{"lon":[-89.622072],"lat":[40.778111]},{"lon":[-89.622072],"lat":[40.778103]
        double[] dpoints = new double[]{-89.622072,40.778103,-89.622073,40.778089,-89.622089,40.778089,-89.622089,40.778093,-89.622088,40.778111,-89.622072,40.778111,-89.622072,40.778103};
        int[] points = new int[dpoints.length];
        System.out.println(dpoints.length);
        for(int i=0; i < dpoints.length; ++i) {
            points[i] =  (int)(dpoints[i] * StorageConstants.ACCURACY_FACTOR);
        }
        System.out.print("{");
        for(int j=0; j < points.length; ++j) {
            System.out.print(points[j] + ",");
        }
        System.out.print("}\n");
        Rectangle r = Polygon.createBoundingBoxPolygon(points);
        int radius = GeometricAlgorithms.distance(r.getMaxX(), r.getMaxY(), r.getMinX(), r.getMinY()) / 2;
        System.out.println("radius =" + radius);
        Assert.assertTrue(radius > 0);
    }

    @Test
    public void testPointBoundingBox() {
        double[] dpoints = new double[]{-89.622072, 40.778103, -89.622072, 40.778103};
        int[] points = new int[dpoints.length];
        for(int i=0; i < dpoints.length; ++i) {
            points[i] =  (int)(dpoints[i] * StorageConstants.ACCURACY_FACTOR);
        }
        int pointRadius = 150;
        Rectangle r = Polygon.createBoundingBoxPoint(points, pointRadius);
        int mbrRadius = GeometricAlgorithms.distance(r.getMaxX(), r.getMaxY(), r.getMinX(), r.getMinY()) / 2;
        System.out.println("mbr radius =" + mbrRadius);
        Assert.assertTrue(mbrRadius > 0);
    }
}
