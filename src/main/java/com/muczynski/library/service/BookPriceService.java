/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.AbeSeller;
import com.muczynski.library.domain.AdditionalShippingSource;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookListing;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.dto.BookPriceDto;
import com.muczynski.library.dto.BookPriceLookupResultDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.exception.AbeBooksHttpException;
import com.muczynski.library.exception.AbeBooksRateLimitedException;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AbeSellerRepository;
import com.muczynski.library.repository.BookListingRepository;
import com.muczynski.library.repository.BookPriceRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.service.BooksFromFeedService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookPriceService {

    /** Cap so retries=20 cannot wait days via unbounded exponential backoff. */
    static final long MAX_RATE_LIMIT_BACKOFF_MS = 300_000L;

    /** Stored when AbeBooks returned a real SearchResults page with no usable listing. */
    public static final String NO_MATCHING_LISTING = "No matching listing";

    /** Offers kept per book for the savings page. The cheapest per cover still live on book_price. */
    static final int MAX_LISTINGS_PER_BOOK = 24;

    private static final int SHIP_RATES_STALE_DAYS = 30;

    private final BookRepository bookRepository;
    private final BookPriceRepository bookPriceRepository;
    private final AbeBooksClient abeBooksClient;
    private final AbeSellerRepository sellerRepository;
    private final BookListingRepository listingRepository;
    private final TransactionTemplate transactionTemplate;
    private final DataSource dataSource;
    private final int rateLimitRetries;
    private final long rateLimitBackoffMs;

    public BookPriceService(BookRepository bookRepository,
                            BookPriceRepository bookPriceRepository,
                            AbeBooksClient abeBooksClient) {
        this(bookRepository, bookPriceRepository, abeBooksClient, 0, 0, null, null, null, null);
    }

    public BookPriceService(BookRepository bookRepository,
                            BookPriceRepository bookPriceRepository,
                            AbeBooksClient abeBooksClient,
                            int rateLimitRetries,
                            long rateLimitBackoffMs) {
        this(bookRepository, bookPriceRepository, abeBooksClient, rateLimitRetries, rateLimitBackoffMs,
                null, null, null, null);
    }

    @Autowired
    public BookPriceService(BookRepository bookRepository,
                            BookPriceRepository bookPriceRepository,
                            AbeBooksClient abeBooksClient,
                            @Value("${abebooks.rate-limit-retries:2}") int rateLimitRetries,
                            @Value("${abebooks.rate-limit-backoff-ms:4000}") long rateLimitBackoffMs,
                            PlatformTransactionManager transactionManager,
                            DataSource dataSource,
                            AbeSellerRepository sellerRepository,
                            BookListingRepository listingRepository) {
        this.bookRepository = bookRepository;
        this.bookPriceRepository = bookPriceRepository;
        this.abeBooksClient = abeBooksClient;
        this.sellerRepository = sellerRepository;
        this.listingRepository = listingRepository;
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

        log.info("AbeBooks lookup starting bookId={} title='{}' author='{}' titles={}",
                bookId, prepared.primaryTitle, prepared.authorName, prepared.titlesToSearch);
        logPool("before-http", bookId);
        long started = System.nanoTime();
        final AbeBooksCoverListings[] mergedHolder = {null};
        try {
            for (String searchTitle : prepared.titlesToSearch) {
                AbeBooksCoverListings found = findWithBackoff(searchTitle, prepared.authorName);
                if (mergedHolder[0] == null) {
                    mergedHolder[0] = found;
                } else {
                    mergedHolder[0] = mergeListings(mergedHolder[0], found);
                }
                if (found.isRateLimited()) {
                    break; // per spec: if primary rate-limits, skip alternate
                }
            }
            long elapsedMs = elapsedMs(started);
            log.info("AbeBooks lookup HTTP finished bookId={} elapsedMs={} merged listings", bookId, elapsedMs);
            logPool("after-http", bookId);
            AbeBooksCoverListings finalMerged = mergedHolder[0] != null ? mergedHolder[0] : AbeBooksCoverListings.builder().build();
            PersistedLookup persisted = inTransaction(() -> persistFound(bookId, finalMerged));
            refreshSellerRates(persisted.sellerIds());
            return persisted.dto();
        } catch (AbeBooksRateLimitedException ex) {
            long elapsedMs = elapsedMs(started);
            log.warn("AbeBooks lookup rate-limited bookId={} title='{}' elapsedMs={}",
                    bookId, prepared.primaryTitle, elapsedMs);
            logPool("rate-limited", bookId);
            return inTransaction(() -> saveErrorResult(
                    bookId, AbeBooksRateLimitedException.MESSAGE, true, ex.getSearchUrl()));
        } catch (AbeBooksHttpException ex) {
            long elapsedMs = elapsedMs(started);
            log.warn("AbeBooks lookup HTTP {} bookId={} title='{}' elapsedMs={}",
                    ex.getStatusCode(), bookId, prepared.primaryTitle, elapsedMs);
            logPool("http-error", bookId);
            return inTransaction(() -> saveErrorResult(
                    bookId, ex.getMessage(), false, ex.getSearchUrl()));
        } catch (HttpStatusCodeException ex) {
            long elapsedMs = elapsedMs(started);
            int code = ex.getStatusCode().value();
            log.warn("AbeBooks lookup HTTP {} bookId={} title='{}' elapsedMs={}",
                    code, bookId, prepared.primaryTitle, elapsedMs);
            logPool("http-error", bookId);
            return inTransaction(() -> saveErrorResult(
                    bookId, AbeBooksHttpException.messageFor(code), false, null));
        } catch (Exception ex) {
            long elapsedMs = elapsedMs(started);
            log.warn("AbeBooks lookup failed bookId={} title='{}' elapsedMs={}: {}",
                    bookId, prepared.primaryTitle, elapsedMs, ex.getMessage(), ex);
            logPool("failed", bookId);
            String message = ex.getMessage() == null ? "AbeBooks lookup failed" : ex.getMessage();
            return inTransaction(() -> saveErrorResult(bookId, truncate(message, 500), false, null));
        }
    }

    /**
     * Merge two AbeBooksCoverListings by keeping the cheaper good-or-better listing per cover type.
     * If primary is empty, use alternate. Persist one merged result.
     */
    private AbeBooksCoverListings mergeListings(AbeBooksCoverListings primary, AbeBooksCoverListings alternate) {
        AbeBooksCoverListings.AbeBooksCoverListingsBuilder builder = AbeBooksCoverListings.builder();
        builder.hardcover(cheaperOrFirst(primary.getHardcover(), alternate.getHardcover()));
        builder.softcover(cheaperOrFirst(primary.getSoftcover(), alternate.getSoftcover()));
        builder.libraryBinding(primary.getLibraryBinding() != null ? primary.getLibraryBinding() : alternate.getLibraryBinding());
        builder.other(primary.getOther() != null ? primary.getOther() : alternate.getOther());
        builder.searchUrl(primary.getSearchUrl() != null ? primary.getSearchUrl() : alternate.getSearchUrl());
        builder.rateLimited(primary.isRateLimited() || alternate.isRateLimited());
        List<AbeBooksListing> candidates = new ArrayList<>();
        if (primary.getCandidates() != null) {
            candidates.addAll(primary.getCandidates());
        }
        if (alternate.getCandidates() != null) {
            candidates.addAll(alternate.getCandidates());
        }
        builder.candidates(candidates);
        return builder.build();
    }

    private AbeBooksListing cheaperOrFirst(AbeBooksListing a, AbeBooksListing b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.getPriceDollars() == null) return b;
        if (b.getPriceDollars() == null) return a;
        return a.getPriceDollars().compareTo(b.getPriceDollars()) <= 0 ? a : b;
    }

    private PreparedLookup prepareLookup(Long bookId) {
        Book book = requireBook(bookId);
        List<String> titles = Book.titlesToSearch(book);
        if (titles.isEmpty()) {
            return new PreparedLookup(null, null,
                    saveErrorResult(book, "Book has no title", false, null));
        }
        String primaryTitle = book.getTitle();
        if (BooksFromFeedService.isTemporaryTitle(primaryTitle)) {
            return new PreparedLookup(null, null,
                    saveErrorResult(book, "Not Ready - Temporary title", false, null));
        }
        if (isExcerptTitle(primaryTitle)) {
            return new PreparedLookup(null, null, createExcerptResult(book));
        }
        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : null;
        return new PreparedLookup(primaryTitle, titles, authorName, null);
    }

    private PersistedLookup persistFound(Long bookId, AbeBooksCoverListings found) {
        Book book = requireBook(bookId);
        List<Long> sellerIds = storeCandidates(book, found);
        CoverSaveResult saved = saveFoundCovers(book, found, found.getSearchUrl());
        boolean success = hasListing(saved.hardcover)
                || hasListing(saved.softcover)
                || hasListing(saved.libraryBinding)
                || hasListing(saved.other);
        String error = success ? null : joinErrors(saved.hardcover, saved.softcover);
        BookPriceLookupResultDto dto = BookPriceLookupResultDto.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .success(success)
                .hardcover(toDto(saved.hardcover))
                .softcover(toDto(saved.softcover))
                .libraryBinding(toDto(saved.libraryBinding))
                .errorMessage(error)
                .build();
        return new PersistedLookup(dto, sellerIds);
    }

    private record PersistedLookup(BookPriceLookupResultDto dto, List<Long> sellerIds) {}

    /**
     * Stores up to {@link #MAX_LISTINGS_PER_BOOK} offers and returns seller ids whose
     * shipping terms should be fetched. Null repositories (unit tests) skip this.
     */
    private List<Long> storeCandidates(Book book, AbeBooksCoverListings found) {
        if (sellerRepository == null || listingRepository == null || book.getId() == null) {
            return List.of();
        }
        List<AbeBooksListing> covers = new ArrayList<>();
        if (found.getHardcover() != null) {
            covers.add(found.getHardcover());
        }
        if (found.getSoftcover() != null) {
            covers.add(found.getSoftcover());
        }
        if (found.getLibraryBinding() != null) {
            covers.add(found.getLibraryBinding());
        }
        if (found.getOther() != null) {
            covers.add(found.getOther());
        }
        List<AbeBooksListing> pool = candidatePool(found, covers);
        Map<Long, AbeSeller> sellers = new LinkedHashMap<>();
        for (AbeBooksListing listing : pool) {
            if (listing.getSellerId() == null) {
                continue;
            }
            sellers.computeIfAbsent(listing.getSellerId(), id -> upsertSeller(listing));
        }
        List<AbeBooksListing> kept = capListings(pool, covers);
        List<Long> abeIds = kept.stream()
                .map(AbeBooksListing::getListingId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!abeIds.isEmpty()) {
            listingRepository.deleteByAbeListingIdIn(abeIds);
        }
        listingRepository.deleteByBook_Id(book.getId());
        listingRepository.flush();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (AbeBooksListing listing : kept) {
            if (listing.getListingId() == null || listing.getSellerId() == null) {
                continue;
            }
            AbeSeller seller = sellers.get(listing.getSellerId());
            if (seller == null) {
                continue;
            }
            BookListing row = new BookListing();
            row.setBook(book);
            row.setSeller(seller);
            row.setAbeListingId(listing.getListingId());
            row.setListingTitle(truncateOrNull(listing.getListingTitle(), 500));
            row.setDetailsUrl(truncateOrNull(listing.getDetailsUrl(), 2000));
            row.setPriceDollars(listing.getPriceDollars());
            row.setShippingDollars(listing.getShippingDollars());
            row.setPriorityShippingDollars(listing.getPriorityShippingDollars());
            row.setCondition(truncateOrNull(listing.getCondition(), 80));
            row.setBinding(listing.getBinding());
            row.setIsbn13(truncateOrNull(listing.getIsbn13(), 20));
            row.setQuantity(listing.getQuantity());
            row.setLookedUpAt(now);
            listingRepository.save(row);
        }
        List<Long> refresh = new ArrayList<>();
        for (AbeSeller seller : sellers.values()) {
            if (seller.getId() != null && seller.getSampleListingId() != null && ratesStale(seller)) {
                refresh.add(seller.getId());
            }
        }
        return refresh;
    }

    private static List<AbeBooksListing> candidatePool(AbeBooksCoverListings found, List<AbeBooksListing> covers) {
        List<AbeBooksListing> pool = new ArrayList<>();
        if (found.getCandidates() != null) {
            pool.addAll(found.getCandidates());
        }
        pool.addAll(covers);
        return pool;
    }

    /**
     * One row per AbeBooks listing id, cheapest total first, then the cover winners
     * even when they sit outside the cap.
     */
    static List<AbeBooksListing> capListings(List<AbeBooksListing> pool, List<AbeBooksListing> covers) {
        Map<Long, AbeBooksListing> byId = new LinkedHashMap<>();
        for (AbeBooksListing listing : pool) {
            if (listing == null || listing.getListingId() == null || listing.getSellerId() == null) {
                continue;
            }
            AbeBooksListing existing = byId.get(listing.getListingId());
            if (existing == null || listing.totalDollars().compareTo(existing.totalDollars()) < 0) {
                byId.put(listing.getListingId(), listing);
            }
        }
        List<AbeBooksListing> sorted = new ArrayList<>(byId.values());
        sorted.sort(Comparator.comparing(AbeBooksListing::totalDollars)
                .thenComparing(AbeBooksListing::getListingId));
        List<AbeBooksListing> kept = new ArrayList<>();
        Set<Long> keptIds = new HashSet<>();
        for (AbeBooksListing listing : sorted) {
            if (kept.size() >= MAX_LISTINGS_PER_BOOK) {
                break;
            }
            kept.add(listing);
            keptIds.add(listing.getListingId());
        }
        if (covers != null) {
            for (AbeBooksListing cover : covers) {
                if (cover == null || cover.getListingId() == null || cover.getSellerId() == null) {
                    continue;
                }
                if (keptIds.contains(cover.getListingId())) {
                    continue;
                }
                AbeBooksListing chosen = byId.get(cover.getListingId());
                if (chosen == null) {
                    continue;
                }
                kept.add(chosen);
                keptIds.add(chosen.getListingId());
            }
        }
        return kept;
    }

    private AbeSeller upsertSeller(AbeBooksListing listing) {
        AbeSeller seller = sellerRepository.findByVendorId(listing.getSellerId()).orElseGet(AbeSeller::new);
        seller.setVendorId(listing.getSellerId());
        if (listing.getSellerName() != null && !listing.getSellerName().isBlank()) {
            seller.setName(truncate(listing.getSellerName(), 200));
        }
        if (listing.getSellerCity() != null && !listing.getSellerCity().isBlank()) {
            seller.setCity(truncate(listing.getSellerCity(), 120));
        }
        if (listing.getSellerRegion() != null && !listing.getSellerRegion().isBlank()) {
            seller.setRegion(truncate(listing.getSellerRegion(), 80));
        }
        if (listing.getSellerCountry() != null && !listing.getSellerCountry().isBlank()) {
            seller.setCountry(truncate(listing.getSellerCountry(), 80));
        }
        if (listing.getSellerStars() != null) {
            seller.setStarRating(listing.getSellerStars());
        }
        if (listing.getSellerStorefrontUrl() != null && !listing.getSellerStorefrontUrl().isBlank()) {
            seller.setStorefrontUrl(truncate(listing.getSellerStorefrontUrl(), 500));
        }
        if (seller.getSampleListingId() == null && listing.getListingId() != null) {
            seller.setSampleListingId(listing.getListingId());
        }
        seller.setLookedUpAt(LocalDateTime.now(ZoneOffset.UTC));
        if (seller.getAdditionalShippingSource() == null) {
            seller.setAdditionalShippingSource(AdditionalShippingSource.UNKNOWN);
        }
        return sellerRepository.save(seller);
    }

    private static boolean ratesStale(AbeSeller seller) {
        if (seller.getRatesLookedUpAt() == null) {
            return true;
        }
        return seller.getRatesLookedUpAt().isBefore(
                LocalDateTime.now(ZoneOffset.UTC).minusDays(SHIP_RATES_STALE_DAYS));
    }

    /**
     * One ShipRates fetch per seller, outside the price-save transaction.
     * A rate limit stops further fetches and leaves the saved prices in place.
     */
    private void refreshSellerRates(List<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty() || sellerRepository == null || abeBooksClient == null) {
            return;
        }
        for (Long id : sellerIds) {
            AbeSeller seller = sellerRepository.findById(id).orElse(null);
            if (seller == null || seller.getVendorId() == null || seller.getSampleListingId() == null) {
                continue;
            }
            try {
                AbeBooksShipRates rates = abeBooksClient.fetchShipRates(
                        seller.getVendorId(), seller.getSampleListingId());
                inTransaction(() -> {
                    applyRates(id, rates);
                    return null;
                });
            } catch (AbeBooksRateLimitedException ex) {
                log.warn("AbeBooks ShipRates rate-limited sellerId={}: {}", id, ex.getMessage());
                break;
            } catch (RuntimeException ex) {
                log.warn("AbeBooks ShipRates failed sellerId={}: {}", id, ex.getMessage());
            }
        }
    }

    private void applyRates(Long sellerId, AbeBooksShipRates rates) {
        if (rates == null) {
            return;
        }
        AbeSeller seller = sellerRepository.findById(sellerId).orElse(null);
        if (seller == null) {
            return;
        }
        if (rates.getTerms() != null) {
            seller.setShippingTerms(rates.getTerms());
        }
        if (rates.getStandardDollars() != null) {
            seller.setSampleStandardShipping(rates.getStandardDollars());
        }
        if (rates.getPriorityDollars() != null) {
            seller.setSamplePriorityShipping(rates.getPriorityDollars());
        }
        seller.setRatesLookedUpAt(LocalDateTime.now(ZoneOffset.UTC));
        if (seller.getAdditionalShippingSource() != AdditionalShippingSource.LIBRARIAN) {
            seller.setAdditionalShippingDollars(rates.getAdditionalDollars());
            seller.setAdditionalShippingSource(rates.getAdditionalSource() != null
                    ? rates.getAdditionalSource()
                    : AdditionalShippingSource.UNKNOWN);
        }
        sellerRepository.save(seller);
    }

    private void deleteListings(Book book) {
        if (listingRepository == null || book == null || book.getId() == null) {
            return;
        }
        listingRepository.deleteByBook_Id(book.getId());
    }

    private BookPriceLookupResultDto saveErrorResult(Long bookId, String message, boolean rateLimited,
                                                    String searchUrl) {
        return saveErrorResult(requireBook(bookId), message, rateLimited, searchUrl);
    }

    private BookPriceLookupResultDto saveErrorResult(Book book, String message, boolean rateLimited,
                                                    String searchUrl) {
        deleteListings(book);
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

    private BookPriceLookupResultDto createExcerptResult(Book book) {
        deleteListings(book);
        // Delete all other covers (keep only softcover per spec)
        deleteCover(book, BookCoverType.HARDCOVER);
        deleteCover(book, BookCoverType.LIBRARY_BINDING);
        deleteCover(book, BookCoverType.OTHER);
        deleteCover(book, BookCoverType.UNKNOWN);

        BookPrice softcover = bookPriceRepository.findByBook_IdAndCover(book.getId(), BookCoverType.SOFTCOVER)
                .orElseGet(BookPrice::new);
        softcover.setBook(book);
        softcover.setCover(BookCoverType.SOFTCOVER);
        softcover.setPriceDollars(new BigDecimal("0.01"));
        softcover.setShippingDollars(BigDecimal.ZERO);
        softcover.setCondition(null);
        softcover.setDetailsUrl(null);
        softcover.setLookupError(null);
        softcover.setSeller(null);
        softcover.setLookedUpAt(LocalDateTime.now(ZoneOffset.UTC));
        bookPriceRepository.save(softcover);

        return BookPriceLookupResultDto.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .success(true)
                .softcover(toDto(softcover))
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
                AbeBooksCoverListings result = abeBooksClient.findCheapestGoodOrBetter(title, author);
                // propagate rate limit flag from client if added later
                return result;
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

    /**
     * Case-insensitive check for titles starting with "Excerpt from " or "Excerpts from "
     * (with space after "from"). Matches the temporary-title early-exit pattern.
     */
    private static boolean isExcerptTitle(String title) {
        if (title == null) {
            return false;
        }
        String trimmed = title.trim().toLowerCase();
        return trimmed.startsWith("excerpt from ") || trimmed.startsWith("excerpts from ");
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
        private final String primaryTitle;
        private final List<String> titlesToSearch;
        private final String authorName;
        private final BookPriceLookupResultDto completed;

        private PreparedLookup(String primaryTitle, List<String> titlesToSearch, String authorName,
                               BookPriceLookupResultDto completed) {
            this.primaryTitle = primaryTitle;
            this.titlesToSearch = titlesToSearch;
            this.authorName = authorName;
            this.completed = completed;
        }

        private PreparedLookup(String title, String authorName, BookPriceLookupResultDto completed) {
            this.primaryTitle = title;
            this.titlesToSearch = title != null ? List.of(title) : List.of();
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
        if (sellerRepository != null && listing.getSellerId() != null) {
            sellerRepository.findByVendorId(listing.getSellerId()).ifPresent(row::setSeller);
        } else {
            row.setSeller(null);
        }
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
        row.setSeller(null);
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
                .sellerId(price.getSeller() != null ? price.getSeller().getId() : null)
                .sellerName(price.getSeller() != null ? price.getSeller().getName() : null)
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

    private static String truncateOrNull(String value, int max) {
        if (value == null) {
            return null;
        }
        return truncate(value, max);
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
