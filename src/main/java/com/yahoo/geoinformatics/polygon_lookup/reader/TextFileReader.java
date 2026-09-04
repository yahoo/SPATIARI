package com.yahoo.geoinformatics.polygon_lookup.reader;

import com.yahoo.geoinformatics.polygon_lookup.datastore.StorageConstants;
import com.yahoo.geoinformatics.polygon_lookup.geometry.Polygon;
import com.yahoo.geoinformatics.polygon_lookup.index.AttributeIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//The initial version of text file reader : only for point data
public final class TextFileReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextFileReader.class);
    public static final String PIPE_DELIMITER = "\\|";
    private final String filePath;
    private final String textExtension ;
    private final List<Polygon> polygons;

    private int runningIndex;
    private AttributeIndexer indexer;

    public TextFileReader(String inputFilePath, String extension, AttributeIndexer indexer,
                          int startIndex) {
        this.filePath = inputFilePath;
        this.textExtension = extension;
        this.runningIndex = startIndex;
        this.indexer = indexer;
        this.polygons = new ArrayList<>();
    }

    public boolean process() {
        File folder = new File(filePath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(textExtension));
        System.out.println("files total = " + files.length);
        if(files == null) {
            //exit that there is no file found
            LOGGER.error("No input text files found while building the index");
            System.out.println("No input text files found while building the index");
            return false;
        }

        for(File file : files) {
            try {
                System.out.println("start processing file : " + file.getName().toString());
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                //header: POST_MAIN|POST_FULL|LAT|LON
                String header = bufferedReader.readLine();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] tokens = line.split(PIPE_DELIMITER);
                    String plus4CodeString = tokens[1].trim();
                    String latitudeString = tokens[2].trim();
                    String longitudeString = tokens[3].trim();

                    long plus4Code = Long.valueOf(plus4CodeString);
                    double latitude = Double.valueOf(latitudeString);
                    double longitude = Double.valueOf(longitudeString);

                    int[] polygon = new int[4];
                    int x = (int)(longitude * StorageConstants.ACCURACY_FACTOR);
                    int y = (int)(latitude * StorageConstants.ACCURACY_FACTOR);
                    polygon[0] = polygon[2] = x;
                    polygon[1] = polygon[3] = y;
                    int radius = 10;
                    polygons.add(new Polygon(runningIndex, polygon, null, 0, x, y, radius));

                    Map<String , String > attributes = new HashMap<>();
                    attributes.put("post_full", plus4CodeString);
                    indexer.addAttributeValues(runningIndex, attributes);
                    ++runningIndex;

                }
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("Exception occurred when processing shape file(s) {}", file, e);
            }
        }

        return true;
    }

    public int computeEntries() {
        File folder = new File(filePath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(textExtension));
        System.out.println("files total = " + files.length);
        if(files == null) {
            //exit that there is no file found
            LOGGER.error("No input text files found while building the index");
            return 0;
        }
        int count = 0 ;
        for(File file : files) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                //header: POST_MAIN|POST_FULL|LAT|LON
                String header = bufferedReader.readLine();
                String line; int newCount = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    count++ ; newCount++;
                }
                System.out.println("for flie :" + file.getName().toString() + " count = " + newCount + " total so far" + count);
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("Exception occurred when processing shape file(s) {}", file, e);
            }
        }
        return count;
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }

    /*
    public static void main(String[] args) {
        String inputPath =  "/Users/koushikm/data/zip_plus_four/zip_4_final_columns"; //"/Users/koushikm/data/zip_plus_four/zip_4_final_columns_sample";
        String extension = ".txt";
        int start = 0;
        Instant begin = Instant.now();
        TextFileReader reader = new TextFileReader(inputPath, extension, new DataIndexer(), start);
        System.out.println("total count = " + reader.computeEntries());
        reader.process();
        List<Polygon> polygons = reader.getPolygons();
        Instant end = Instant.now();
        Duration elapsed = Duration.between(begin, end);
        System.out.println("Time taken: " + elapsed.toMillis() + " milliseconds");
        System.out.println("total polygons = " + polygons.size());
    }
     */
}
