/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Copies {@code plot_summary} and {@code detailed_description} off PostgreSQL
 * large objects before the HTTP port opens.
 *
 * <p>Hibernate 6 stores a plain {@code @Lob String} as {@code oid}.
 * {@code ddl-auto=update} will not change that, and a cast of {@code oid} to
 * {@code text} would keep the object id. This reads each value with
 * {@code lo_get} inside one transaction, then unlinks only those objects.
 *
 * <p>{@code SmartInitializingSingleton} runs after the schema update and before
 * {@code WebServerStartStopLifecycle} accepts connections.
 * {@code ContextRefreshedEvent} is later, after that lifecycle has started.
 *
 * <p>A fresh database gets Hibernate's {@code LONGVARCHAR} mapping, which is
 * {@code varchar(32600)} here, not {@code oid}. {@code text} is what this
 * migration leaves behind. Both are safe to read with {@code getString} and
 * {@code length}. Any other type fails startup.
 */
@Component
@Slf4j
public class EssayColumnMigration implements SmartInitializingSingleton {

    static final List<String> BOOK_ESSAY_COLUMNS = List.of("plot_summary", "detailed_description");

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

    private final DataSource dataSource;

    public EssayColumnMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                convert(connection, "book", BOOK_ESSAY_COLUMNS);
                connection.commit();
            } catch (Exception ex) {
                rollback(connection);
                if (ex instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException("Could not convert essay columns", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not convert essay columns", ex);
        }
    }

    /**
     * Converts each column when it is still {@code oid}. The connection must
     * already be in a transaction. A second caller waits on the table lock,
     * sees {@code text} or {@code varchar}, and leaves the column alone.
     */
    static void convert(Connection connection, String table, List<String> columns) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("Essay conversion must run inside a transaction");
        }
        String lockedTable = identifier(table);
        try (Statement statement = connection.createStatement()) {
            statement.execute("LOCK TABLE " + lockedTable + " IN ACCESS EXCLUSIVE MODE");
        }
        for (String column : columns) {
            convertColumn(connection, lockedTable, identifier(column));
        }
    }

    private static void convertColumn(Connection connection, String table, String column) throws SQLException {
        long started = System.nanoTime();
        String udt = readUdtName(connection, table, column);
        if (udt == null) {
            throw new IllegalStateException("Missing column " + table + "." + column);
        }
        if ("text".equals(udt) || "varchar".equals(udt)) {
            log.info("Essay column {}.{} is already {}; left unchanged", table, column, udt);
            return;
        }
        if (!"oid".equals(udt)) {
            throw new IllegalStateException(
                    "Essay column " + table + "." + column + " has unexpected type " + udt);
        }
        List<Long> oids = readOids(connection, table, column);
        int copied = countCopied(connection, table, column);
        try (Statement statement = connection.createStatement()) {
            // PostgreSQL rejects a subquery in ALTER ... USING. Clear dangling
            // oids first, then copy the objects that are still there. Both
            // statements are in the caller's transaction.
            statement.execute("""
                    UPDATE %s
                    SET %s = NULL
                    WHERE %s IS NOT NULL
                      AND NOT EXISTS (
                        SELECT 1 FROM pg_largeobject_metadata WHERE oid = %s.%s
                      )
                    """.formatted(table, column, column, table, column));
            statement.execute("""
                    ALTER TABLE %s ALTER COLUMN %s TYPE text
                    USING CASE
                      WHEN %s IS NULL THEN NULL
                      ELSE convert_from(lo_get(%s), 'UTF8')
                    END
                    """.formatted(table, column, column, column));
        }
        int unlinked = unlink(connection, oids);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        log.info(
                "Essay column {}.{} converted from oid to text; copied {} values, unlinked {} large objects, {} ms",
                table, column, copied, unlinked, elapsedMs);
    }

    private static String readUdtName(Connection connection, String table, String column) throws SQLException {
        String sql = """
                SELECT udt_name
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                return rows.getString(1);
            }
        }
    }

    private static List<Long> readOids(Connection connection, String table, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM " + table + " WHERE " + column + " IS NOT NULL";
        Set<Long> oids = new LinkedHashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                oids.add(rows.getLong(1));
            }
        }
        return new ArrayList<>(oids);
    }

    private static int countCopied(Connection connection, String table, String column) throws SQLException {
        String sql = """
                SELECT count(*)
                FROM %s
                WHERE %s IS NOT NULL
                  AND EXISTS (
                    SELECT 1 FROM pg_largeobject_metadata WHERE oid = %s.%s
                  )
                """.formatted(table, column, table, column);
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static int unlink(Connection connection, List<Long> oids) throws SQLException {
        int unlinked = 0;
        String existsSql = "SELECT 1 FROM pg_largeobject_metadata WHERE oid = ?";
        try (PreparedStatement exists = connection.prepareStatement(existsSql);
             PreparedStatement unlink = connection.prepareStatement("SELECT lo_unlink(?)")) {
            for (Long oid : oids) {
                exists.setLong(1, oid);
                boolean present;
                try (ResultSet rows = exists.executeQuery()) {
                    present = rows.next();
                }
                if (!present) {
                    continue;
                }
                unlink.setLong(1, oid);
                try (ResultSet ignored = unlink.executeQuery()) {
                    // lo_unlink returns the removed oid.
                }
                unlinked++;
            }
        }
        return unlinked;
    }

    private static String identifier(String name) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalStateException("Refusing essay column identifier " + name);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private static void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            log.warn("Essay column conversion rollback failed", ex);
        }
    }
}
