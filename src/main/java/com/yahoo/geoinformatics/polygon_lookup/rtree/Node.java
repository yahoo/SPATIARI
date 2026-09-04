package com.yahoo.geoinformatics.polygon_lookup.rtree;

import com.yahoo.geoinformatics.polygon_lookup.geometry.Rectangle;

import java.util.Comparator;
import java.util.List;


/**
 * This class captures a node in the R-tree. A node can be a leaf node containing spatial data besides index information
 * Or just an internal node, containing only index information
 *
 * @author koushikm
 */
public class Node {

    private final int id; // each node is identified by an id
    private final int[] children; // other nodes it contains
    private final Rectangle mbr; // bounding box of this node


    public static Node createInternalNode(int id, List<Node> candidates, int start, int maxChildren) {
        int index = 0;
        Rectangle rect = null;

        // find total number of children this node can have
        int size = candidates.size();
        int totalChildren = maxChildren;
        if ((size - start) < maxChildren) {
            totalChildren = (size - start);
        }
        int[] children = new int[totalChildren];

        // add all the children and create the bounding box for this node
        for (int i = 0; ((i < maxChildren) && (start < size)); ++i, ++start) {
            children[index++] = candidates.get(start).getId();
            Rectangle r = candidates.get(start).getBoundingBox();
            if (rect == null) {
                rect = r;
            } else {
                rect = rect.union(r);
            }
        }

        return new Node(id, children, rect);
    }

    public static Node createInternalNode(int id, int[] children, Rectangle mbr) {
        return new Node(id, children, mbr);
    }

    public static Node createLeafNode(int id, Rectangle mbr) {
        return new Node(id, null, mbr);
    }

    private Node(int id, int[] children, Rectangle mbr) {
        this.id = id;
        this.children = children;
        this.mbr = mbr;
    }

    public int[] getChildren() {
        return children;
    }

    public int getId() {
        return this.id;
    }

    public Rectangle getBoundingBox() {
        return mbr;
    }

    public boolean contains(int x, int y) {
        if (mbr != null) {
            return mbr.contains(x, y);
        } else {
            return false;
        }
    }

    public static Comparator<Node> XMinComparator = new Comparator<Node>() {
        public int compare(Node n1, Node n2) {
            return (int) (n1.getBoundingBox().getMinX() - n2.getBoundingBox().getMinX());
        }
    };

    public static Comparator<Node> YMinComparator = new Comparator<Node>() {
        public int compare(Node n1, Node n2) {
            return (int) (n1.getBoundingBox().getMinY() - n2.getBoundingBox().getMinY());
        }
    };

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.getClass().getName());
        result.append(" NodeId: " + this.id);
        result.append(" MBR:" + this.mbr);
        result.append(" Children entries: [");
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                result.append(children[i] + " : ");
            }
        } else {
            result.append("no children");
        }
        result.append(" ]\n");
        return result.toString();
    }
}
