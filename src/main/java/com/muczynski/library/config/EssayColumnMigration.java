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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Copies {@code plot_summary} and {@code detailed_description} off PostgreSQL
 * large objects before Hibernate updates the schema.
 *
 * <p>Hibernate 6 stores a plain {@code @Lob String} as {@code oid}. Schema
 * update then casts that {@code oid} to {@code varchar} and keeps the object
 * id, which is what 2.4.68 did on the dev database. The bytes stayed in
 * {@code pg_largeobject}. This reads them with {@code lo_get} and unlinks
 * only the objects it copied.
 *
 * <p>{@code entityManagerFactory} depends on this bean, so
 * {@link #afterPropertiesSet()} finishes before that schema update. A missing
 * {@code book} table is skipped; Hibernate creates {@code text} columns on a
 * fresh database. A second instance waits on the table lock, sees {@code text},
 * and copies nothing.
 *
 * <p>Each column is committed before its large objects are unlinked.
 * {@code lo_unlink} holds one lock per object until commit, and unlinking both
 * columns in one transaction ran out of shared memory on Cloud SQL.
 */
@Component
@Slf4j
public class EssayColumnMigration implements InitializingBean {

    static final List<String> BOOK_ESSAY_COLUMNS = List.of("plot_summary", "detailed_description");

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

    private static final long MAX_OID = 4294967295L;

    /** Small enough that one batch stays under Cloud SQL's lock-table limit. */
    private static final int UNLINK_BATCH = 500;

    private final DataSource dataSource;

    public EssayColumnMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
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
     * Converts each column when it is still {@code oid}, and copies large
     * objects back into {@code varchar} or {@code text} values that are only
     * the object id. The connection must already be in a transaction.
     * Each finished column is committed, including its unlinks.
     */
    static void convert(Connection connection, String table, List<String> columns) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("Essay conversion must run inside a transaction");
        }
        String lockedTable = identifier(table);
        if (!tableExists(connection, lockedTable)) {
            log.info("Essay columns skipped; table {} is not present yet", lockedTable);
            return;
        }
        List<String> names = new ArrayList<>();
        for (String column : columns) {
            names.add(identifier(column));
        }
        for (int index = 0; index < names.size(); index++) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("LOCK TABLE " + lockedTable + " IN ACCESS EXCLUSIVE MODE");
            }
            List<String> siblings = new ArrayList<>(names);
            siblings.remove(index);
            convertColumn(connection, lockedTable, names.get(index), siblings);
        }
    }

    private static void convertColumn(
            Connection connection, String table, String column, List<String> siblings) throws SQLException {
        long started = System.nanoTime();
        String udt = readUdtName(connection, table, column);
        if (udt == null) {
            log.info("Essay column {}.{} is absent; left for schema creation", table, column);
            connection.commit();
            return;
        }
        if ("varchar".equals(udt) || "text".equals(udt)) {
            restoreTextColumn(connection, table, column, siblings, udt, started);
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
        int unlinked = commitAndUnlink(connection, oids, oidsStillStored(connection, table, siblings));
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        log.info(
                "Essay column {}.{} converted from oid to text; copied {} values, unlinked {} large objects, {} ms",
                table, column, copied, unlinked, elapsedMs);
    }

    /**
     * 2.4.68 cast {@code oid} to {@code varchar(32600)} and stored the object
     * id. Widen that to {@code text}, then replace a value only when it is
     * the id of a large object whose text is longer than those digits. Real
     * prose and dangling numbers stay.
     */
    private static void restoreTextColumn(
            Connection connection,
            String table,
            String column,
            List<String> siblings,
            String udt,
            long started) throws SQLException {
        boolean widened = false;
        if ("varchar".equals(udt)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE text");
            }
            widened = true;
        }
        Restored restored = restoreOidStrings(connection, table, column);
        int unlinked = commitAndUnlink(connection, restored.oids, oidsStillStored(connection, table, siblings));
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        if (widened) {
            log.info(
                    "Essay column {}.{} widened from varchar to text; restored {} values, unlinked {} large objects, {} ms",
                    table, column, restored.rows, unlinked, elapsedMs);
            return;
        }
        if (restored.rows == 0) {
            log.info("Essay column {}.{} is text; no large-object ids to restore", table, column);
            return;
        }
        log.info(
                "Essay column {}.{} restored {} values from large objects, unlinked {} large objects, {} ms",
                table, column, restored.rows, unlinked, elapsedMs);
    }

    private static Restored restoreOidStrings(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TEMP TABLE IF NOT EXISTS essay_lob_restore (
                        oid oid PRIMARY KEY,
                        body text
                    ) ON COMMIT DROP
                    """);
            statement.execute("TRUNCATE essay_lob_restore");
            statement.execute("""
                    INSERT INTO essay_lob_restore (oid, body)
                    SELECT DISTINCT ON (src.oid) src.oid, src.body
                    FROM (
                        SELECT %1$s::oid AS oid,
                               convert_from(lo_get(%1$s::oid), 'UTF8') AS body,
                               char_length(%1$s) AS stored
                        FROM %2$s
                        WHERE %1$s ~ '^[0-9]{1,10}$'
                          AND %1$s::numeric <= 4294967295
                          AND EXISTS (
                            SELECT 1 FROM pg_largeobject_metadata meta
                            WHERE meta.oid = %2$s.%1$s::oid
                          )
                    ) src
                    WHERE length(src.body) > src.stored
                    ORDER BY src.oid
                    """.formatted(column, table));
            int rows = statement.executeUpdate("""
                    UPDATE %2$s AS target
                    SET %1$s = restored.body
                    FROM essay_lob_restore restored
                    WHERE target.%1$s ~ '^[0-9]{1,10}$'
                      AND target.%1$s::numeric <= 4294967295
                      AND target.%1$s::oid = restored.oid
                    """.formatted(column, table));
            List<Long> oids = new ArrayList<>();
            try (ResultSet results = statement.executeQuery("SELECT oid FROM essay_lob_restore")) {
                while (results.next()) {
                    oids.add(results.getLong(1));
                }
            }
            return new Restored(rows, oids);
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        String sql = """
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                  AND table_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
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

    /**
     * Oids still stored as digits in a column this pass did not convert.
     * Unlinking one of those would drop the other column's essay.
     */
    private static Set<Long> oidsStillStored(Connection connection, String table, List<String> siblings)
            throws SQLException {
        Set<Long> stored = new HashSet<>();
        for (String sibling : siblings) {
            String udt = readUdtName(connection, table, sibling);
            if (udt == null || (!"oid".equals(udt) && !"text".equals(udt) && !"varchar".equals(udt))) {
                continue;
            }
            String sql = """
                    SELECT %s::text
                    FROM %s
                    WHERE %s IS NOT NULL
                      AND %s::text ~ '^[0-9]{1,10}$'
                    """.formatted(sibling, table, sibling, sibling);
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(sql)) {
                while (rows.next()) {
                    stored.add(Long.parseLong(rows.getString(1)));
                }
            }
        }
        stored.removeIf(oid -> oid < 0 || oid > MAX_OID);
        return stored;
    }

    /**
     * Saves the column text before unlinking. The keep set is the ids still
     * stored in a sibling column, captured before the commit.
     */
    private static int commitAndUnlink(Connection connection, List<Long> oids, Set<Long> keep) throws SQLException {
        List<Long> toUnlink = new ArrayList<>();
        for (Long oid : oids) {
            if (!keep.contains(oid)) {
                toUnlink.add(oid);
            }
        }
        connection.commit();
        int unlinked = 0;
        for (int start = 0; start < toUnlink.size(); start += UNLINK_BATCH) {
            int end = Math.min(start + UNLINK_BATCH, toUnlink.size());
            unlinked += unlink(connection, toUnlink.subList(start, end), Set.of());
            connection.commit();
        }
        return unlinked;
    }

    private static int unlink(Connection connection, List<Long> oids, Set<Long> keep) throws SQLException {
        int unlinked = 0;
        String existsSql = "SELECT 1 FROM pg_largeobject_metadata WHERE oid = ?";
        try (PreparedStatement exists = connection.prepareStatement(existsSql);
             PreparedStatement unlink = connection.prepareStatement("SELECT lo_unlink(?)")) {
            for (Long oid : oids) {
                if (keep.contains(oid)) {
                    continue;
                }
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

    private record Restored(int rows, List<Long> oids) {
    }
}
