package com.yahoo.geoinformatics.polygon_lookup.datastore;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;


public class MaxPriorityQueueTest {

    @Test
    public void testInsert() {
        int[] results = new int[350];
        int offset = 7;
        System.out.println(results.length);
        Arrays.fill(results, -1);
        System.out.println("init done");
        int index = 1001, distance = 50;
        MaxPriorityQueue.insert(results, index, distance, offset);
        Assert.assertEquals(results[0], 50);
        Assert.assertEquals(results[1], 1001);

        index = 1002;
        distance = 55;
        MaxPriorityQueue.insert(results, index, distance, offset);
        Assert.assertEquals(results[0], 55);
        Assert.assertEquals(results[1], 1002);

        int count = 0;
        for (int i = 0; i < results.length; i = i + offset) {
            if (results[i] < 0) {
                break;
            }
            ++count;
        }
        Assert.assertEquals(count, 2);

        index = 1003;
        distance = 40;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 1004;
        distance = 101;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 1005;
        distance = 201;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 1006;
        distance = 33;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 1007;
        distance = 150;
        MaxPriorityQueue.insert(results, index, distance, offset);

        //at this point highest distance entered in 201, total 7 entries
        Assert.assertEquals(results[0], 201);
        Assert.assertEquals(results[1], 1005);
        count = 0;
        for (int i = 0; i < results.length; i = i + offset) {
            if (results[i] < 0) {
                break;
            }
            ++count;
        }
        Assert.assertEquals(count, 7);
    }

    @Test
    public void testExtraction() {
        int[] results = new int[70];
        int offset = 7;
        Arrays.fill(results, -1);
        int index = 2001, distance = 4;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2002;
        distance = 4;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2003;
        distance = 8;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2004;
        distance = 9;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2005;
        distance = 4;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2006;
        distance = 11;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2007;
        distance = 12;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2008;
        distance = 9;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2009;
        distance = 17;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2010;
        distance = 3;
        MaxPriorityQueue.insert(results, index, distance, offset);
        //at this point highest distance entered in 17, total 10 entries
        Assert.assertEquals(results[0], 17);
        Assert.assertEquals(results[1], 2009);
        int count = 0;
        for (int i = 0; i < results.length; i = i + offset) {
            if (results[i] < 0) {
                break;
            }
            ++count;
        }
        Assert.assertEquals(count, 10);

        //next entry should result in extraction
        index = 2011;
        distance = 15;
        MaxPriorityQueue.insert(results, index, distance, offset);
        //at this point highest distance entered in 15, total 10 entries
        Assert.assertEquals(results[0], 15);
        Assert.assertEquals(results[1], 2011);
        count = 0;
        for (int i = 0; i < results.length; i = i + offset) {
            if (results[i] < 0) {
                break;
            }
            ++count;
        }
        Assert.assertEquals(count, 10);

        //next entry should not change the content as this one is the worse of all we have
        //so still the highest distance is 15
        index = 2012;
        distance = 21;
        MaxPriorityQueue.insert(results, index, distance, offset);
        Assert.assertEquals(results[0], 15);
        Assert.assertEquals(results[1], 2011);
        count = 0;
        for (int i = 0; i < results.length; i = i + offset) {
            if (results[i] < 0) {
                break;
            }
            ++count;
        }
        Assert.assertEquals(count, 10);
    }

    @Test
    public void testOffset() {
        int max = 5;
        int offset = 2;
        int[] results1 = new int[max * offset];
        fillContent(results1, offset);
        Assert.assertEquals(results1[0], 19);

        max = 5;
        offset = 3;
        int[] results2 = new int[max * offset];
        fillContent(results2, offset);
        Assert.assertEquals(results2[0], 19);

        max = 5;
        offset = 8;
        int[] results3 = new int[max * offset];
        fillContent(results3, offset);
        Assert.assertEquals(results3[0], 19);
    }

    public void fillContent(int[] results, int offset) {
        Arrays.fill(results, -1);
        int index = 1;
        int distance = 13;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 2;
        distance = 11;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 3;
        distance = 17;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 4;
        distance = 19;
        MaxPriorityQueue.insert(results, index, distance, offset);
        index = 5;
        distance = 18;
        MaxPriorityQueue.insert(results, index, distance, offset);
    }
}
