/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

/**
 * Copies {@code book.alternate_title} into {@code alternate_titles text[]}
 * before Hibernate updates the schema.
 *
 * <p>{@code ddl-auto=update} does not convert a varchar column to {@code text[]}
 * or keep the old values. {@code entityManagerFactory} depends on this bean,
 * so the copy finishes first. A missing {@code book} table is skipped.
 * A second instance waits on the table lock, sees {@code text[]}, and copies nothing.
 */
@Component
@Slf4j
public class AlternateTitleMigration implements InitializingBean {

    private static final Set<String> TEXT_TYPES = Set.of("varchar", "text", "bpchar");

    private final DataSource dataSource;

    public AlternateTitleMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                convert(connection, "book");
                connection.commit();
            } catch (Exception ex) {
                rollback(connection);
                if (ex instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException("Could not convert alternate titles", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not convert alternate titles", ex);
        }
    }

    /**
     * Converts {@code alternate_title} on {@code table} into {@code alternate_titles}.
     * The connection must already be in a transaction.
     */
    static void convert(Connection connection, String table) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("Alternate title conversion must run inside a transaction");
        }
        String lockedTable = identifier(table);
        if (!tableExists(connection, lockedTable)) {
            log.info("Alternate titles skipped; table {} is not present yet", lockedTable);
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("LOCK TABLE " + lockedTable + " IN ACCESS EXCLUSIVE MODE");
        }
        String titlesType = udtName(connection, lockedTable, "alternate_titles");
        String oldType = udtName(connection, lockedTable, "alternate_title");
        if ("_text".equals(titlesType) && oldType == null) {
            log.info("Alternate titles on {} are already text[]", lockedTable);
            return;
        }
        if (titlesType != null && !"_text".equals(titlesType)) {
            throw new IllegalStateException(
                    "Unexpected type " + titlesType + " for " + lockedTable + ".alternate_titles");
        }
        if (oldType != null && !TEXT_TYPES.contains(oldType)) {
            throw new IllegalStateException(
                    "Unexpected type " + oldType + " for " + lockedTable + ".alternate_title");
        }
        if (titlesType == null) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + lockedTable + " ADD COLUMN alternate_titles text[]");
            }
        }
        if (oldType != null) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("UPDATE " + lockedTable + " SET alternate_titles = ARRAY[btrim(alternate_title)] "
                        + "WHERE alternate_title IS NOT NULL AND btrim(alternate_title) <> '' "
                        + "AND alternate_titles IS NULL");
                if ("book".equals(lockedTable)) {
                    statement.execute("DROP INDEX IF EXISTS idx_book_alternate_title");
                }
                statement.execute("ALTER TABLE " + lockedTable + " DROP COLUMN alternate_title");
            }
            log.info("Copied alternate_title into alternate_titles on {}", lockedTable);
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?")) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static String udtName(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT udt_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString(1) : null;
            }
        }
    }

    private static String identifier(String name) {
        if (name == null || !name.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Refusing table name " + name);
        }
        return name;
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            log.warn("Alternate title conversion rollback failed: {}", ex.getMessage());
        }
    }
}
