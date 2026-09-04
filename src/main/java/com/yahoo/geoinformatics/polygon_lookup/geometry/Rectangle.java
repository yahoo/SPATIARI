package com.yahoo.geoinformatics.polygon_lookup.geometry;


/**
 * Rectangle class is modeled via two points
 * It is used to model  minimum bounding rectangle(MBR) or bounding box
 *
 * @author koushikm
 */
public final class Rectangle {

    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;

    public Rectangle(int x1, int y1, int x2, int y2) {
        if (x1 < x2) {
            minX = x1;
            maxX = x2;
        } else {
            minX = x2;
            maxX = x1;
        }
        if (y1 < y2) {
            minY = y1;
            maxY = y2;
        } else {
            minY = y2;
            maxY = y1;
        }
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    // create a new bounding box by extending this bounding with another point
    public Rectangle extend(int[] p) {
        return extend(p[0], p[1]);
    }

    public Rectangle extend(int x, int y) {
        int x1, y1, x2, y2;
        x1 = (minX > x) ? x : minX;
        y1 = (minY > y) ? y : minY;
        x2 = (maxX < x) ? x : maxX;
        y2 = (maxY < y) ? y : maxY;

        return new Rectangle(x1, y1, x2, y2);
    }

    // create a new bounding box by union with this bounding box
    public Rectangle union(Rectangle r) {
        int x1, y1, x2, y2;
        x1 = (minX < r.getMinX()) ? minX : r.getMinX();
        y1 = (minY < r.getMinY()) ? minY : r.getMinY();
        x2 = (maxX > r.getMaxX()) ? maxX : r.getMaxX();
        y2 = (maxY > r.getMaxY()) ? maxY : r.getMaxY();

        return new Rectangle(x1, y1, x2, y2);
    }

    public boolean contains(int x, int y) {
        if ((x < minX) || (y < minY) || (x > maxX) || (y > maxY)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MBR:[" + "minX:" + minX + ", minY:" + minY + "; maxX:" + maxX + ", maxY:" + maxY + "]");
        return result.toString();
    }
}
