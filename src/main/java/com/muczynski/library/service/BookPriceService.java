/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.dto.BookPriceDto;
import com.muczynski.library.dto.BookPriceLookupResultDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.exception.AbeBooksHttpException;
import com.muczynski.library.exception.AbeBooksRateLimitedException;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.BookPriceRepository;
import com.muczynski.library.repository.BookRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookPriceService {

    /** Cap so retries=20 cannot wait days via unbounded exponential backoff. */
    static final long MAX_RATE_LIMIT_BACKOFF_MS = 300_000L;

    /** Stored when AbeBooks returned a real SearchResults page with no usable listing. */
    public static final String NO_MATCHING_LISTING = "No matching listing";

    private final BookRepository bookRepository;
    private final BookPriceRepository bookPriceRepository;
    private final AbeBooksClient abeBooksClient;
    private final TransactionTemplate transactionTemplate;
    private final DataSource dataSource;
    private final int rateLimitRetries;
    private final long rateLimitBackoffMs;

    public BookPriceService(BookRepository bookRepository,
                            BookPriceRepository bookPriceRepository,
                            AbeBooksClient abeBooksClient) {
        this(bookRepository, bookPriceRepository, abeBooksClient, 0, 0, null, null);
    }

    public BookPriceService(BookRepository bookRepository,
                            BookPriceRepository bookPriceRepository,
                            AbeBooksClient abeBooksClient,
                            int rateLimitRetries,
                            long rateLimitBackoffMs) {
        this(bookRepository, bookPriceRepository, abeBooksClient, rateLimitRetries, rateLimitBackoffMs, null, null);
    }

    @Autowired
    public BookPriceService(BookRepository bookRepository,
                            BookPriceRepository bookPriceRepository,
                            AbeBooksClient abeBooksClient,
                            @Value("${abebooks.rate-limit-retries:2}") int rateLimitRetries,
                            @Value("${abebooks.rate-limit-backoff-ms:4000}") long rateLimitBackoffMs,
                            PlatformTransactionManager transactionManager,
                            DataSource dataSource) {
        this.bookRepository = bookRepository;
        this.bookPriceRepository = bookPriceRepository;
        this.abeBooksClient = abeBooksClient;
        this.rateLimitRetries = rateLimitRetries;
        this.rateLimitBackoffMs = rateLimitBackoffMs;
        if (transactionManager == null) {
            this.transactionTemplate = null;
        } else {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.transactionTemplate = template;
        }
        this.dataSource = dataSource;
    }

    @Transactional(readOnly = true)
    public List<BookPriceDto> listAll() {
        return bookPriceRepository.findAllWithBookAndAuthor().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookSummaryDto> getAllPriceSummaries() {
        return bookPriceRepository.findAllPriceSummaries();
    }

    @Transactional(readOnly = true)
    public List<BookPriceDto> getPricesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return bookPriceRepository.findByIdsWithBookAndAuthor(ids).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Fetch prices scoped to a list of book IDs (for filtered Books/Prices pages).
     * Uses the new repository query that joins on book.id. Empty list returns empty.
     * Reuses existing toDto() and JOIN FETCH for book/author data.
     */
    @Transactional(readOnly = true)
    public List<BookPriceDto> getPricesByBookIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }
        return bookPriceRepository.findByBookIdsWithBookAndAuthor(bookIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Looks up AbeBooks prices for a book and upserts one {@link BookPrice} row
     * per cover. Listings with no hardcover/softcover/library-binding/other
     * binding are stored as {@link BookCoverType#UNKNOWN}. Library binding and
     * other named bindings are saved when parsed from the listing.
     *
     * <p>AbeBooks HTTP and throttling sleeps run <em>outside</em> a database
     * transaction so the Hikari pool (size 3) stays available for other tabs
     * such as Data Management. {@link Propagation#NOT_SUPPORTED} suspends any
     * caller transaction for the duration of the HTTP work.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BookPriceLookupResultDto lookupAndUpdateBook(Long bookId) {
        PreparedLookup prepared = inTransaction(() -> prepareLookup(bookId));
        if (prepared.completed != null) {
            return prepared.completed;
        }

        log.info("AbeBooks lookup starting bookId={} title='{}' author='{}'",
                bookId, prepared.title, prepared.authorName);
        logPool("before-http", bookId);
        long started = System.nanoTime();
        try {
            AbeBooksCoverListings found = findWithBackoff(prepared.title, prepared.authorName);
            long elapsedMs = elapsedMs(started);
            log.info("AbeBooks lookup HTTP finished bookId={} elapsedMs={} hardcover={} softcover={} libraryBinding={}",
                    bookId, elapsedMs,
                    found.getHardcover() != null, found.getSoftcover() != null,
                    found.getLibraryBinding() != null);
            logPool("after-http", bookId);
            return inTransaction(() -> saveFoundResult(bookId, found));
        } catch (AbeBooksRateLimitedException ex) {
            long elapsedMs = elapsedMs(started);
            log.warn("AbeBooks lookup rate-limited bookId={} title='{}' elapsedMs={}",
                    bookId, prepared.title, elapsedMs);
            logPool("rate-limited", bookId);
            return inTransaction(() -> saveErrorResult(
                    bookId, AbeBooksRateLimitedException.MESSAGE, true, ex.getSearchUrl()));
        } catch (AbeBooksHttpException ex) {
            long elapsedMs = elapsedMs(started);
            log.warn("AbeBooks lookup HTTP {} bookId={} title='{}' elapsedMs={}",
                    ex.getStatusCode(), bookId, prepared.title, elapsedMs);
            logPool("http-error", bookId);
            return inTransaction(() -> saveErrorResult(
                    bookId, ex.getMessage(), false, ex.getSearchUrl()));
        } catch (HttpStatusCodeException ex) {
            long elapsedMs = elapsedMs(started);
            int code = ex.getStatusCode().value();
            log.warn("AbeBooks lookup HTTP {} bookId={} title='{}' elapsedMs={}",
                    code, bookId, prepared.title, elapsedMs);
            logPool("http-error", bookId);
            return inTransaction(() -> saveErrorResult(
                    bookId, AbeBooksHttpException.messageFor(code), false, null));
        } catch (Exception ex) {
            long elapsedMs = elapsedMs(started);
            log.warn("AbeBooks lookup failed bookId={} title='{}' elapsedMs={}: {}",
                    bookId, prepared.title, elapsedMs, ex.getMessage(), ex);
            logPool("failed", bookId);
            String message = ex.getMessage() == null ? "AbeBooks lookup failed" : ex.getMessage();
            return inTransaction(() -> saveErrorResult(bookId, truncate(message, 500), false, null));
        }
    }

    private PreparedLookup prepareLookup(Long bookId) {
        Book book = requireBook(bookId);
        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : null;
        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            return new PreparedLookup(null, null,
                    saveErrorResult(book, "Not Ready - Temporary title", false, null));
        }
        return new PreparedLookup(book.getTitle(), authorName, null);
    }

    private BookPriceLookupResultDto saveFoundResult(Long bookId, AbeBooksCoverListings found) {
        Book book = requireBook(bookId);
        CoverSaveResult saved = saveFoundCovers(book, found, found.getSearchUrl());
        boolean success = hasListing(saved.hardcover)
                || hasListing(saved.softcover)
                || hasListing(saved.libraryBinding)
                || hasListing(saved.other);
        String error = success ? null : joinErrors(saved.hardcover, saved.softcover);
        return BookPriceLookupResultDto.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .success(success)
                .hardcover(toDto(saved.hardcover))
                .softcover(toDto(saved.softcover))
                .libraryBinding(toDto(saved.libraryBinding))
                .errorMessage(error)
                .build();
    }

    private BookPriceLookupResultDto saveErrorResult(Long bookId, String message, boolean rateLimited,
                                                    String searchUrl) {
        return saveErrorResult(requireBook(bookId), message, rateLimited, searchUrl);
    }

    private BookPriceLookupResultDto saveErrorResult(Book book, String message, boolean rateLimited,
                                                    String searchUrl) {
        deleteCover(book, BookCoverType.UNKNOWN);
        BookPrice hardcover = saveError(book, BookCoverType.HARDCOVER, message, searchUrl);
        BookPrice softcover = saveError(book, BookCoverType.SOFTCOVER, message, searchUrl);
        return BookPriceLookupResultDto.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .success(false)
                .rateLimited(rateLimited)
                .hardcover(toDto(hardcover))
                .softcover(toDto(softcover))
                .errorMessage(message)
                .build();
    }

    private Book requireBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new LibraryException("Book not found: " + bookId));
    }

    private AbeBooksCoverListings findWithBackoff(String title, String author) {
        AbeBooksRateLimitedException last = null;
        int attempts = Math.max(1, rateLimitRetries + 1);
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (attempt > 0) {
                long waitMs = backoffMsForAttempt(attempt - 1, rateLimitBackoffMs);
                log.info("AbeBooks backoff {} ms before retry {} of {} for title '{}'",
                        waitMs, attempt, rateLimitRetries, title);
                sleepQuietly(waitMs);
            }
            try {
                return abeBooksClient.findCheapestGoodOrBetter(title, author);
            } catch (AbeBooksRateLimitedException ex) {
                last = ex;
            }
        }
        throw last != null ? last : new AbeBooksRateLimitedException();
    }

    /**
     * Exponential backoff capped at 8× the base, then at
     * {@link #MAX_RATE_LIMIT_BACKOFF_MS}, so a large retry count cannot stall
     * a lookup for days.
     */
    static long backoffMsForAttempt(int zeroBasedRetry, long baseMs) {
        if (baseMs <= 0 || zeroBasedRetry < 0) {
            return 0;
        }
        int shift = Math.min(zeroBasedRetry, 3);
        long raw = baseMs * (1L << shift);
        return Math.min(raw, MAX_RATE_LIMIT_BACKOFF_MS);
    }

    private static void sleepQuietly(long waitMs) {
        if (waitMs <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AbeBooksRateLimitedException("AbeBooks lookup interrupted", ex);
        }
    }

    private CoverSaveResult saveFoundCovers(Book book, AbeBooksCoverListings found, String searchUrl) {
        AbeBooksListing hcListing = found.getHardcover();
        AbeBooksListing scListing = found.getSoftcover();
        boolean hcUnknown = isUnknownBinding(hcListing);
        boolean scUnknown = isUnknownBinding(scListing);

        BookPrice unknown = null;
        if (hcUnknown || scUnknown) {
            AbeBooksListing unknownListing = hcUnknown ? hcListing : scListing;
            unknown = saveListing(book, BookCoverType.UNKNOWN, unknownListing);
        } else {
            deleteCover(book, BookCoverType.UNKNOWN);
        }

        BookPrice hardcover;
        if (hcListing != null && !hcUnknown) {
            hardcover = saveListing(book, BookCoverType.HARDCOVER, hcListing);
        } else if (hcUnknown) {
            deleteCover(book, BookCoverType.HARDCOVER);
            hardcover = unknown;
        } else {
            hardcover = saveError(book, BookCoverType.HARDCOVER, NO_MATCHING_LISTING, searchUrl);
        }

        BookPrice softcover;
        if (scListing != null && !scUnknown) {
            softcover = saveListing(book, BookCoverType.SOFTCOVER, scListing);
        } else if (scUnknown) {
            deleteCover(book, BookCoverType.SOFTCOVER);
            softcover = unknown;
        } else {
            softcover = saveError(book, BookCoverType.SOFTCOVER, NO_MATCHING_LISTING, searchUrl);
        }

        AbeBooksListing lbListing = found.getLibraryBinding();
        BookPrice libraryBinding;
        if (lbListing != null) {
            libraryBinding = saveListing(book, BookCoverType.LIBRARY_BINDING, lbListing);
        } else {
            deleteCover(book, BookCoverType.LIBRARY_BINDING);
            libraryBinding = null;
        }

        AbeBooksListing otherListing = found.getOther();
        BookPrice other;
        if (otherListing != null) {
            other = saveListing(book, BookCoverType.OTHER, otherListing);
        } else {
            deleteCover(book, BookCoverType.OTHER);
            other = null;
        }
        return new CoverSaveResult(hardcover, softcover, libraryBinding, other);
    }

    private static boolean isUnknownBinding(AbeBooksListing listing) {
        return listing != null
                && (listing.getBinding() == null || listing.getBinding() == BookCoverType.UNKNOWN);
    }

    private void deleteCover(Book book, BookCoverType cover) {
        bookPriceRepository.findByBook_IdAndCover(book.getId(), cover)
                .ifPresent(bookPriceRepository::delete);
    }

    private static final class CoverSaveResult {
        private final BookPrice hardcover;
        private final BookPrice softcover;
        private final BookPrice libraryBinding;
        private final BookPrice other;

        private CoverSaveResult(BookPrice hardcover, BookPrice softcover,
                               BookPrice libraryBinding, BookPrice other) {
            this.hardcover = hardcover;
            this.softcover = softcover;
            this.libraryBinding = libraryBinding;
            this.other = other;
        }
    }

    private static final class PreparedLookup {
        private final String title;
        private final String authorName;
        private final BookPriceLookupResultDto completed;

        private PreparedLookup(String title, String authorName, BookPriceLookupResultDto completed) {
            this.title = title;
            this.authorName = authorName;
            this.completed = completed;
        }
    }

    private BookPrice saveListing(Book book, BookCoverType cover, AbeBooksListing listing) {
        BookPrice row = bookPriceRepository.findByBook_IdAndCover(book.getId(), cover)
                .orElseGet(BookPrice::new);
        row.setBook(book);
        row.setCover(cover);
        row.setPriceDollars(listing.getPriceDollars());
        row.setShippingDollars(listing.getShippingDollars());
        row.setCondition(listing.getCondition());
        row.setDetailsUrl(listing.getDetailsUrl());
        row.setLookupError(null);
        row.setLookedUpAt(LocalDateTime.now(ZoneOffset.UTC));
        return bookPriceRepository.save(row);
    }

    private BookPrice saveError(Book book, BookCoverType cover, String error, String searchUrl) {
        BookPrice row = bookPriceRepository.findByBook_IdAndCover(book.getId(), cover)
                .orElseGet(BookPrice::new);
        row.setBook(book);
        row.setCover(cover);
        row.setPriceDollars(null);
        row.setShippingDollars(null);
        row.setCondition(null);
        row.setDetailsUrl(searchUrl);
        row.setLookupError(error);
        row.setLookedUpAt(LocalDateTime.now(ZoneOffset.UTC));
        return bookPriceRepository.save(row);
    }

    BookPriceDto toDto(BookPrice price) {
        if (price == null) {
            return null;
        }
        Book book = price.getBook();
        String author = book != null && book.getAuthor() != null ? book.getAuthor().getName() : null;
        BigDecimal total = null;
        if (price.getPriceDollars() != null) {
            BigDecimal shipping = price.getShippingDollars() != null
                    ? price.getShippingDollars()
                    : BigDecimal.ZERO;
            total = price.getPriceDollars().add(shipping);
        }
        return BookPriceDto.builder()
                .id(price.getId())
                .bookId(book != null ? book.getId() : null)
                .bookTitle(book != null ? book.getTitle() : null)
                .author(author)
                .cover(price.getCover())
                .priceDollars(price.getPriceDollars())
                .shippingDollars(price.getShippingDollars())
                .totalDollars(total)
                .condition(price.getCondition())
                .lookedUpAt(price.getLookedUpAt())
                .detailsUrl(price.getDetailsUrl())
                .lookupError(price.getLookupError())
                .lastModified(price.getLastModified())
                .build();
    }

    private static boolean hasListing(BookPrice price) {
        return price != null && price.getPriceDollars() != null && price.getLookupError() == null;
    }

    private static String joinErrors(BookPrice hardcover, BookPrice softcover) {
        String hard = hardcover != null ? hardcover.getLookupError() : null;
        String soft = softcover != null ? softcover.getLookupError() : null;
        if (hard != null && hard.equals(soft)) {
            return hard;
        }
        StringBuilder sb = new StringBuilder();
        if (hard != null) {
            sb.append("Hardcover: ").append(hard);
        }
        if (soft != null) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append("Softcover: ").append(soft);
        }
        return sb.length() == 0 ? NO_MATCHING_LISTING : sb.toString();
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private <T> T inTransaction(Supplier<T> work) {
        if (transactionTemplate == null) {
            return work.get();
        }
        return transactionTemplate.execute(status -> work.get());
    }

    private void logPool(String phase, Long bookId) {
        HikariPoolMXBean pool = hikariPool();
        if (pool == null) {
            return;
        }
        log.info("AbeBooks {} bookId={} hikari active={} idle={} awaiting={} total={}",
                phase, bookId,
                pool.getActiveConnections(),
                pool.getIdleConnections(),
                pool.getThreadsAwaitingConnection(),
                pool.getTotalConnections());
    }

    private HikariPoolMXBean hikariPool() {
        if (dataSource == null) {
            return null;
        }
        try {
            HikariDataSource hikari = dataSource instanceof HikariDataSource hikariDataSource
                    ? hikariDataSource
                    : dataSource.unwrap(HikariDataSource.class);
            return hikari.getHikariPoolMXBean();
        } catch (Exception ex) {
            log.debug("Could not read Hikari pool stats: {}", ex.getMessage());
            return null;
        }
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}
