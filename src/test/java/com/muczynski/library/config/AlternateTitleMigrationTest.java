/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixture is committed before conversion. A class-level {@code @Transactional}
 * test would hide those rows from the migrator's transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
class AlternateTitleMigrationTest {

    private static final String TABLE = "alt_title_fixture";

    @Autowired
    private DataSource dataSource;

    @Test
    void copiesVarcharIntoOneTextArrayElementAndLeavesBlanksNull() throws Exception {
        dropFixture();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE " + TABLE + " (id int primary key, alternate_title varchar(255))");
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + TABLE + " (id, alternate_title) VALUES (1, ?), (2, ?), (3, NULL), (4, ?)")) {
                insert.setString(1, "On Loving God");
                insert.setString(2, "   ");
                insert.setString(3, "");
                insert.executeUpdate();
            }
            connection.commit();

            AlternateTitleMigration.convert(connection, TABLE);
            connection.commit();

            assertEquals("_text", udtName(connection, "alternate_titles"));
            assertNull(udtName(connection, "alternate_title"));
            assertEquals(List.of("On Loving God"), titles(connection, 1));
            assertNull(titles(connection, 2));
            assertNull(titles(connection, 3));
            assertNull(titles(connection, 4));

            AlternateTitleMigration.convert(connection, TABLE);
            connection.commit();
            assertEquals("_text", udtName(connection, "alternate_titles"));
            assertNull(udtName(connection, "alternate_title"));
            assertEquals(List.of("On Loving God"), titles(connection, 1));
        } finally {
            dropFixture();
        }
    }

    @Test
    void refusesAnUnexpectedColumnType() throws Exception {
        dropFixture();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE " + TABLE + " (id int primary key, alternate_title integer)");
            }
            connection.commit();
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> AlternateTitleMigration.convert(connection, TABLE));
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

    private String udtName(Connection connection, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT udt_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?")) {
            statement.setString(1, TABLE);
            statement.setString(2, column);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString(1) : null;
            }
        }
    }

    private List<String> titles(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT alternate_titles FROM " + TABLE + " WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                Array array = rows.getArray(1);
                if (array == null) {
                    return null;
                }
                return Arrays.asList((String[]) array.getArray());
            }
        }
    }
}
