package com.yahoo.geoinformatics.polygon_lookup.spatial;

import com.yahoo.geoinformatics.polygon_lookup.datastore.Storage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * End-to-end town-layer {@code searchNearby} benchmark on production {@code world_town.dp}.
 *
 * <p>Run manually:
 * <pre>
 *   mvn -Dtest=TownLayerNearbySearchPerfBenchmark -DrunTownPerfBenchmark=true test
 * </pre>
 *
 * <p>Datapack source: {@code s3://yp--userlocation-prd-use1--geo-datapacks/reverse_geocoder_datapacks/1.10.12/world_town.dp}
 */
public class TownLayerNearbySearchPerfBenchmark {

    private static final int TOWN_RADIUS_METERS = 3000;
    private static final int HORIZONTAL_ACCURACY = 0;
    private static final int WARMUP_ITERATIONS = 500;
    private static final int MEASURE_ITERATIONS = 5000;

    private static final Path DATAPACK = Paths.get("src/test/resources/perf/world_town/world_town.dp");
    private static final Path REPORT = Paths.get("target/town-layer-perf-report.md");

    private SpatialLookup lookup;

    static final class QueryPoint {
        final String name;
        final double lat;
        final double lon;

        QueryPoint(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    static final class SampleResult {
        final int distanceMeters;
        final int attributeIndex;

        SampleResult(int distanceMeters, int attributeIndex) {
            this.distanceMeters = distanceMeters;
            this.attributeIndex = attributeIndex;
        }
    }

    static final class TimingStats {
        final long count;
        final double meanMicros;
        final double p50Micros;
        final double p95Micros;
        final double p99Micros;
        final double minMicros;
        final double maxMicros;
        final double qps;

        TimingStats(long count, double meanMicros, double p50Micros, double p95Micros, double p99Micros,
                        double minMicros, double maxMicros, double qps) {
            this.count = count;
            this.meanMicros = meanMicros;
            this.p50Micros = p50Micros;
            this.p95Micros = p95Micros;
            this.p99Micros = p99Micros;
            this.minMicros = minMicros;
            this.maxMicros = maxMicros;
            this.qps = qps;
        }
    }

    @BeforeClass
    public void loadTownDatapack() throws Exception {
        assumeBenchmarkEnabled();
        Assert.assertTrue(Files.isRegularFile(DATAPACK),
                        "Missing " + DATAPACK + ". Copy from S3 reverse_geocoder_datapacks/1.10.12/world_town.dp");
        Files.deleteIfExists(REPORT);
        SpatialIndexer indexer = new SpatialIndexer();
        lookup = indexer.buildIndexFromDatapack(DATAPACK.toString(), false);
        Assert.assertNotNull(lookup);
        Files.write(REPORT, ("# Town layer nearby search performance\n\n"
                        + "Datapack: `" + DATAPACK + "`\n"
                        + "Compared strategies: baseline centroid (pre-fix), production boundary distance.\n")
                        .getBytes(StandardCharsets.UTF_8));
    }

    @DataProvider(name = "strategies")
    public Object[][] strategies() {
        return new Object[][] {
                        {Storage.NearbyRankingStrategy.CENTROID, "baseline_centroid"},
                        {Storage.NearbyRankingStrategy.BOUNDARY, "production_boundary"}
        };
    }

    @Test
    public void verifyFranklinTicketCorrectnessAcrossStrategies() {
        assumeBenchmarkEnabled();
        QueryPoint franklinTicket = new QueryPoint("location_13774", 35.972681, -86.905842);

        SampleResult centroid = runOnce(Storage.NearbyRankingStrategy.CENTROID, franklinTicket);
        SampleResult boundary = runOnce(Storage.NearbyRankingStrategy.BOUNDARY, franklinTicket);

        Assert.assertEquals(centroid.distanceMeters, 3745, "centroid baseline should match dev-pod Bethlehem distance");
        Assert.assertTrue(boundary.distanceMeters < 1000,
                        "boundary ranking should report meter distance near Franklin city limit, got "
                                        + boundary.distanceMeters);
        Assert.assertNotEquals(centroid.attributeIndex, boundary.attributeIndex,
                        "ranked town should change vs centroid baseline");
    }

    @Test(dataProvider = "strategies")
    public void benchmarkAllQuerySuites(Storage.NearbyRankingStrategy strategy, String strategyLabel) throws IOException {
        assumeBenchmarkEnabled();
        List<QueryPoint> queries = buildQuerySuites();
        Map<String, TimingStats> statsByQuery = new LinkedHashMap<>();
        List<Long> allNanos = new ArrayList<>(queries.size() * MEASURE_ITERATIONS);

        for (QueryPoint query : queries) {
            warmup(strategy, query);
            List<Long> samples = measure(strategy, query, MEASURE_ITERATIONS);
            allNanos.addAll(samples);
            statsByQuery.put(query.name, toStats(samples));
        }

        appendReportSection(strategyLabel, statsByQuery, toStats(allNanos));
    }

    private static void assumeBenchmarkEnabled() {
        Assert.assertTrue(Boolean.getBoolean("runTownPerfBenchmark"),
                        "Set -DrunTownPerfBenchmark=true to run town-layer perf benchmark");
    }

    private SampleResult runOnce(Storage.NearbyRankingStrategy strategy, QueryPoint query) {
        lookup.setNearbyRankingStrategyForBenchmark(strategy);
        int[] results = new int[5];
        Arrays.fill(results, -1);
        lookup.searchNearby(query.lat, query.lon, HORIZONTAL_ACCURACY, TOWN_RADIUS_METERS, results, null);
        return new SampleResult(results[0], results[1]);
    }

    private void warmup(Storage.NearbyRankingStrategy strategy, QueryPoint query) {
        lookup.setNearbyRankingStrategyForBenchmark(strategy);
        int[] results = new int[5];
        for (int i = 0; i < WARMUP_ITERATIONS; ++i) {
            lookup.searchNearby(query.lat, query.lon, HORIZONTAL_ACCURACY, TOWN_RADIUS_METERS, results, null);
        }
    }

    private List<Long> measure(Storage.NearbyRankingStrategy strategy, QueryPoint query, int iterations) {
        lookup.setNearbyRankingStrategyForBenchmark(strategy);
        int[] results = new int[5];
        List<Long> samples = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; ++i) {
            long start = System.nanoTime();
            lookup.searchNearby(query.lat, query.lon, HORIZONTAL_ACCURACY, TOWN_RADIUS_METERS, results, null);
            samples.add(System.nanoTime() - start);
        }
        return samples;
    }

    private static TimingStats toStats(List<Long> nanosSamples) {
        List<Long> sorted = new ArrayList<>(nanosSamples);
        Collections.sort(sorted);
        long count = sorted.size();
        double sum = 0;
        for (long n : sorted) {
            sum += n;
        }
        double meanNanos = sum / count;
        return new TimingStats(count, meanNanos / 1000.0, percentile(sorted, 50) / 1000.0, percentile(sorted, 95) / 1000.0,
                        percentile(sorted, 99) / 1000.0, sorted.get(0) / 1000.0, sorted.get(sorted.size() - 1) / 1000.0,
                        count / (sum / 1_000_000_000.0));
    }

    private static double percentile(List<Long> sorted, int pct) {
        int index = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(index);
    }

    private static List<QueryPoint> buildQuerySuites() {
        List<QueryPoint> queries = new ArrayList<>();
        queries.add(new QueryPoint("location_13774_franklin_ticket", 35.972681, -86.905842));
        queries.add(new QueryPoint("franklin_city_centroid", 35.92425, -86.87093));
        queries.add(new QueryPoint("nyc_manhattan", 40.758, -73.9855));
        queries.add(new QueryPoint("la_downtown", 34.0522, -118.2437));
        queries.add(new QueryPoint("chicago_loop", 41.8837, -87.6324));
        queries.add(new QueryPoint("london_city", 51.5074, -0.1278));
        queries.add(new QueryPoint("paris_center", 48.8566, 2.3522));
        queries.add(new QueryPoint("tokyo_shinjuku", 35.6895, 139.6917));
        queries.add(new QueryPoint("sydney_cbd", -33.8688, 151.2093));
        queries.add(new QueryPoint("sao_paulo_center", -23.5505, -46.6333));
        queries.add(new QueryPoint("mumbai_center", 19.076, 72.8777));
        queries.add(new QueryPoint("rural_montana", 46.8797, -110.3626));
        queries.add(new QueryPoint("alaska_fairbanks", 64.8378, -147.7164));
        queries.add(new QueryPoint("honolulu", 21.3069, -157.8583));

        for (int bearing = 0; bearing < 360; bearing += 45) {
            double radians = Math.toRadians(bearing);
            double lat = 35.972681 + (2.0 / 111.0) * Math.cos(radians);
            double lon = -86.905842 + (2.0 / (111.0 * Math.cos(Math.toRadians(35.972681)))) * Math.sin(radians);
            queries.add(new QueryPoint(String.format(Locale.US, "franklin_ring_2km_%03d", bearing), lat, lon));
        }

        for (int latStep = 0; latStep < 10; ++latStep) {
            for (int lonStep = 0; lonStep < 10; ++lonStep) {
                double lat = 25.0 + latStep * 3.0;
                double lon = -125.0 + lonStep * 6.0;
                queries.add(new QueryPoint(String.format(Locale.US, "us_grid_%02d_%02d", latStep, lonStep), lat, lon));
            }
        }

        return queries;
    }

    private static synchronized void appendReportSection(String strategyLabel, Map<String, TimingStats> perQuery,
                    TimingStats aggregate) throws IOException {
        if (!Boolean.getBoolean("runTownPerfBenchmark")) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n## Strategy: ").append(strategyLabel).append("\n\n");
        sb.append("Town radius: ").append(TOWN_RADIUS_METERS).append(" m, horizontal accuracy: ")
                        .append(HORIZONTAL_ACCURACY).append(", warmup: ").append(WARMUP_ITERATIONS)
                        .append(", measure/query: ").append(MEASURE_ITERATIONS).append("\n\n");
        sb.append("### Aggregate (all query suites)\n\n");
        appendStatsRow(sb, "ALL", aggregate);
        sb.append("\n### Per query point\n\n");
        sb.append("| query | mean µs | p50 µs | p95 µs | p99 µs | min µs | max µs | qps |\n");
        sb.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        perQuery.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
            TimingStats s = entry.getValue();
            sb.append(String.format(Locale.US, "| %s | %.1f | %.1f | %.1f | %.1f | %.1f | %.1f | %.0f |\n",
                            entry.getKey(), s.meanMicros, s.p50Micros, s.p95Micros, s.p99Micros, s.minMicros,
                            s.maxMicros, s.qps));
        });

        if (!Files.exists(REPORT.getParent())) {
            Files.createDirectories(REPORT.getParent());
        }
        Files.write(REPORT, sb.toString().getBytes(StandardCharsets.UTF_8),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    private static void appendStatsRow(StringBuilder sb, String label, TimingStats s) {
        sb.append(String.format(Locale.US,
                        "- **%s**: mean %.1f µs, p50 %.1f µs, p95 %.1f µs, p99 %.1f µs, min %.1f µs, max %.1f µs, %.0f qps\n",
                        label, s.meanMicros, s.p50Micros, s.p95Micros, s.p99Micros, s.minMicros, s.maxMicros,
                        s.qps));
    }
}
