package com.yahoo.geoinformatics.polygon_lookup.rtree;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import com.yahoo.geoinformatics.polygon_lookup.geometry.Rectangle;

public class NodeTest {

    @Test
    public void testNodeConstruction() {
        int id = 2;

        Rectangle r = new Rectangle(-122, 375, -121, 376);
        Node n1 = Node.createLeafNode(1, r);
        List<Node> candidates = Arrays.asList(n1);
        Node n = Node.createInternalNode(id, candidates, 0, 3);

        Assert.assertEquals(2, n.getId());
        Assert.assertEquals(1, n.getChildren().length);

        Assert.assertEquals(-122, n.getBoundingBox().getMinX());
        Assert.assertEquals(375, n.getBoundingBox().getMinY());
        Assert.assertEquals(-121, n.getBoundingBox().getMaxX());
        Assert.assertEquals(376, n.getBoundingBox().getMaxY());
        Assert.assertFalse(n.toString().isEmpty());
    }

    @Test
    public void testNodeChildren() {
        int id = 1;

        Rectangle ch1 = new Rectangle(-121999980, 37578515, -121973703, 37591962);
        Rectangle ch2 = new Rectangle(-122020262, 37565717, -121988369, 37584467);
        Rectangle ch3 = new Rectangle(-121981691, 37595220, -121943899, 37659025);

        Node n1 = Node.createLeafNode(2, ch1);
        Node n2 = Node.createLeafNode(3, ch2);
        Node n3 = Node.createLeafNode(4, ch3);

        List<Node> candidates = Arrays.asList(n1, n2, n3);
        Node n = Node.createInternalNode(id, candidates, 0, 3);

        Assert.assertEquals(3, n.getChildren().length);

        Assert.assertEquals(-122020262, n.getBoundingBox().getMinX());
        Assert.assertEquals(37565717, n.getBoundingBox().getMinY());
        Assert.assertEquals(-121943899, n.getBoundingBox().getMaxX());
        Assert.assertEquals(37659025, n.getBoundingBox().getMaxY());
        Assert.assertFalse(n.toString().isEmpty());

    }
}
