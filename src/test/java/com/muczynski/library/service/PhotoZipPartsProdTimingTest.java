/*
 * (c) Copyright 2025 by Muczynski
 *
 * TEMPORARY — delete before committing.
 * Times the zip-parts computation against the production database.
 */
package com.muczynski.library.service;

import com.muczynski.library.dto.PhotoZipPartDto;
import com.muczynski.library.repository.PhotoZipSortProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Connects to the production Cloud SQL database (via the postgres-socket-factory)
 * and measures how long the zip-parts query and algorithm take.
 *
 * Requires the following environment variables (the test is skipped if GCP_PROJECT_ID
 * or DB_PASSWORD are absent):
 *   GCP_PROJECT_ID  — GCP project that owns the Cloud SQL instance
 *   DB_PASSWORD     — PostgreSQL password for the "postgres" user
 *   GCP_REGION      — (optional, defaults to "us-east1")
 *   DB_NAME         — (optional, defaults to "library")
 */
@EnabledIfEnvironmentVariable(named = "GCP_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD",    matches = ".+")
class PhotoZipPartsProdTimingTest {

    private static final String INSTANCE_SUFFIX = "scrabble-db";

    @Test
    void timeProdZipPartsComputation() throws Exception {
        String projectId = System.getenv("GCP_PROJECT_ID");
        String region    = Optional.ofNullable(System.getenv("GCP_REGION")).orElse("us-east1");
        String dbName    = Optional.ofNullable(System.getenv("DB_NAME")).orElse("library");
        String password  = System.getenv("DB_PASSWORD");

        String instanceName = projectId + ":" + region + ":" + INSTANCE_SUFFIX;
        String jdbcUrl = "jdbc:postgresql:///" + dbName
                + "?cloudSqlInstance=" + instanceName
                + "&socketFactory=com.google.cloud.sql.postgres.SocketFactory";

        System.out.println("Connecting to: " + instanceName + "/" + dbName);

        long t0 = System.currentTimeMillis();

        List<PhotoZipSortProjection> sortData;
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "postgres", password)) {
            long tConnected = System.currentTimeMillis();
            System.out.printf("Connection established:      %4d ms%n", tConnected - t0);

            sortData = runSortQuery(conn);
            long tQueried = System.currentTimeMillis();
            System.out.printf("findAllSortKeysForZip query: %4d ms  (%d rows)%n",
                    tQueried - tConnected, sortData.size());
        }

        long tQueryDone = System.currentTimeMillis();

        List<PhotoZipPartDto> parts = PhotoExportService.computeZipPartsFromSortData(sortData);
        long tAlgorithmDone = System.currentTimeMillis();
        System.out.printf("computeZipPartsFromSortData: %4d ms%n", tAlgorithmDone - tQueryDone);
        System.out.printf("Total (query + algorithm):   %4d ms%n", tAlgorithmDone - tQueryDone + (tQueryDone - t0));

        System.out.println();
        System.out.println("Resulting zip parts:");
        int grandTotal = parts.stream().mapToInt(PhotoZipPartDto::getPhotoCount).sum();
        for (PhotoZipPartDto part : parts) {
            System.out.printf("  Part %d of %d: %-20s  %d of %d photos  (~%d MB estimated)%n",
                    part.getPartNumber(), part.getTotalParts(),
                    part.getRangeLabel(),
                    part.getPhotoCount(), grandTotal,
                    part.getEstimatedMb());
        }

        assertThat(parts).isNotEmpty();
        assertThat(grandTotal).isGreaterThan(0);
    }

    private static List<PhotoZipSortProjection> runSortQuery(Connection conn) throws SQLException {
        String sql = """
                SELECT p.id AS id,
                       COALESCE(b.title, a.name, lb.title) AS sortName
                FROM photo p
                LEFT JOIN book b   ON p.book_id   = b.id
                LEFT JOIN author a ON p.author_id  = a.id
                LEFT JOIN loan l   ON p.loan_id    = l.id
                LEFT JOIN book lb  ON l.book_id    = lb.id
                WHERE p.deleted_at IS NULL
                ORDER BY p.id
                """;

        List<PhotoZipSortProjection> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id        = rs.getLong("id");
                String sortName = rs.getString("sortName");
                result.add(new PhotoZipSortProjection() {
                    @Override public Long   getId()       { return id; }
                    @Override public String getSortName() { return sortName; }
                });
            }
        }
        return result;
    }
}
