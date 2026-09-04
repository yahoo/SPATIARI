package com.yahoo.geoinformatics.polygon_lookup.datastore;

//import java.util.Arrays;

import java.util.Arrays;

/**
 * A binary heap based max priority queue implementation The binary heap is zeroth index based array implementation,
 * with user defined offset for next entry The class assumes that at any point the array passed to it is already
 * maintaining the max-heap property
 * For a max heap with zeroth index based array implementation, we have :
 * left child = 2*i + 1; right child = 2*i + 2; parent = (n - 1)/2;
 * We generalize the above with offset(s) as follows:
 * left child = 2*i + s; right child = 2*i + 2*s; parent = ((n/s - 1)/2) * s;
 *
 * This is specifically modeled to maintain a specific number of search results during radial search
 *
 * @author koushikm
 */
public class MaxPriorityQueue {

    /**
     * Add new entry to given array while maintaining the max priority-queue property The first entry is distance (which
     * is key for the priority-queue), and second entry is entry id The next key is put after the offset amount of gap
     * in the array to be used by higher layer Offset part ensures than any requirement changes in higher layer does not
     * break this class
     *
     * @param searchResults : add new elements to this array modeled as priority queue
     * @param entryIndex    : id of the new entry
     * @param distance      : distance of the new entry
     * @param offset        : offset for next entry in input array
     */
    public static void insert(int[] searchResults, int entryIndex, int distance, int offset) {
        //NOT doing any checks here as this will be repeatedly called, higher layer should take care of it
        int nextKey = 0;

        // queue is full, remove the max element
        int last = searchResults.length - offset;
        if (searchResults[last] >= 0) {
            if (!extractMax(searchResults, distance, offset)) {
                return;
            }
            nextKey = last;
        } else {
            nextKey = getNextKeyPosition(searchResults, offset);
        }

        searchResults[nextKey] = distance;
        searchResults[nextKey + 1] = entryIndex;
        bubbleUp(searchResults, nextKey, offset);
    }


    /**
     * Remove the entry with highest distance while maintaining the max priority-queue property
     *
     * @param searchResults : actual container to be modeled as max-priority queue
     * @param distance      : distance of the new entry
     * @param offset        : offset for next entry in input array
     */
    private static boolean extractMax(int[] searchResults, int distance, int offset) {
        int last = searchResults.length - offset;

        // if the new entry is worse than the worst, then do NOT extract
        if (searchResults[0] < distance) {
            return false;
        }

        // remove the max entry and bring the last entry in its place
        searchResults[0] = searchResults[last];
        searchResults[1] = searchResults[last + 1];

        // reset the last entry
        searchResults[last] = -1;
        searchResults[last + 1] = -1;

        // restore the max-heap property
        bubbleDown(searchResults, offset);
        return true;
    }

    /**
     * @param searchResults : actual container to be modeled as max-priority queue
     * @param offset        : offset for next entry
     * @return next empty place in the array governed by offset
     */
    private static int getNextKeyPosition(int[] searchResults, int offset) {
        int next = 0;
        for (; next < searchResults.length; next = next + offset) {
            if (searchResults[next] < 0) {
                break;
            }
        }
        return next;
    }

    /**
     * Helper method to restore the max-heap property after insertion
     *
     * @param searchResults : actual container to be modeled as max-priority queue
     * @param n             : entry to be bubbled up
     * @param offset        : offset for next entry in input array
     */
    private static void bubbleUp(int[] searchResults, int n, int offset) {
        int parent = n;
        while (n > 0) {
            parent = ((((n / offset) - 1) / 2) * offset);
            if (searchResults[parent] > searchResults[n]) {
                break;
            }

            exchange(searchResults, parent, n);
            n = parent;
        }
    }

    /**
     * Helper method to restore the max-heap property after max-element extraction
     *
     * @param searchResults : actual container to be modeled as max-priority queue
     * @param offset        : offset for next entry in input array
     */
    private static void bubbleDown(int[] searchResults, int offset) {
        int k = 0;
        int n = searchResults.length - 1;
        while (((2 * k) + offset) < n) {
            int jump = 2 * k;
            int left = jump + offset;
            int right = jump + (2 * offset);

            // edge case when we have only left child
            if (right >= n) {
                if (searchResults[left] < searchResults[k]) {
                    break;
                }
                exchange(searchResults, left, k);
                break;
            }

            // out of the two children, exchange with largest
            if (searchResults[left] > searchResults[right]) {
                if (searchResults[left] < searchResults[k]) {
                    break;
                }
                exchange(searchResults, left, k);
                k = left;
            } else {
                if (searchResults[right] < searchResults[k]) {
                    break;
                }
                exchange(searchResults, right, k);
                k = right;
            }
        }

    }

    /**
     * Helper method to swap two entries
     *
     * @param searchResults : actual container to be modeled as max-priority queue
     * @param k1            : array index of the first BusinessListingsData entry
     * @param k2            : array index of the second BusinessListingsData entry
     */
    private static void exchange(int[] searchResults, int k1, int k2) {
        int tempDistance = searchResults[k1];
        int tempId = searchResults[k1 + 1];

        searchResults[k1] = searchResults[k2];
        searchResults[k1 + 1] = searchResults[k2 + 1];

        searchResults[k2] = tempDistance;
        searchResults[k2 + 1] = tempId;
    }


    public static void printMaxHeap(int[] searchResults, int offset) {
        System.out.println("total length = " + searchResults.length);
        for (int i = 0; i < searchResults.length; i += offset) {
            if (searchResults[i] < 0) {
                break;
            }
            System.out.print(searchResults[i] + ",");
        }
    }
    public static void main (String[] args) {
        int[] results = new int[450];
        Arrays.fill(results, -1);
//      int[] inputs = {155,141,139,141,27,27,128,140,27,27,27,27,27,27,82,115,115,27};
        int[] inputs = {155, 141, 139, 141, 27, 27, 128, 140, 27, 27, 27, 27, 27, 27, 82, 115, 115, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 77, 85, 27, 27, 115, 115, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 85, 27, 27, 27, 27, 27, 27, 27, 115};
        int offset = 9;
        System.out.println(inputs.length);
        for (int i = 0; i < inputs.length; ++i) {
            MaxPriorityQueue.insert(results, i, inputs[i], offset);
        }

        System.out.println("Heapify complete");

        printMaxHeap(results, offset);
    }

}
