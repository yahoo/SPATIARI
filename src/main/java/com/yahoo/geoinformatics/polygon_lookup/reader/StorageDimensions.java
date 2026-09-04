package com.yahoo.geoinformatics.polygon_lookup.reader;

public class StorageDimensions {

    private int totalPolygons;
    private int totalRings;
    private int totalPoints;
    private int totalEntries;


    public int getTotalPolygons() {
        return totalPolygons;
    }

    public void incrementTotalPolygons(int totalPolygons) {
        this.totalPolygons += totalPolygons;
    }

    public int getTotalRings() {
        return totalRings;
    }

    public void incrementTotalRings(int totalRings) {
        this.totalRings += totalRings;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void incrementTotalPoints(int totalPoints) {
        this.totalPoints += totalPoints;
    }

    /**
     * Total number of entries (or records) from the data, like total number of counties.
     * @return
     */
    public int getTotalEntries() {
        return totalEntries;
    }

    public void incrementTotalEntries(int totalEntries) {
        this.totalEntries += totalEntries;
    }
}
