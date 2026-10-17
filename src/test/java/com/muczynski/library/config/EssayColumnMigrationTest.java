/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixture is committed before conversion. A class-level {@code @Transactional}
 * test would hide those rows from the migrator's transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
class EssayColumnMigrationTest {

    private static final String TABLE = "essay_lob_fixture";

    @Autowired
    private DataSource dataSource;

    @Test
    void convertsOidEssaysAndSkipsText() throws Exception {
        dropFixture();
        long keptOid = 0;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE " + TABLE + " (id int primary key, essay oid)");
            }
            long essayOid = largeObject(connection, "Hello essay");
            keptOid = largeObject(connection, "leave this object");
            long dangling = nextMissingOid(connection);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + TABLE + " (id, essay) VALUES (1, ?), (2, NULL), (3, ?)")) {
                insert.setLong(1, essayOid);
                insert.setLong(2, dangling);
                insert.executeUpdate();
            }
            connection.commit();

            EssayColumnMigration.convert(connection, TABLE, List.of("essay"));
            connection.commit();

            assertEquals("text", udtName(connection));
            assertEquals("Hello essay", essayText(connection, 1));
            assertNull(essayText(connection, 2));
            assertNull(essayText(connection, 3));
            assertFalse(largeObjectExists(connection, essayOid));
            assertTrue(largeObjectExists(connection, keptOid));

            EssayColumnMigration.convert(connection, TABLE, List.of("essay"));
            connection.commit();
            assertEquals("text", udtName(connection));
            assertEquals("Hello essay", essayText(connection, 1));
            assertTrue(largeObjectExists(connection, keptOid));
        } finally {
            dropFixture();
            unlinkQuietly(keptOid);
        }
    }

    @Test
    void refusesAnUnexpectedColumnType() throws Exception {
        dropFixture();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE " + TABLE + " (id int primary key, essay integer)");
            }
            connection.commit();
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> EssayColumnMigration.convert(connection, TABLE, List.of("essay")));
            assertTrue(error.getMessage().contains("int4"));
            connection.rollback();
        } finally {
            dropFixture();
        }
    }

    private void dropFixture() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + TABLE);
        }
    }

    private static long largeObject(Connection connection, String text) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT lo_from_bytea(0, convert_to(?, 'UTF8'))")) {
            statement.setString(1, text);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getLong(1);
            }
        }
    }

    private static long nextMissingOid(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT COALESCE(MAX(oid)::bigint, 0) + 1 FROM pg_largeobject_metadata")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private static String udtName(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT udt_name
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = 'essay'
                """)) {
            statement.setString(1, TABLE);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getString(1);
            }
        }
    }

    private static String essayText(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT essay FROM " + TABLE + " WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getString(1);
            }
        }
    }

    private static boolean largeObjectExists(Connection connection, long oid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM pg_largeobject_metadata WHERE oid = ?")) {
            statement.setLong(1, oid);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private void unlinkQuietly(long oid) {
        if (oid == 0) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("SELECT lo_unlink(?)")) {
                statement.setLong(1, oid);
                try (ResultSet ignored = statement.executeQuery()) {
                    // consume
                }
            }
            connection.commit();
        } catch (SQLException ex) {
            // The container is discarded with the test JVM.
        }
    }
}
