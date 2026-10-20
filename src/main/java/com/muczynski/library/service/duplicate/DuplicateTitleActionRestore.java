/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.AuthorState;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.BookState;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.FavoriteState;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.LoanState;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.PriceState;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Puts a deleted Duplicate Titles author or book back with its original id.
 * PostgreSQL identity columns accept an explicit id. The sequence is moved
 * forward so the next generated id does not collide.
 */
@Component
public class DuplicateTitleActionRestore {

    private final JdbcTemplate jdbc;

    public DuplicateTitleActionRestore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertAuthor(AuthorState author) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO author (
                        id, name, alternate_names, date_of_birth, date_of_death,
                        religious_affiliation, birth_country, nationality, brief_biography,
                        grokipedia_url, last_modified)
                    VALUES (?, ?, ?, ?, ?, lo_from_bytea(0, ?), lo_from_bytea(0, ?), lo_from_bytea(0, ?), lo_from_bytea(0, ?), ?, CURRENT_TIMESTAMP)
                    """)) {
                statement.setLong(1, author.getId());
                statement.setString(2, author.getName());
                String[] alternates = author.getAlternateNames() == null
                        ? new String[0]
                        : author.getAlternateNames().toArray(String[]::new);
                Array names = connection.createArrayOf("text", alternates);
                statement.setArray(3, names);
                setDate(statement, 4, author.getDateOfBirth());
                setDate(statement, 5, author.getDateOfDeath());
                setOidText(statement, 6, author.getReligiousAffiliation());
                setOidText(statement, 7, author.getBirthCountry());
                setOidText(statement, 8, author.getNationality());
                setOidText(statement, 9, author.getBiographicalEssay());
                statement.setString(10, author.getGrokipediaUrl());
                statement.executeUpdate();
            }
            return null;
        });
        advanceSequence("author");
    }

    public void insertBook(BookState book) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO book (
                        id, title, alternate_title, publication_year, publisher, plot_summary,
                        related_works, detailed_description, grokipedia_url, free_text_url,
                        date_added_to_library, last_modified, status, loc_number, electronic_resource,
                        status_reason, ydl_audio_available, ydl_paper_available, ydl_ebook_available,
                        ydl_last_checked, ydl_lookup_error, emu_audio_available, emu_paper_available,
                        emu_ebook_available, emu_last_checked, emu_lookup_error, acla_audio_available,
                        acla_paper_available, acla_ebook_available, acla_last_checked, acla_lookup_error,
                        reading_difficulty, binding, desire_to_purchase, author_id, library_id)
                    VALUES (?, ?, ?, ?, ?, ?, lo_from_bytea(0, ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                setLong(statement, 1, book.getId());
                setString(statement, 2, book.getTitle());
                setString(statement, 3, book.getAlternateTitle());
                setInteger(statement, 4, book.getPublicationYear());
                setString(statement, 5, book.getPublisher());
                setString(statement, 6, book.getPlotEssay());
                setOidText(statement, 7, book.getRelatedWorks());
                setString(statement, 8, book.getDetailedDescription());
                setString(statement, 9, book.getGrokipediaUrl());
                setString(statement, 10, book.getFreeTextUrl());
                setTimestamp(statement, 11, book.getDateAddedToLibrary());
                setTimestamp(statement, 12, book.getLastModified());
                setString(statement, 13, book.getStatus());
                setString(statement, 14, book.getLocNumber());
                setBoolean(statement, 15, book.getElectronicResource());
                setString(statement, 16, book.getStatusReason());
                setBoolean(statement, 17, book.getYdlAudioAvailable());
                setBoolean(statement, 18, book.getYdlPaperAvailable());
                setBoolean(statement, 19, book.getYdlEbookAvailable());
                setTimestamp(statement, 20, book.getYdlLastChecked());
                setString(statement, 21, book.getYdlLookupError());
                setBoolean(statement, 22, book.getEmuAudioAvailable());
                setBoolean(statement, 23, book.getEmuPaperAvailable());
                setBoolean(statement, 24, book.getEmuEbookAvailable());
                setTimestamp(statement, 25, book.getEmuLastChecked());
                setString(statement, 26, book.getEmuLookupError());
                setBoolean(statement, 27, book.getAclaAudioAvailable());
                setBoolean(statement, 28, book.getAclaPaperAvailable());
                setBoolean(statement, 29, book.getAclaEbookAvailable());
                setTimestamp(statement, 30, book.getAclaLastChecked());
                setString(statement, 31, book.getAclaLookupError());
                setString(statement, 32, book.getReadingDifficulty());
                setString(statement, 33, book.getBinding());
                setInteger(statement, 34, book.getDesireToPurchase());
                setLong(statement, 35, book.getAuthorId());
                setLong(statement, 36, book.getLibraryId());
                statement.executeUpdate();
            }
            return null;
        });
        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                jdbc.update("INSERT INTO book_tags (book_id, tag) VALUES (?, ?)", book.getId(), tag);
            }
        }
        advanceSequence("book");
    }

    public void insertLoan(LoanState loan) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO loan (id, book_id, user_id, loan_date, due_date, return_date, last_modified)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                setLong(statement, 1, loan.getId());
                setLong(statement, 2, loan.getBookId());
                setLong(statement, 3, loan.getUserId());
                setDate(statement, 4, loan.getLoanDate());
                setDate(statement, 5, loan.getDueDate());
                setDate(statement, 6, loan.getReturnDate());
                setTimestamp(statement, 7, loan.getLastModified());
                statement.executeUpdate();
            }
            return null;
        });
        advanceSequence("loan");
    }

    public void insertPrice(PriceState price) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO book_price (
                        id, book_id, cover, price_dollars, shipping_dollars, condition,
                        looked_up_at, details_url, lookup_error, last_modified)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                setLong(statement, 1, price.getId());
                setLong(statement, 2, price.getBookId());
                setString(statement, 3, price.getCover());
                setDecimal(statement, 4, price.getPriceDollars());
                setDecimal(statement, 5, price.getShippingDollars());
                setString(statement, 6, price.getCondition());
                setTimestamp(statement, 7, price.getLookedUpAt());
                setString(statement, 8, price.getDetailsUrl());
                setString(statement, 9, price.getLookupError());
                setTimestamp(statement, 10, price.getLastModified());
                statement.executeUpdate();
            }
            return null;
        });
        advanceSequence("book_price");
    }

    public void insertFavorite(FavoriteState favorite) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO favorites (id, user_id, book_id, author_id, list_name, last_modified)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                setLong(statement, 1, favorite.getId());
                setLong(statement, 2, favorite.getUserId());
                setLong(statement, 3, favorite.getBookId());
                setLong(statement, 4, favorite.getAuthorId());
                setString(statement, 5, favorite.getListName());
                setTimestamp(statement, 6, favorite.getLastModified());
                statement.executeUpdate();
            }
            return null;
        });
        advanceSequence("favorites");
    }

    /**
     * Hibernate maps these {@code @Lob} strings to PostgreSQL {@code oid}.
     * {@code lo_from_bytea} stores the text and returns null when the bytes are null.
     */
    private static void setOidText(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BINARY);
        } else {
            statement.setBytes(index, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void setString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setBoolean(PreparedStatement statement, int index, Boolean value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BOOLEAN);
        } else {
            statement.setBoolean(index, value);
        }
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setObject(index, value);
        }
    }

    private static void setTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setObject(index, value);
        }
    }

    private static void setDecimal(PreparedStatement statement, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.NUMERIC);
        } else {
            statement.setBigDecimal(index, value);
        }
    }

    public void pointBooksAtAuthor(List<Long> bookIds, long authorId) {
        updateIds("UPDATE book SET author_id = ? WHERE id = ?", authorId, bookIds);
    }

    public void pointPhotosAtAuthor(List<Long> photoIds, long authorId) {
        updateIds("UPDATE photo SET author_id = ? WHERE id = ?", authorId, photoIds);
    }

    public void pointFavoritesAtAuthor(List<Long> favoriteIds, long authorId) {
        updateIds("UPDATE favorites SET author_id = ? WHERE id = ?", authorId, favoriteIds);
    }

    public void pointPhotosAtBook(List<Long> photoIds, long bookId) {
        updateIds("UPDATE photo SET book_id = ? WHERE id = ?", bookId, photoIds);
    }

    public void pointLoansAtBook(List<Long> loanIds, long bookId) {
        updateIds("UPDATE loan SET book_id = ? WHERE id = ?", bookId, loanIds);
    }

    public void pointPricesAtBook(List<Long> priceIds, long bookId) {
        updateIds("UPDATE book_price SET book_id = ? WHERE id = ?", bookId, priceIds);
    }

    public void pointFavoritesAtBook(List<Long> favoriteIds, long bookId) {
        updateIds("UPDATE favorites SET book_id = ? WHERE id = ?", bookId, favoriteIds);
    }

    private void updateIds(String sql, long ownerId, List<Long> ids) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                jdbc.update(sql, ownerId, id);
            }
        }
    }

    private void advanceSequence(String table) {
        try {
            jdbc.queryForObject(
                    "SELECT setval(pg_get_serial_sequence(?, 'id'), (SELECT COALESCE(MAX(id), 1) FROM "
                            + table + "))",
                    Long.class,
                    table);
        } catch (RuntimeException ex) {
            throw new LibraryException("Could not advance the id sequence for " + table, ex);
        }
    }
}
