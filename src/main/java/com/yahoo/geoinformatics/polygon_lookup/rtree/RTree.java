package com.yahoo.geoinformatics.polygon_lookup.rtree;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
import com.yahoo.geoinformatics.polygon_lookup.datastore.Storage;

/**
 * This class represent the R-Tree
 *
 * @author koushikm
 */
public final class RTree {

    private final int maxChildren; // a node can contain maximum how many child nodes
    private int root; // id of the node which is root of the tree
    private int height; // height of the tree
    private int id;
    private Storage storage;

    public RTree(int maxChildren) {
        id = 0;
        height = 0;
        root = -1;
        this.maxChildren = maxChildren;
        storage = null;
    }

    public RTree(Storage store) {
        storage = store;
        root = storage.getRoot();
        height = storage.getHeight();
        maxChildren = storage.getMaxChildren();
    }

    private int getNextId() {
        return id++;
    }

    /**
     * Create datapack out of index and data
     *
     * @param datapackName name of the output file
     * @param storageNeutral true if datapack to be created language-neutral way
     * @return true if datapack creation succeeds
     */
    public boolean storeIndex(String datapackName, boolean storageNeutral) {
        if (storage == null) {
            return false;
        }

        try {
            if (storageNeutral) {
                storage.saveDatapack(datapackName);
            } else {
                storage.save(datapackName);
            }
            return true;
        } catch (FileNotFoundException e) {
            // TODO Replace with a log
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Replace with a log
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Given a list polygons, build a spatial index for faster lookup
     *
     * @param polygons list of input polygons
     * @return true if index building is successful
     */
    public boolean buildIndex(List<Polygon> polygons) {
        if (polygons.isEmpty()) {
            System.out.println("Nothing to build - no input");
            return false;
        }

        // create all leaf nodes from the input data and store them
        List<Node> leafNodes = insert(polygons);

        // now proceed to create internal nodes from leaf nodes and rest of the tree
        int level = 1;
        createNodes(leafNodes, level);

        storage.storeHeader(root, height, maxChildren);
        return true;
    }

    public int getRootId() {
        return root;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxChildren() {
        return maxChildren;
    }

    public Node getNode(int id) {
        return storage.getNode(id);
    }

    public Polygon getPolygon(int id) {
        return storage.getPolygon(id);
    }

    private List<Node> insert(List<Polygon> polygons) {
        int totalPolygons = polygons.size();
        int totalPoints = 0;
        int totalRings = 0;

        for (Polygon p : polygons) {
            totalPoints += p.getTotalPoints();
            totalRings += p.getTotalRings();
        }

        // initialize the storage
        storage = new Storage(totalPolygons, totalRings, totalPoints, maxChildren);

        List<Node> leafNodes = new ArrayList<>(totalPolygons);
        for (Polygon p : polygons) {
            int entryId = getNextId();
            Node entry = Node.createLeafNode(entryId, p.getBoundingBox());
            leafNodes.add(entry);

            storage.storePolygon(entryId, p);
            storage.storeIndex(entryId, entry);
        }

        return leafNodes;
    }

    private void sortEntries(List<Node> input) {
        int inputSize = input.size();

        int yNum = input.size() / maxChildren;
        if (yNum < 1) {
            return;
        }

        Collections.sort(input, Node.XMinComparator);
        yNum = (int) Math.sqrt(yNum);
        yNum = yNum * maxChildren;

        int num = inputSize;
        int index = 0;

        while (num > 0) {
            if (num > yNum) {
                Collections.sort(input.subList(index, index + yNum), Node.YMinComparator);
                num -= yNum;
                index += yNum;
            } else {
                Collections.sort(input.subList(index, index + num), Node.YMinComparator);
                num = 0;
            }
        }
    }

    // We use sort-recurse-tile strategy to build up the index bottom up
    private List<Node> createNodes(List<Node> input) {
        double outputCount = Math.ceil((double) input.size() / maxChildren);
        List<Node> output = new ArrayList<Node>((int) (outputCount));
        sortEntries(input);

        Node entry;
        int size = input.size();
        int start = 0;
        while (start < size) {
            int entryId = getNextId();
            entry = Node.createInternalNode(entryId, input, start, maxChildren);
            if ((size - start) < maxChildren) {
                start += (size - start);
            } else {
                start += maxChildren;
            }

            output.add(entry);
            storage.storeIndex(entryId, entry);
        }
        return output;
    }

    private void createNodes(List<Node> inputNodes, int level) {
        List<Node> nextLevelBoxes = createNodes(inputNodes);

        if (nextLevelBoxes.size() == 1) {
            root = nextLevelBoxes.get(0).getId();
            height = level;
            return;
        }

        createNodes(nextLevelBoxes, ++level);
    }

    /**
     * Point in polygon based search with input coordinate. Layer contains only polygon.Only one result maximum is
     * expected. The no-object footprint version of the search. Starting with the root node of R-Tree search till we get
     * all the candidates polygon. Then we apply point-in-polygon lookup and return the attribute index of first polygon
     * Note : Returning the first polygon is the good enough current assumption, we may change that later if we have a
     * case of overlapping polygon
     *
     * @param x x-coordinate of the point to be searched
     * @param y y-coordinate of the point to be searched
     * @param metrics Internal metrics of RTree to get insights
     * @return return the first polygon which contains the point
     */
    public int search(int x, int y, int[] metrics) {
        if (root < 0) {
            // TODO: add log here
            throw new IllegalStateException("Nothing to process, no R-Tree index exist");
        }

        if (storage == null) {
            // TODO: add log here
            throw new IllegalStateException("Storage not set, can't search");
        }

        return storage.search(root, x, y, metrics);
    }

    /**
     * Nearby search for a given coordinate along with horizontal accuracy.Layer may contain both polygon and point
     * data. The result carries index along with confidence and distance. The layer can be exclusively polygon or point.
     * Only one result maximum is expected.
     *
     * @param x             x-coordinate of the point to be searched
     * @param y             y-coordinate of the point to be searched
     * @param accuracy      horizontal accuracy of the input coordinate
     * @param radius        search radius in Meters
     * @param searchResults array of indices of polygons inside the search circle
     * @param metrics Internal metrics of RTree to get insights
     */
    public void searchNearby(int x, int y, int accuracy, int radius, int[] searchResults, int[] metrics) {
        if (searchResults == null) {
            throw new IllegalArgumentException("searchResults not initialized");
        }
        storage.search(root, x, y, radius, searchResults, metrics);
        int offset = StorageConstants.ATTRIBUTES_PER_LOCATION; //distance, index, confidence, radius of influence, location-coverage
        storage.calculateConfidenceScore(x, y, accuracy, searchResults, offset);
        storage.fillAttributeIndex(searchResults, offset);
    }

    /**
     * Package-private hook for town-layer performance benchmarks.
     */
    public void setNearbyRankingStrategyForBenchmark(Storage.NearbyRankingStrategy strategy) {
        storage.setNearbyRankingStrategyForBenchmark(strategy);
    }

    public Storage.NearbyRankingStrategy getNearbyRankingStrategyForBenchmark() {
        return storage.getNearbyRankingStrategyForBenchmark();
    }

    /**
     * Nearby search for a given circle.Layer may contain both polygon and point data and there will be multiple
     * results. The result carries index along with confidence and distance. Results are separated by offset.
     *
     * @param x             x-coordinate of the point to be searched
     * @param y             y-coordinate of the point to be searched
     * @param accuracy      horizontal accuracy
     * @param searchRadius  search radius
     * @param searchResults array of indices of polygons inside the search circle
     * @param offset        distance between two entries in the result array, this allows some gaps to filled up in higher
     *                      layer
     * @param metrics       Internal metrics of RTree to get insights
     */
    public void searchNearby(int x, int y, int accuracy, int searchRadius, int[] searchResults, int offset,
                             int[] metrics) {
        if (searchResults == null) {
            throw new IllegalArgumentException("searchResults not initialized");
        }

        storage.search(root, x, y, accuracy, searchRadius, searchResults, offset, metrics);
        storage.calculateConfidenceScore(x, y, accuracy, searchResults, offset);
        storage.fillAttributeIndex(searchResults, offset);
    }

    /**
     * Nearby search based on bounding box input.
     *
     * @param rminX         x-coordinate of bottom left search bounding box
     * @param rminY         y-coordinate of bottom left of search bounding box
     * @param rmaxX         x-coordinate of top right search bounding box
     * @param rmaxY         y-coordinate of top right of search bounding box
     * @param searchResults array of search results
     * @param metrics Internal metrics of RTree to get insights
     */
    public void search(int rminX, int rminY, int rmaxX, int rmaxY, int[] searchResults, int[] metrics) {
        if (searchResults == null) {
            throw new IllegalArgumentException("searchResults not initialized");
        }

        storage.search(root, rminX, rminY, rmaxX, rmaxY, searchResults, metrics);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        Queue<Integer> nodeQueue = new LinkedList<>();
        Queue<Integer> polygonQueue = new LinkedList<>();
        int root = storage.getRoot();
        int height = storage.getHeight();
        nodeQueue.add(root);
        result.append("root id : " + root + "\n");
        result.append("height of the tree: " + height + "\n");

        while (!nodeQueue.isEmpty()) {
            int id = nodeQueue.remove();
            Node entry = storage.getNode(id);
            int[] childrenList = entry.getChildren();
            if (childrenList != null) {
                int size = childrenList.length;
                for (int i = 0; i < size; ++i) {
                    nodeQueue.add(childrenList[i]);
                }
            } else {
                // no child means that node is a leaf node and hence contain polygon data
                polygonQueue.add(id);
            }

            result.append(entry.toString() + "\n");
        }
        while (!polygonQueue.isEmpty()) {
            int id = polygonQueue.remove();
            Polygon p = storage.getPolygon(id);
            result.append("EntryId:" + id + " Polygon: " + p.toString() + "\n");
        }

        return result.toString();
    }
}
