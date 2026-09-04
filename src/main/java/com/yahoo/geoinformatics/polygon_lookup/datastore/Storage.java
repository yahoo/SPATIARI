package com.yahoo.geoinformatics.polygon_lookup.datastore;

import com.yahoo.geoinformatics.polygon_lookup.geometry.GeometricAlgorithms;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Rectangle;
import com.yahoo.geoinformatics.polygon_lookup.rtree.Node;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author koushikm
 *
 * This capture the acutal data structure to store the polygon data and index data
 */

public class Storage {

    /**
     * Class names allowed when reading Java-serialized datapacks ({@code storageNeutral == false}).
     * Only primitive {@code int[]} and {@code byte[]} payloads are written by {@link #save(String)}.
     */
    private static final Set<String> JAVA_DATAPACK_ALLOWED_CLASSES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("[I", "[B")));

    /**
     * Restricts Java deserialization to the array types written by {@link #save(String)}.
     * Datapacks are treated as trusted build artifacts; this blocks gadget-chain payloads if a
     * file is substituted. Prefer {@code storageNeutral == true} (language-agnostic) for new packs.
     */
    private static final class RestrictedObjectInputStream extends ObjectInputStream {
        RestrictedObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            final String name = desc.getName();
            if (!JAVA_DATAPACK_ALLOWED_CLASSES.contains(name)) {
                throw new InvalidClassException(name, "Unauthorized deserialization for spatial datapack");
            }
            return super.resolveClass(desc);
        }
    }

    /**
     * Selects how exterior-point distance is computed during single-result {@code searchNearby}.
     * Default is {@link #CENTROID} for backward compatibility. Callers that need boundary-based ranking
     * (e.g. reverse geocoder town lookup) must opt in to {@link #BOUNDARY} explicitly.
     */
    public enum NearbyRankingStrategy {
        /** Default: centroid distance when outside the polygon. */
        CENTROID,
        /** Opt-in: rank by boundary distance via relative vertex search, then meter distance to selected vertex. */
        BOUNDARY
    }

    private NearbyRankingStrategy nearbyRankingStrategy = NearbyRankingStrategy.CENTROID;

    // maximum number of bytes needed to store an internal node
    private int maxInternalNodeSize;

    // metadata regarding the R-Tree (root + depth + max children = three ints)
    private int[] headerStore;
    private int nextHeaderOffset;

    // all bounding boxes which are part of R-Tree
    // (4 integer for bounding box + 1 byte for children count + ids of the children)
    private byte[] indexStore;
    // contains the offset for each id of the R-Tree node
    private int[] indexOffset;
    // next free byte in index store
    private int nextIndexOffset;

    // actual polygon data (attributes index + area + centroid(lat, lon) + number of points + {lat, lon) for outer
    // boundary + ringcount +
    // bounding box for 0th ring + 0th ringsize + {lat, lon) for 0th ring ...till n'th ring)
    private int[] polygonStore;
    // contains the offset for each polygon data in polygonStore
    private int[] polygonOffset;
    // next free location in polygon store
    private int nextPolygonOffset;

    //extra metadata related to datapack
    int indexStoreSize;
    int indexOffsetSize;
    int polygonStoreSize;
    int polygonOffsetSize;
    int totalPolygons;

    /*
     * This method initilize the required storage for the datapack
     *
     * @param totalPolygons : total number of polygons to be stored - all polygons of a multi-polygon is handled
     *                      individually
     * @param totalRings : total number of inner rings
     * @param totalPoints : total number of points from boundary as well as inner rings
     * @param maxChildren : maximum children each internal node can accommodate
     *
     */
    public Storage(int totalPolygons, int totalRings, int totalPoints, int maxChildren) {
        this.totalPolygons = totalPolygons;
        initIndexStore(maxChildren);

        initPolygonStore(totalRings, totalPoints);

        // currently header contains only root id, depth of the R-Tree, and max children per node
        headerStore = new int[StorageConstants.MAX_HEADER_SIZE];
        nextHeaderOffset = 0;
    }

    public Storage() {

    }

    /**
     * This method calculates number of bytes needed to store the R-Tree index (that is all R-Tree nodes) For each
     * internal node, we need store the bounding box, followed by children count and ids of all the children.For each
     * leaf node, we need store the bounding box, followed by number of children(zero always) indexStore is byte array,
     * so all storage calculations are number of bytes required
     *
     * @param maxChildren   : maximum children each internal node can accommodate
     */
    private void initIndexStore(int maxChildren) {
        // number of bytes required to store a bounding box
        int boundingBoxStoreSize = (StorageConstants.BOUNDING_BOX_POINTS_COUNT * StorageConstants.SIZE_INT);
        // number of bytes required to store children indices
        int childrenStoreSize = (maxChildren * StorageConstants.SIZE_INT);

        this.maxInternalNodeSize = boundingBoxStoreSize + childrenStoreSize + StorageConstants.CHILDREN_COUNT_STORE;
        int maxLeafNodeSize = boundingBoxStoreSize + StorageConstants.CHILDREN_COUNT_STORE;

        int internalNodeCount = calculateInternalNodeCount(totalPolygons, maxChildren);
        indexStoreSize = ((internalNodeCount * maxInternalNodeSize) + (totalPolygons * maxLeafNodeSize));
        indexOffsetSize = totalPolygons + internalNodeCount;

        indexStore = new byte[indexStoreSize];
        indexOffset = new int[indexOffsetSize];
        nextIndexOffset = 0;
    }

    /**
     * This method calculates total number of integers required to store all polygons For each polygon, we need to store
     * the attribute index of the polygon, area, centroid, outer boundary, and all rings if any. For outer boundary, we
     * need to store the count and all the points of the outer boundary For rings, we need to store number of rings,
     * followed by details of each ring For each ring, we need to store the ring size, followed by bounding box of the
     * ring, followed by all coordinates of the ring
     *
     * @param totalRings    total number of inner rings
     * @param totalPoints   total number of points from boundary as well as inner rings
     */
    private void initPolygonStore(int totalRings, int totalPoints) {
        // in polygon meta data we currently store six must values:
        // 1. the attribute index of the polygon,
        // 2. area of the polygon,
        // 3. centroid latitude ,
        // 4. centroid longitude,
        // 5. radius of influence,
        // 6. total number of points of outer boundary,
        // 7. all outer boundary points, and
        // 6. number of rings for that polygon (ring count will be set to zero for polygons with no rings)
        // for each ring we need to store the size of that ring (this is collectively totalRings)
        // for each ring, we also need to store the bounding box of that ring, followed by total number of points in the
        // ring,
        // followed by all points in the ring
        polygonStoreSize = ((StorageConstants.POLYGON_METADATA_SIZE * totalPolygons) + totalRings
                                + (totalRings * StorageConstants.BOUNDING_BOX_POINTS_COUNT)
                                + (totalPoints * StorageConstants.POINT_SIZE_IN_INT));
        polygonStore = new int[polygonStoreSize];
        polygonOffset = new int[totalPolygons];
        nextPolygonOffset = 0;
    }

    /**
     * @param totalLeafNodes     : number of leaf nodes
     * @param maxChildrenPerNode : maximum children of each node
     * @return : total number of nodes (leafnodes + internalnodes)
     */
    private int calculateInternalNodeCount(int totalLeafNodes, int maxChildrenPerNode) {
        int total = 0;
        int nodes = totalLeafNodes;

        // add up the internal nodes (time complexity : O(log n))
        while (nodes > 1) {
            nodes = (int) Math.ceil((double) nodes / maxChildrenPerNode);
            total += nodes;
        }

        return total;
    }


    /**
     * Helper method to store data
     *
     * @param id : integer index of the polygon (index starts with zero)
     * @param p  : Polygon data containing all points
     * @return : whether the data could be successfully stored
     */
    public boolean storePolygon(int id, Polygon p) {
        int totalPointsInPolygon = p.getTotalPoints();
        int totalRings = p.getTotalRings();
        // For each polygon we need to store (1, 2, 3, 4, 5, 6, 7 below are part of polygon meta data)
        // 1.attribute index of the polygon
        // 2. area of the polygon
        // 3. centroid longitude
        // 4. centroid latitude
        // 5. radius of influence
        // 6. number of outer boundary points followed by all exterior boundary points
        // 7. total number of rings in that polygon
        // 8. Then for each ring, first store bounding box of the ring,
        // followed by number of coordinates for each ring (captured in variable totalRings)
        // followed by all interior boundary points of that ring
        int inputLength = StorageConstants.POLYGON_METADATA_SIZE + totalRings
                          + (totalRings * StorageConstants.BOUNDING_BOX_POINTS_COUNT)
                          + (StorageConstants.POINT_SIZE_IN_INT * totalPointsInPolygon);

        if (polygonStore == null) {
            System.out.println("Polygon storage not initialized ... failure");
            return false;
        }

        // check if the enough space is available
        if ((polygonStore.length - nextPolygonOffset) < inputLength) {
            System.out.println("Not enough storage while storing polygons, bug in storage allocation ... failure");
            return false;
        }

        // store the offset for this polygon
        polygonOffset[id] = nextPolygonOffset;

        // first store index to attribute data, followed by area, centroid, and radius of influence
        polygonStore[nextPolygonOffset++] = p.getAttributeIndex();
        polygonStore[nextPolygonOffset++] = p.getArea();
        polygonStore[nextPolygonOffset++] = p.getCentroidX();
        polygonStore[nextPolygonOffset++] = p.getCentroidY();
        polygonStore[nextPolygonOffset++] = p.getRadiusOfInfluence();

        // next store the outer polygon : number of points followed by all exterior boundary points
        polygonStore[nextPolygonOffset++] = p.getOuterBoundaryPoints().length / 2;
        for (int i = 0; i < p.getOuterBoundaryPoints().length; i += 2) {
            polygonStore[nextPolygonOffset++] = p.getOuterBoundaryPoints()[i];
            polygonStore[nextPolygonOffset++] = p.getOuterBoundaryPoints()[i + 1];
        }

        // next store all rings: first number of rings
        // then for each ring: bounding box of that ring, followed by size, then total coordinates
        polygonStore[nextPolygonOffset++] = totalRings; // store total number of rings first
        if (totalRings > 0) {
            int[][] rings = p.getRings();
            for (int i = 0; i < totalRings; ++i) {
                int[] innerRing = rings[i];
                // for each ring, first store the bounding box
                Rectangle r = Polygon.createBoundingBoxPolygon(innerRing);
                polygonStore[nextPolygonOffset++] = r.getMaxX();
                polygonStore[nextPolygonOffset++] = r.getMaxY();
                polygonStore[nextPolygonOffset++] = r.getMinX();
                polygonStore[nextPolygonOffset++] = r.getMinY();
                // for each ring, next store size of the ring
                polygonStore[nextPolygonOffset++] = innerRing.length / 2;
                // store the coordinates finally
                for (int j = 0; j < innerRing.length; j += 2) {
                    polygonStore[nextPolygonOffset++] = innerRing[j];
                    polygonStore[nextPolygonOffset++] = innerRing[j + 1];
                }
            }
        }

        return true;
    }


    /**
     * Helper method to store a R-Tree nodes
     *
     * @param id    : index of the node (index starts with zero)
     * @param entry : Node object to be stored
     * @return : whether node could be successfully stored
     */
    public boolean storeIndex(int id, Node entry) {
        if (indexStore == null) {
            System.out.println("Index storage not initialized ... failure");
            return false;
        }

        if ((indexStore.length - nextIndexOffset) < maxInternalNodeSize) {
            System.out.println("Not enough storage while storing index, bug in storage allocation ... failure");
            return false;
        }

        // store the offset for this node
        indexOffset[id] = nextIndexOffset;

        // store the bounding box
        nextIndexOffset = storeData(indexStore, nextIndexOffset, entry.getBoundingBox().getMaxX());
        nextIndexOffset = storeData(indexStore, nextIndexOffset, entry.getBoundingBox().getMaxY());
        nextIndexOffset = storeData(indexStore, nextIndexOffset, entry.getBoundingBox().getMinX());
        nextIndexOffset = storeData(indexStore, nextIndexOffset, entry.getBoundingBox().getMinY());

        // store the children list
        int totalChildren = 0;
        if (entry.getChildren() != null) {
            totalChildren = entry.getChildren().length;
        }
        indexStore[nextIndexOffset++] = (byte) totalChildren;

        for (int i = 0; i < totalChildren; ++i) {
            nextIndexOffset = storeData(indexStore, nextIndexOffset, entry.getChildren()[i]);
        }

        return true;
    }

    private int storeData(byte[] buffer, int offset, int data) {
        ByteArray.writeInt(buffer, offset, data);
        offset += StorageConstants.SIZE_INT;
        return offset;
    }

    /**
     * Helper method to store R-Tree header
     *
     * @param root        : root index of the r-tree
     * @param height      : depth of the the r-tree
     * @param maxChildren : maximum children each internal node can have
     * @return : whether header could be successfully stored
     */
    public boolean storeHeader(int root, int height, int maxChildren) {
        if (headerStore == null) {
            System.out.println("Storage not initialized ... failure");
            return false;
        }

        headerStore[nextHeaderOffset++] = root;
        headerStore[nextHeaderOffset++] = height;
        headerStore[nextHeaderOffset++] = maxChildren;

        //store extra information for language-neutral datapack creation
        headerStore[nextHeaderOffset++] = indexStoreSize;
        headerStore[nextHeaderOffset++] = indexOffsetSize;
        headerStore[nextHeaderOffset++] = polygonStoreSize;
        headerStore[nextHeaderOffset++] = totalPolygons;
        return true;
    }

    /**
     * Given the id, construct the Node from the storage buffer This is a utility function to be used to debug the
     * storage buffer
     *
     * @param id : index of the node to be returned
     * @return : Node with the given index
     */
    public Node getNode(int id) {
        int offset = indexOffset[id];
        // System.out.println("getNode: found offset " + offset);
        int maxX = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int maxY = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int minX = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int minY = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        Rectangle mbr = new Rectangle(minX, minY, maxX, maxY);

        // System.out.println("Rectangle found : " + mbr.toString());

        int totalChildren = indexStore[offset];
        offset++;
        int[] children = null;
        if (totalChildren > 0) {
            children = new int[totalChildren];
            for (int i = 0; i < totalChildren; ++i) {
                int child = ByteArray.readInt(indexStore, offset);
                offset += StorageConstants.SIZE_INT;
                children[i] = child;
            }
        }

        return Node.createInternalNode(id, children, mbr);
    }

    /**
     * Given the id, construct the polygon from the storage buffer This is a utility function to be used to debug the
     * storage buffer
     *
     * @param id index of the polygon to be returned
     * @return return the polygon given the index
     */
    public Polygon getPolygon(int id) {
        int offset = polygonOffset[id];
        int attributeId = polygonStore[offset++];
        int area = polygonStore[offset++];
        int centroidX = polygonStore[offset++];
        int centroidY = polygonStore[offset++];
        int radius = polygonStore[offset++];
        int[] centroid = new int[]{centroidX, centroidY};
        int total = polygonStore[offset++] * 2;

        int[] boundaryPoints = new int[total];
        for (int i = 0; i < total; i += 2) {
            boundaryPoints[i] = polygonStore[offset++];
            boundaryPoints[i + 1] = polygonStore[offset++];
        }

        int totalRings = polygonStore[offset++];
        int[][] rings = null;
        if (totalRings > 0) {
            rings = new int[totalRings][];
            for (int i = 0; i < totalRings; ++i) {
                // skip bounding box of the ring
                offset += StorageConstants.BOUNDING_BOX_POINTS_COUNT;
                int ringSize = polygonStore[offset++] * 2;
                rings[i] = new int[ringSize];
                for (int j = 0; j < ringSize; j += 2) {
                    rings[i][j] = polygonStore[offset++];
                    rings[i][j + 1] = polygonStore[offset++];
                }
            }
        }

        return new Polygon(attributeId, boundaryPoints, rings, area, centroidX, centroidY, radius);
    }

    public int getRoot() {
        return headerStore[0];
    }

    public int getHeight() {
        return headerStore[1];
    }

    public int getMaxChildren() {
        return headerStore[2];
    }

    /**
     * @param id : root id to start with, any node id in general
     * @param x  x-coordinate
     * @param y  y-coordinate
     * @return : entry index of the polygon if there is match, else -1
     * @param metrics       Internal metrics of RTree to get insights.
     */
    public int search(int id, int x, int y, int[] metrics) {
        // if the node does not contain the point return
        if (!checkPointInBoundingbox(id, x, y)) {
            return -1;
        }

        int totalChildren = getChildrenCount(id);

        // we have hit the leaf node, time for point in polygon
        if (totalChildren == 0) {
            int attributeIndex = checkPointInPolygon(id, x, y);
            if (attributeIndex >= 0) {
                return attributeIndex;
            }
        }

        // depth-first search follows : iterate over each child
        // node offset + child count + 1 gives the location of the first child
        int firstChildOffset = indexOffset[id] + (StorageConstants.NODE_CHILD_COUNT_OFFSET) + 1;
        for (int i = 0; i < totalChildren; ++i) {
            int childOffset = firstChildOffset + (i * StorageConstants.SIZE_INT);
            int childIndex = ByteArray.readInt(indexStore, childOffset);
            int attributeIndex = search(childIndex, x, y, metrics);
            if (attributeIndex >= 0) {
                return attributeIndex;
            }
        }

        // end of search, nothing succeeded
        return -1;
    }

    /**
     * @param id : index of the node
     * @return get number of children of this node
     */
    private int getChildrenCount(int id) {
        int offset = indexOffset[id];
        offset += StorageConstants.NODE_CHILD_COUNT_OFFSET;
        return indexStore[offset];
    }

    /**
     * Construct the bounding box from the store and then check for containment of the given coordinate
     *
     * @param id node index
     * @param x  x-coordinate
     * @param y  y-coordinate
     * @return true if the given coordinate is within the MBR
     */
    private boolean checkPointInBoundingbox(int id, int x, int y) {
        int offset = indexOffset[id];
        int maxX = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int maxY = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int minX = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int minY = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;

        // check containment in a bounding box
        return checkPointInBoundingbox(x, y, maxX, maxY, minX, minY);
    }

    private boolean checkPointInBoundingbox(int x, int y, int maxX, int maxY, int minX, int minY) {
        if ((x < minX) || (y < minY) || (x > maxX) || (y > maxY)) {
            return false;
        }

        return true;
    }

    /**
     * Algorithm to see if a point is within a polygon. We use ray-casting to check polygon containment. For polygons
     * with one or more rings, we take that into account.
     *
     * @param id entry index of the polygon
     * @param x  x-coordinate
     * @param y  y-coordinate
     * @return attribute index of the polygon if point is inside polygon else -1
     */
    private int checkPointInPolygon(int id, int x, int y) {
        int offset = polygonOffset[id];
        int attributeIndex = polygonStore[offset++];
        // skip area, centroid_latitude, centroid_longitude, radius
        offset += 4;
        int totalOuterBoundaryPoints = polygonStore[offset++];
        boolean inside = isPointInPolygon(x, y, totalOuterBoundaryPoints, offset);

        // if the point is outside, return from here
        if (!inside) {
            return -1;
        }

        // check for all rings only if the point is inside the outer boundary
        offset += (2 * totalOuterBoundaryPoints);
        int ringCount = polygonStore[offset++];
        boolean insideRing = false;
        for (int i = 0; i < ringCount; ++i) {
            // first check the bounding box
            int maxX = polygonStore[offset++];
            int maxY = polygonStore[offset++];
            int minX = polygonStore[offset++];
            int minY = polygonStore[offset++];

            boolean insideBoundingBox = checkPointInBoundingbox(x, y, maxX, maxY, minX, minY);
            int ringSize = polygonStore[offset++];
            // check for inside polygon only if the point is inside the bounding box
            if (insideBoundingBox) {
                insideRing = isPointInPolygon(x, y, ringSize, offset);
            }

            // if the point is inside any one ring, lookup failed
            if (insideRing) {
                return -1;
            }

            // Look at the next ring
            offset += (2 * ringSize);
        }

        if (inside) {
            return attributeIndex;
        } else {
            return -1;
        }
    }

    /**
     * Implementation of ray-casting algorithm to check whether search point is within a polygon
     * This version of the algorithm returns true or false depending on whether the given point is
     * inside or outside the polygon
     *
     * @param x           x-coordinate
     * @param y           y-coordinate
     * @param totalPoints total number of points in the polygon
     * @param offset      offset to first coordinate
     * @return true if the search point is inside the polygon
     */
    private boolean isPointInPolygon(int x, int y, int totalPoints, int offset) {
        boolean inside = false;
        int x1 = polygonStore[offset++];
        int y1 = polygonStore[offset++];
        for (int i = 1; i < totalPoints; ++i) {
            int x2 = polygonStore[offset++];
            int y2 = polygonStore[offset++];
            if ((y1 > y) != (y2 > y)) {
                // x-coordinate of the test-point can't be greater than the x-values of the edge to intersect
                if ((x < x1) || (x < x2)) {
                    if ((x < (((x2 - x1) * (y - y1) / (y2 - y1)) + x1))) {
                        inside = !inside;
                    }
                }
            }
            x1 = x2;
            y1 = y2;
        }

        return inside;
    }


    /**
     * @param id            index of the node (root node to start with)
     * @param rminX         x-coordinate of bottom left search bounding box
     * @param rminY         y-coordinate of bottom left of search bounding box
     * @param rmaxX         x-coordinate of top right search bounding box
     * @param rmaxY         y-coordinate of top right of search bounding box
     * @param searchResults array of indices of polygons inside search bounding box
     * @param metrics       Internal metrics of RTree to get insights.
     */
    public void search(int id, int rminX, int rminY, int rmaxX, int rmaxY, int[] searchResults, int[] metrics) {
        // if the node does not contain the point return
        if (!intersect(id, rminX, rminY, rmaxX, rmaxY)) {
            return;
        }

        int totalChildren = getChildrenCount(id);

        // we have hit the leaf node, time for updating result list
        if (totalChildren == 0) {
            int index = getAttributeIndex(id);
            fillIndex(searchResults, index);
        }

        // depth-first search follows : iterate over each child
        // node offset + child count + 1 gives the location of the first child
        int firstChildOffset = indexOffset[id] + (StorageConstants.NODE_CHILD_COUNT_OFFSET) + 1;
        for (int i = 0; i < totalChildren; ++i) {
            int childOffset = firstChildOffset + (i * StorageConstants.SIZE_INT);
            int childIndex = ByteArray.readInt(indexStore, childOffset);
            search(childIndex, rminX, rminY, rmaxX, rmaxY, searchResults, metrics);
        }
    }


    /**
     * @param id    index of the node (root node to start with)
     * @param rminX x-coordinate of bottom left search bounding box
     * @param rminY y-coordinate of bottom left of search bounding box
     * @param rmaxX x-coordinate of top right search bounding box
     * @param rmaxY y-coordinate of top right of search bounding box
     * @return true if two bounding boxes intersect
     */
    private boolean intersect(int id, int rminX, int rminY, int rmaxX, int rmaxY) {
        int offset = indexOffset[id];
        int maxX = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int maxY = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int minX = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int minY = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;

        if ((rminX > maxX) || (rminY > maxY) || (rmaxX < minX) || (rmaxY < minY)) {
            return false;
        } else {
            return true;
        }
    }


    /**
     * Given the entry-index of a polygon in R-Tree, get the attribute-index of that polygon
     *
     * @param id entry-index of a polygon
     * @return attribute index of the polygon if successful
     */
    private int getAttributeIndex(int id) {
        if (id < 0) {
            return -1;
        }
        int offset = polygonOffset[id];
        int attributeId = polygonStore[offset++];
        return attributeId;
    }


    /**
     * @param searchResults
     * @param index
     */
    private void fillIndex(int[] searchResults, int index) {
        for (int i = 0; i < searchResults.length; ++i) {
            if (searchResults[i] < 0) {
                searchResults[i] = index;
                return;
            }
        }
    }

    /**
     * @param id            index of the node (root node to start with)
     * @param centerX       x-coordinate of the center
     * @param centerY       y-coordinate of the center
     * @param radius        search radius
     * @param searchResults array of indices of polygons inside search radius, along with distance of the nearest
     *                      boundary vertex from the search point. Following is the order in the array : index_1, distance_1, index_2, distance_2, .... -1.
     *                      It's the responsibility of the client that result array has enough capacity.
     * @param metrics       Internal metrics of RTree to get insights.
     */
    public void search(int id, int centerX, int centerY, int radius, int[] searchResults, int[] metrics) {
        // if the circle does not intersect with the node,
        if (!intersect(id, centerX, centerY, radius)) {
            return;
        }

        int totalChildren = getChildrenCount(id);

        // we have hit the leaf node, time for updating result list
        if (totalChildren == 0) {
            fillIndexAndDistance(searchResults, id, centerX, centerY);
        }

        // depth-first search follows : iterate over each child
        // node offset + child count + 1 gives the location of the first child
        int firstChildOffset = indexOffset[id] + (StorageConstants.NODE_CHILD_COUNT_OFFSET) + 1;
        for (int i = 0; i < totalChildren; ++i) {
            int childOffset = firstChildOffset + (i * StorageConstants.SIZE_INT);
            int childIndex = ByteArray.readInt(indexStore, childOffset);
            search(childIndex, centerX, centerY, radius, searchResults, metrics);
        }
    }

    /**
     * @param id                 index of the node (root node to start with)
     * @param centerX            x-coordinate of the center
     * @param centerY            y-coordinate of the center
     * @param horizontalAccuracy horizontal accuracy
     * @param searchRadius       search radius
     * @param searchResults      flattened array of results : distance_1, id_1, confidence_1, .., distance_2, id_2,
     *                           confidence_2,...etc offset determines the distance between distance_1 and distance_2 the array length and
     *                           offset together determines maximum how many entries we can put in the array
     * @param offset             distance between two entries in the result array, this allows some gaps to filled up in higher
     *                           layer
     * @param metrics            Internal metrics of RTree to get insights.
     */
    public void search(int id, int centerX, int centerY, int horizontalAccuracy, int searchRadius, int[] searchResults,
                       int offset, int[] metrics) {

        if (metrics != null) {
            // First metric is the number of rectangles that are explored.
            metrics[0]++;
        }
        // if the circle does not intersect with the node,
        if (!intersect(id, centerX, centerY, searchRadius)) {
            return;
        }

        int totalChildren = getChildrenCount(id);

        // we have hit the leaf node, time for updating result list
        if (totalChildren == 0) {
            addCandidateIndex(searchResults, id, centerX, centerY, offset);
        }

        // depth-first search follows : iterate over each child
        // node offset + child count + 1 gives the location of the first child
        int firstChildOffset = indexOffset[id] + (StorageConstants.NODE_CHILD_COUNT_OFFSET) + 1;
        for (int i = 0; i < totalChildren; ++i) {
            int childOffset = firstChildOffset + (i * StorageConstants.SIZE_INT);
            int childIndex = ByteArray.readInt(indexStore, childOffset);
            search(childIndex, centerX, centerY, horizontalAccuracy, searchRadius, searchResults, offset, metrics);
        }
    }


    /**
     * Fill the index, distance and radius. The final result is supposed to contain attribute-index, so this mapping has
     * to be done post-ranking. Confidence score will be calculated later which is function of the radius.
     *
     * @param searchResults array containing the search result
     * @param id            entry index of the result to be inserted
     * @param centerX       x-coordinate of the search query
     * @param centerY       y-coordinate of the search query
     * @param offset        distance between two entries in the result array, this allows some gaps to filled up in higher
     *                      layer
     */
    private void addCandidateIndex(int[] searchResults, int id, int centerX, int centerY, int offset) {
        int newDistance = 0;
        if (!checkInsidePolygon(id, centerX, centerY)) {
            newDistance = minimumDistance(id, centerX, centerY);
        }

        // Maintains maximum number of results accommodating the better result,
        // pushing off less relevant result based on distance
        MaxPriorityQueue.insert(searchResults, id, newDistance, offset);
    }

    /**
     * Find the minimum distance between polygon and the queried point
     * We loop over all points of the polygon to find the nearest point.
     * Please note that it will also work for point data where the respective polygon structure contains two same points
     *
     * @param id            entry index of the result to be inserted
     * @param x       x-coordinate of the search query
     * @param y       y-coordinate of the search query
     *
     */
    private int minimumDistance(int id, int x, int y) {
        int offset = polygonOffset[id];
        int totalPoints = polygonStore[offset + 5];
        // skip index, area, centroid_latitude, centroid_longitude, radius of influence, total boundary points
        offset += 6;
        int minDistance = Integer.MAX_VALUE;
        for (int i = 1; i < totalPoints; ++i) {
            int x1 = polygonStore[offset++];
            int y1 = polygonStore[offset++];

            int distance = GeometricAlgorithms.distance(x, y, x1, y1);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    /**
     * Find the closest boundary vertex using a cheap relative metric, then return the meter distance to that vertex.
     */
    private int minimumBoundaryDistance(int id, int x, int y) {
        int offset = polygonOffset[id];
        int totalPoints = polygonStore[offset + 5];
        // skip index, area, centroid_latitude, centroid_longitude, radius of influence, total boundary points
        offset += 6;
        int minRelativeDistance = Integer.MAX_VALUE;
        int closestVertexX = 0;
        int closestVertexY = 0;
        for (int i = 1; i < totalPoints; ++i) {
            int x1 = polygonStore[offset++];
            int y1 = polygonStore[offset++];

            int relativeDistance = GeometricAlgorithms.relativeRankingDistance(x, y, x1, y1);
            if (relativeDistance < minRelativeDistance) {
                minRelativeDistance = relativeDistance;
                closestVertexX = x1;
                closestVertexY = y1;
            }
        }
        if (minRelativeDistance == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return GeometricAlgorithms.distance(x, y, closestVertexX, closestVertexY);
    }

    /**
     * replace each entry-index by attribute-index in the final result Since the each result is flattened, the first
     * entry is distance followed by entry-index
     *
     * @param searchResults array of search results containing ranked entry-indices (distance, followed by entry-id)
     * @param offset        distance between two entries in the result array, this allows some gaps to filled up in higher
     *                      layer
     */
    public void fillAttributeIndex(int[] searchResults, int offset) {
        for (int i = 0; i < searchResults.length; i += offset) {
            if (searchResults[i] < 0) {
                break;
            }
            searchResults[i + 1] = getAttributeIndex(searchResults[i + 1]);
        }
    }

    /**
     * Given a set of results, calculate the confidence score and location-coverage of each result.
     * Confidence calculation is done by figuring out how much of the user circle (defined by user coordinate and horizontal accuracy)
     * overlaps with a place.
     *
     * @param centerX            x-coordinate of user input
     * @param centerY            y-coordinate of user input
     * @param horizontalAccuracy accuracy of the user circle
     * @param searchResults      All search results
     * @param resultOffset       Distance between two entries in the result array, this allows some gaps to filled up in
     *                           higher layer.
     */
    public void calculateConfidenceScore(int centerX, int centerY, int horizontalAccuracy, int[] searchResults,
                                         int resultOffset) {
        for (int i = 0; i < searchResults.length; i += resultOffset) {
            if (searchResults[i] < 0) {
                break;
            }
            int id = searchResults[i + 1];
            int offset = polygonOffset[id];
            int radiusOfInfluence = polygonStore[offset + 4];
            double userArea = (StorageConstants.PI * horizontalAccuracy * horizontalAccuracy);
            int indexOffset = this.indexOffset[id];
            int maxX = ByteArray.readInt(indexStore, indexOffset);
            indexOffset += StorageConstants.SIZE_INT;
            int maxY = ByteArray.readInt(indexStore, indexOffset);
            indexOffset += StorageConstants.SIZE_INT;
            int minX = ByteArray.readInt(indexStore, indexOffset);
            indexOffset += StorageConstants.SIZE_INT;
            int minY = ByteArray.readInt(indexStore, indexOffset);

            //NOTE: following centroids derived from bounding box may be different from polygon centroid
            //And these centroids should be used instead of polygon centroid for confidence calculation
            int centroidX = (minX + maxX)/2;
            int centroidY = (minY + maxY)/2;

            double overlappedArea = GeometricAlgorithms.getOverlappingArea(centerX, centerY, horizontalAccuracy,
                                                                           centroidX, centroidY, radiusOfInfluence);
            double locationArea = (StorageConstants.PI * radiusOfInfluence * radiusOfInfluence);
            searchResults[i + 2] = (int) Math.round((overlappedArea / userArea) * 100); //confidence
            searchResults[i + 3] = radiusOfInfluence;
            searchResults[i + 4] = (int) Math.round((overlappedArea / locationArea) * 100); //location coverage
        }
    }

    /**
     * @param id      index of the node
     * @param centerX x-coordinate of the center
     * @param centerY y-coordinate of the center
     * @param radius  search radius
     * @return true if the bounding box of the node intersects the circle
     */
    private boolean intersect(int id, int centerX, int centerY, int radius) {
        int offset = indexOffset[id];
        int maxX = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int maxY = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int minX = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;
        int minY = ByteArray.readInt(indexStore, offset);
        offset += StorageConstants.SIZE_INT;

        // we have mainly three cases to handle to check intersection between a circle and rectangle
        // 1. circle is completely within the rectangle : center will be within rectangle
        // 2. rectangle overlap with the circle in more than one edge (this includes all four edge where entire
        // rectangle is inside the circle) : distance between one or more corner to center is less
        // than radius,
        // 3. only one edge of the rectangle cut the circle with four corners outside: shortest distance from the center
        // to that edge is less than radius
        // Following algorithm handles all three scenarios at one shot

        // find the the point in the rectangle closest to center of the circle
        int closestX = Math.min(Math.max(centerX, minX), maxX);
        int closestY = Math.min(Math.max(centerY, minY), maxY);

        // if the point in the rectangle closest to the center is less than radius, we have an overlap
        if (GeometricAlgorithms.distance(closestX, closestY, centerX, centerY) < radius) {
            return true;
        }

        return false;
    }


    /**
     * Fill the distance followed by index. To be noted that we are filling only entry-index of the result. The final
     * result is supposed to contain attribute-index, so this mapping has to be done post-ranking. Here we are storing
     * only one result - the most relevant one.
     * Distance of the queried point to the polygon is zero if the point is within the polygon.
     * Otherwise we rank by boundary distance ({@link #minimumBoundaryDistance}).
     *
     * @param searchResults array containing the search result
     * @param id            entry index of the result to be inserted
     * @param centerX       x-coordinate of the search query
     * @param centerY       y-coordinate of the search query
     */
    private void fillIndexAndDistance(int[] searchResults, int id, int centerX, int centerY) {
        int newResultId = id;
        int newResultDistance = rankingDistance(id, centerX, centerY);

        if ((searchResults[1] < 0) || (nearer(newResultId, searchResults[1], centerX, centerY, newResultDistance,
                                              searchResults[0]))) {
            searchResults[0] = newResultDistance;
            searchResults[1] = newResultId;
            return;
        }
    }


    /**
     * @param id entry index of the polygon
     * @param x  x-coordinate
     * @param y  y-coordinate
     * @return true if the search point is inside polygon
     */
    private boolean checkInsidePolygon(int id, int x, int y) {
        if (checkPointInBoundingbox(id, x, y)) {
            if (checkPointInPolygon(id, x, y) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets how exterior-point distance is ranked during single-result {@code searchNearby}.
     * Defaults to {@link NearbyRankingStrategy#CENTROID}; set {@link NearbyRankingStrategy#BOUNDARY}
     * to enable boundary-based ranking (LOCATION-13774 fix).
     */
    public void setNearbyRankingStrategyForBenchmark(NearbyRankingStrategy strategy) {
        this.nearbyRankingStrategy = strategy;
    }

    public NearbyRankingStrategy getNearbyRankingStrategyForBenchmark() {
        return nearbyRankingStrategy;
    }

    /**
     * Distance used to rank a leaf polygon during single-result nearby search.
     *
     * @return 0 when the point is inside the polygon; otherwise a positive distance in meters
     */
    int rankingDistance(int id, int x, int y) {
        if (checkInsidePolygon(id, x, y)) {
            return 0;
        }
        switch (nearbyRankingStrategy) {
            case BOUNDARY:
                return minimumBoundaryDistance(id, x, y);
            case CENTROID:
            default:
                return distance(id, x, y);
        }
    }

    /**
     * For a point within two polygons, we chose the smaller polygon.
     * This is an example of choosing a more granular zip like UK zips.
     * If the point is inside a polygon (Point-in-polygon is true and hence distance is zero) then that is higher in rank.
     * For all other cases, go by distance.
     *
     * @param firstId        index of first input polygon
     * @param secondId       index of second input polygon
     * @param x              x-coordinate of the search point
     * @param y              y-coordinate of the search point
     * @param firstDistance  pre-calculated distance of the firstId from search point
     * @param secondDistance pre-calculated distance of the secondId from search point
     * @return true if from the search point is strictly nearer to polygon indexed by firstId than the one indexed by
     * secondId
     */
    private boolean nearer(int firstId, int secondId, int x, int y, int firstDistance, int secondDistance) {
        // handle the equal case first
        if (firstId == secondId) {
            return false;
        }

        //when the point is inside both polygons, chose the one with smaller area (UK zips is an example)
        if((firstDistance == 0) && (secondDistance == 0)) {
            return (area(firstId) < area(secondId));
        }

        //inside polygon has highest rank where distance is zero
        if( firstDistance == 0 ) {
            return true;
        }
        if( secondDistance == 0 ) {
            return false;
        }

        //for every other case go by distance
        return (firstDistance < secondDistance);
    }


    /**
     * @param id index of the polygon
     * @return area of the polygon stored(rounded to nearest integer value)
     */
    private int area(int id) {
        int offset = polygonOffset[id];
        int area = polygonStore[++offset]; // skip attributeIndex and then get the area
        return area;
    }

    /**
     * get the centroid of the polygon and then calculate the distance with the given point
     *
     * @param id index of the polygon
     * @param x  x-coordinate of the input coordinate
     * @param y  y-coordinate of the input coordinate
     * @return the distance from a point to centroid of the polygon indexed by id
     */
    private int distance(int id, int x, int y) {
        int offset = polygonOffset[id];
        offset += 2; // skip attribute index and area

        double lon1 = (double) (polygonStore[offset++] / StorageConstants.ACCURACY_FACTOR);
        double lat1 = (double) (polygonStore[offset++] / StorageConstants.ACCURACY_FACTOR);

        double lon2 = (double) (x / StorageConstants.ACCURACY_FACTOR);
        double lat2 = (double) (y / StorageConstants.ACCURACY_FACTOR);

        return GeometricAlgorithms.planeProjectedDistance(lat1, lon1, lat2, lon2);
    }

    /*
     * Language neutral way of saving index+data in a file
     * All the extra information one need to retrive the storage are kept in the headerStore
     * @param fileName : name of the datapack file
     * @throws IOException           exception thrown on I/O error
     */
    public void saveDatapack(String fileName) throws IOException {
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
        try {
            //write the header first
            for (int i =    0; i < StorageConstants.MAX_HEADER_SIZE; ++i) {
                out.writeInt(headerStore[i]);
            }

            //next index store followed by index-offset array
            for (int i = 0; i < indexStoreSize; ++i) {
                out.writeByte(indexStore[i]);
            }
            for (int i = 0; i < indexOffsetSize; ++i) {
                out.writeInt(indexOffset[i]);
            }

            //finally write the actual data and data-offset array
            for (int i = 0; i < polygonStoreSize; ++i) {
                out.writeInt(polygonStore[i]);
            }
            for (int i = 0; i < totalPolygons; ++i) {
                out.writeInt(polygonOffset[i]);
            }
        } finally {
            out.close();
        }
    }

    /**
     * Save data to a file which will act as datapack
     *
     * @param fileName : name of the datapack file
     * @throws FileNotFoundException exception thrown
     * @throws IOException           exception thrown on I/O error
     */
    public void save(String fileName) throws FileNotFoundException, IOException {
        try (ObjectOutputStream outStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            outStream.writeObject(headerStore);
            outStream.writeObject(indexStore);
            outStream.writeObject(indexOffset);
            outStream.writeObject(polygonStore);
            outStream.writeObject(polygonOffset);
        }
    }

    /**
     * restore data from a datapack
     *
     * @param fileName : datapack name to be passed
     * @param storageNeutral : true for Java independent datapack creation, false to use Java technology to create datapack
     * @return : Storage object to be used during lookup
     * @throws FileNotFoundException exception thrown if file is not found
     * @throws IOException           exception thrown on I/O error
     */
    public static Storage loadFrom(final String fileName , final boolean storageNeutral) throws FileNotFoundException, IOException {
        if (storageNeutral) {
            return loadFromLanguageAgnostic(fileName);
        }

        // LOCATION-12800: allowlist-only deserialization (int[] / byte[] only).
        try (ObjectInputStream inStream =
                 new RestrictedObjectInputStream(new BufferedInputStream(new FileInputStream(fileName)))) {
            Storage storage = new Storage();
            storage.headerStore = (int[]) inStream.readObject();
            storage.indexStore = (byte[]) inStream.readObject();
            storage.indexOffset = (int[]) inStream.readObject();
            storage.polygonStore = (int[]) inStream.readObject();
            storage.polygonOffset = (int[]) inStream.readObject();
            return storage;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Couldn't understand file.", e);
        }
    }

    /**
     * restore data from language agnositic datapack
     * @param datapackName : datapack file name
     * @return Storage object to be used during lookup
     * @throws IOException exception thrown for I/O error
     */
    public static Storage loadFromLanguageAgnostic(final String datapackName) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(datapackName)))) {
            Storage storage = new Storage();
            storage.headerStore = new int[StorageConstants.MAX_HEADER_SIZE];

            for (int ii = 0; ii < StorageConstants.MAX_HEADER_SIZE; ++ii) {
                storage.headerStore[ii] = in.readInt();
            }

            storage.indexStoreSize = storage.headerStore[3];
            storage.indexOffsetSize = storage.headerStore[4];
            storage.polygonStoreSize = storage.headerStore[5];
            storage.polygonOffsetSize = storage.headerStore[6];

            storage.indexStore = new byte[storage.indexStoreSize];
            storage.indexOffset = new int[storage.indexOffsetSize];
            storage.polygonStore = new int[storage.polygonStoreSize];
            storage.polygonOffset = new int[storage.polygonOffsetSize];

            for (int ii = 0; ii < storage.indexStoreSize; ++ii) {
                storage.indexStore[ii] = in.readByte();
            }

            for (int ii = 0; ii < storage.indexOffsetSize; ++ii) {
                storage.indexOffset[ii] = in.readInt();
            }

            for (int ii = 0; ii < storage.polygonStoreSize; ++ii) {
                storage.polygonStore[ii] = in.readInt();
            }

            for (int ii = 0; ii < storage.polygonOffsetSize; ++ii) {
                storage.polygonOffset[ii] = in.readInt();
            }

            return storage;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Couldn't understand file.", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("header store size : " + headerStore.length + "\n");
        result.append("index offset store size : " + indexOffset.length + "\n");
        result.append("index store size : " + indexStore.length + "\n");
        result.append("polygon offset store size : " + polygonOffset.length + "\n");
        result.append("polygon store size : " + polygonStore.length + "\n");

        for (int i = 0; i < indexOffset.length; ++i) {
            result.append(i + "th node index: \n");
            result.append(getNode(i).toString() + "\n");
        }

        for (int i = 0; i < polygonOffset.length; ++i) {
            result.append(i + "th polygon index: \n");
            result.append(getPolygon(i).toString() + "\n");
        }
        return result.toString();
    }
}
