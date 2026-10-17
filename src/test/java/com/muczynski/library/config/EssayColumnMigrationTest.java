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

            assertEquals("text", udtName(connection, TABLE, "essay"));
            assertEquals("Hello essay", essayText(connection, 1));
            assertNull(essayText(connection, 2));
            assertNull(essayText(connection, 3));
            assertFalse(largeObjectExists(connection, essayOid));
            assertTrue(largeObjectExists(connection, keptOid));

            EssayColumnMigration.convert(connection, TABLE, List.of("essay"));
            connection.commit();
            assertEquals("text", udtName(connection, TABLE, "essay"));
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

    @Test
    void restoresOidNumbersStoredAsVarchar() throws Exception {
        dropFixture();
        long keptOid = 0;
        long shortOid = 0;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE " + TABLE + " (id int primary key, essay varchar(32600))");
            }
            long essayOid = largeObject(connection, "Hello essay from the large object");
            keptOid = largeObject(connection, "leave this object");
            shortOid = largeObject(connection, "x");
            long dangling = nextMissingOid(connection);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + TABLE + " (id, essay) VALUES (1, ?), (2, ?), (3, ?), (4, ?)")) {
                insert.setString(1, Long.toString(essayOid));
                insert.setString(2, "A real sentence that is not an object id.");
                insert.setString(3, Long.toString(dangling));
                insert.setString(4, Long.toString(shortOid));
                insert.executeUpdate();
            }
            connection.commit();

            EssayColumnMigration.convert(connection, TABLE, List.of("essay"));
            connection.commit();

            assertEquals("text", udtName(connection, TABLE, "essay"));
            assertEquals("Hello essay from the large object", essayText(connection, 1));
            assertEquals("A real sentence that is not an object id.", essayText(connection, 2));
            assertEquals(Long.toString(dangling), essayText(connection, 3));
            assertEquals(Long.toString(shortOid), essayText(connection, 4));
            assertFalse(largeObjectExists(connection, essayOid));
            assertTrue(largeObjectExists(connection, keptOid));
            assertTrue(largeObjectExists(connection, shortOid));

            EssayColumnMigration.convert(connection, TABLE, List.of("essay"));
            connection.commit();
            assertEquals("text", udtName(connection, TABLE, "essay"));
            assertEquals("Hello essay from the large object", essayText(connection, 1));
            assertEquals("A real sentence that is not an object id.", essayText(connection, 2));
            assertEquals(Long.toString(dangling), essayText(connection, 3));
            assertTrue(largeObjectExists(connection, keptOid));
            assertTrue(largeObjectExists(connection, shortOid));
        } finally {
            dropFixture();
            unlinkQuietly(keptOid);
            unlinkQuietly(shortOid);
        }
    }

    @Test
    void keepsASharedLargeObjectUntilBothOidColumnsAreCopied() throws Exception {
        String table = "essay_lob_pair";
        dropTable(table);
        long shared = 0;
        long plotOnly = 0;
        long descriptionOnly = 0;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE essay_lob_pair (
                            id int primary key,
                            plot_summary oid,
                            detailed_description oid
                        )
                        """);
            }
            shared = largeObject(connection, "shared essay");
            plotOnly = largeObject(connection, "plot only essay");
            descriptionOnly = largeObject(connection, "description only essay");
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO essay_lob_pair (id, plot_summary, detailed_description)
                    VALUES (1, ?, ?), (2, ?, ?)
                    """)) {
                insert.setLong(1, shared);
                insert.setLong(2, shared);
                insert.setLong(3, plotOnly);
                insert.setLong(4, descriptionOnly);
                insert.executeUpdate();
            }
            connection.commit();

            EssayColumnMigration.convert(connection, table, List.of("plot_summary", "detailed_description"));
            connection.commit();

            assertEquals("text", udtName(connection, table, "plot_summary"));
            assertEquals("text", udtName(connection, table, "detailed_description"));
            assertEquals("shared essay", cell(connection, table, "plot_summary", 1));
            assertEquals("shared essay", cell(connection, table, "detailed_description", 1));
            assertEquals("plot only essay", cell(connection, table, "plot_summary", 2));
            assertEquals("description only essay", cell(connection, table, "detailed_description", 2));
            assertFalse(largeObjectExists(connection, shared));
            assertFalse(largeObjectExists(connection, plotOnly));
            assertFalse(largeObjectExists(connection, descriptionOnly));
        } finally {
            dropTable(table);
            unlinkQuietly(shared);
            unlinkQuietly(plotOnly);
            unlinkQuietly(descriptionOnly);
        }
    }

    @Test
    void restoresASharedOidStoredInBothVarcharColumns() throws Exception {
        String table = "essay_lob_pair";
        dropTable(table);
        long shared = 0;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE essay_lob_pair (
                            id int primary key,
                            plot_summary varchar(32600),
                            detailed_description varchar(32600)
                        )
                        """);
            }
            shared = largeObject(connection, "shared essay from the large object");
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO essay_lob_pair (id, plot_summary, detailed_description)
                    VALUES (1, ?, ?)
                    """)) {
                insert.setString(1, Long.toString(shared));
                insert.setString(2, Long.toString(shared));
                insert.executeUpdate();
            }
            connection.commit();

            EssayColumnMigration.convert(connection, table, List.of("plot_summary", "detailed_description"));
            connection.commit();

            assertEquals("text", udtName(connection, table, "plot_summary"));
            assertEquals("text", udtName(connection, table, "detailed_description"));
            assertEquals("shared essay from the large object", cell(connection, table, "plot_summary", 1));
            assertEquals("shared essay from the large object", cell(connection, table, "detailed_description", 1));
            assertFalse(largeObjectExists(connection, shared));

            EssayColumnMigration.convert(connection, table, List.of("plot_summary", "detailed_description"));
            connection.commit();
            assertEquals("shared essay from the large object", cell(connection, table, "plot_summary", 1));
            assertEquals("shared essay from the large object", cell(connection, table, "detailed_description", 1));
        } finally {
            dropTable(table);
            unlinkQuietly(shared);
        }
    }

    @Test
    void skipsAMissingTable() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            EssayColumnMigration.convert(connection, "essay_lob_missing", List.of("essay"));
            connection.commit();
        }
    }

    @Test
    void bookEssayColumnsAreText() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT column_name, udt_name
                     FROM information_schema.columns
                     WHERE table_schema = current_schema()
                       AND table_name = 'book'
                       AND column_name IN ('plot_summary', 'detailed_description')
                     ORDER BY column_name
                     """);
             ResultSet rows = statement.executeQuery()) {
            assertTrue(rows.next());
            assertEquals("detailed_description", rows.getString(1));
            assertEquals("text", rows.getString(2));
            assertTrue(rows.next());
            assertEquals("plot_summary", rows.getString(1));
            assertEquals("text", rows.getString(2));
            assertFalse(rows.next());
        }
    }

    private void dropFixture() throws SQLException {
        dropTable(TABLE);
    }

    private void dropTable(String table) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + table);
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

    private static String udtName(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT udt_name
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = ?
                """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getString(1);
            }
        }
    }

    private static String essayText(Connection connection, int id) throws SQLException {
        return cell(connection, TABLE, "essay", id);
    }

    private static String cell(Connection connection, String table, String column, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + column + " FROM " + table + " WHERE id = ?")) {
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
