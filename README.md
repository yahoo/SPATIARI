# SPATIARI

A Java library for offline indexing of large spatial datasets (polygons, multipolygons, ring-polygons, and points) and fast runtime point-in-region lookup. SPATIARI powers reverse-geocoding workloads that need to resolve GPS coordinates to geographic regions using pre-built spatial indexes.

Also known internally as `geoinformatics_lib_spatial_lookup`.

## Features

- **Offline R-Tree indexing** of shapefile and text (TSV/CSV) inputs into compact datapack (`.dp`) files
- **Fast runtime lookup** with minimal object allocation on the search path
- **Polygon, multipolygon, ring-polygon, and point** geometry support
- **Radial / nearby search** with distance ranking and confidence scoring
- **Language-neutral datapack format** for cross-language index reuse (see [datapack format docs](docs/DATAPACK_SPATIAL_FORMAT.md))

## Prerequisites

- **Java Development Kit (JDK):** 8 or higher
- **Build tool:** [Maven](https://maven.apache.org/) 3.x
- **Optional:** Sample shapefile or text data for indexing tests

## Building

```bash
mvn clean package
```

CI runs `mvn verify` on every PR and `master` push (see `.github/workflows/ci.yml`).

### Consuming the JAR (Yahoo internal)

New coordinates (preferred). Published to Yahoo Artifactory `ugeo-releases` from the internal mirror `yahoo-platforms/location-spatiari` (Screwdriver, profile `-Pugeo-release`):

```xml
<dependency>
  <groupId>com.yahoo.spatiari</groupId>
  <artifactId>spatiari</artifactId>
  <version>1.0.0</version> <!-- use latest released -->
</dependency>
```

Legacy fallback (still built from GHES + SD 210636 until consumers cut over):

```xml
<dependency>
  <groupId>com.yahoo.geoinformatics</groupId>
  <artifactId>geoinformatics_lib_spatial_lookup</artifactId>
  <version>4.0.3</version>
</dependency>
```

### Roadmap

| Phase | Status |
|-------|--------|
| Public source on `yahoo/SPATIARI` | Done |
| GitHub Actions Maven CI | Done |
| GAV → `com.yahoo.spatiari:spatiari` | This change |
| Internal mirror + SD → `ugeo-releases` | Next ([LOCATION-14334](https://ouryahoo.atlassian.net/browse/LOCATION-14334)) |
| Maven Central | Planned ([LOCATION-14335](https://ouryahoo.atlassian.net/browse/LOCATION-14335)) |

## Documentation

- **[Spatial `.dp` file format (R-Tree datapack)](docs/DATAPACK_SPATIAL_FORMAT.md)** — binary layout for language-neutral vs Java-serialized encodings, header fields, R-Tree node and polygon record layout, and relationship to `Storage` / `SpatialIndexer`.

## License

Copyright 2015-2026 Yahoo Inc.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for details.

## Versioning

Version is managed in `pom.xml`. Current line under the new GAV is `1.0.0-SNAPSHOT`. Historical releases under `geoinformatics_lib_spatial_lookup` remain on the legacy line (through `4.0.3`).

## Known users

This library is consumed by Yahoo reverse-geocoding services. Changes may affect downstream systems — please coordinate with the Location Platforms team before modifying public APIs.

## Release history

- [4.0.0] Optional WOE_ID as polygon attribute index
- [3.1.7] Support for text file reader
- [3.1.6] Speed up language-neutral datapack loading; buffering support in datapack creation and loading
- [3.1.5] Renaming the library from geoinformatics_lib_polygon_lookup to geoinformatics_lib_spatial_lookup
- [3.1.3] Use bounding box centroid instead of polygon centroid to calculate confidence
- [3.1.2] Current finding for polygon inside another polygon; simplified comparison layer for admin region
- [3.1.1] Using radius attribute from data for polygon data
- [3.1.0] Indexing radius values for point data
- [3.0.5] Remove dependency on yjava_jdk
- [3.0.1] Support for BDAI data testing; RHEL7 migration
- [2.0.22] OS-independent build (no RHEL6 dependency)
- [2.0.21] Location-coverage in output (API breaking change)
- [2.0.17] Language-neutral datapack reading (API breaking change)
- [2.0.15] Language-neutral datapack creation for C++ reverse-geocoder reuse
- [2.0.14] Point-only input indexing
- [1.0.1] Initial version — offline R-Tree indexing and fast lookup

See git history for the full changelog.

## CI

GitHub Actions CodeQL runs on `master` and pull requests (see `.github/workflows/codeql.yml`).
