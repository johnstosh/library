/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.dto.BookPriceDto;
import com.muczynski.library.dto.BookPriceLookupResultDto;
import com.muczynski.library.exception.AbeBooksRateLimitedException;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.BookPriceRepository;
import com.muczynski.library.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class BookPriceService {

    private final BookRepository bookRepository;
    private final BookPriceRepository bookPriceRepository;
    private final AbeBooksClient abeBooksClient;
    private final int rateLimitRetries;
    private final long rateLimitBackoffMs;

    public BookPriceService(BookRepository bookRepository,
                            BookPriceRepository bookPriceRepository,
                            AbeBooksClient abeBooksClient) {
        this(bookRepository, bookPriceRepository, abeBooksClient, 0, 0);
    }

    @Autowired
    public BookPriceService(BookRepository bookRepository,
                            BookPriceRepository bookPriceRepository,
                            AbeBooksClient abeBooksClient,
                            @Value("${abebooks.rate-limit-retries:2}") int rateLimitRetries,
                            @Value("${abebooks.rate-limit-backoff-ms:2000}") long rateLimitBackoffMs) {
        this.bookRepository = bookRepository;
        this.bookPriceRepository = bookPriceRepository;
        this.abeBooksClient = abeBooksClient;
        this.rateLimitRetries = rateLimitRetries;
        this.rateLimitBackoffMs = rateLimitBackoffMs;
    }

    @Transactional(readOnly = true)
    public List<BookPriceDto> listAll() {
        return bookPriceRepository.findAllWithBookAndAuthor().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Looks up hardcover and softcover AbeBooks prices for a book and upserts
     * one {@link BookPrice} row per cover.
     */
    public BookPriceLookupResultDto lookupAndUpdateBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new LibraryException("Book not found: " + bookId));

        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            BookPrice hardcover = saveError(book, BookCoverType.HARDCOVER, "Not Ready - Temporary title");
            BookPrice softcover = saveError(book, BookCoverType.SOFTCOVER, "Not Ready - Temporary title");
            return BookPriceLookupResultDto.builder()
                    .bookId(book.getId())
                    .bookTitle(book.getTitle())
                    .success(false)
                    .hardcover(toDto(hardcover))
                    .softcover(toDto(softcover))
                    .errorMessage("Not Ready - Temporary title")
                    .build();
        }

        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : null;
        BookPrice hardcover;
        BookPrice softcover;
        try {
            AbeBooksCoverListings found = findWithBackoff(book.getTitle(), authorName);
            hardcover = found.getHardcover() != null
                    ? saveListing(book, BookCoverType.HARDCOVER, found.getHardcover())
                    : saveError(book, BookCoverType.HARDCOVER, "No matching listing");
            softcover = found.getSoftcover() != null
                    ? saveListing(book, BookCoverType.SOFTCOVER, found.getSoftcover())
                    : saveError(book, BookCoverType.SOFTCOVER, "No matching listing");
        } catch (AbeBooksRateLimitedException ex) {
            log.warn("AbeBooks rate limited for book {}", book.getId());
            hardcover = saveError(book, BookCoverType.HARDCOVER, AbeBooksRateLimitedException.MESSAGE);
            softcover = saveError(book, BookCoverType.SOFTCOVER, AbeBooksRateLimitedException.MESSAGE);
            return BookPriceLookupResultDto.builder()
                    .bookId(book.getId())
                    .bookTitle(book.getTitle())
                    .success(false)
                    .rateLimited(true)
                    .hardcover(toDto(hardcover))
                    .softcover(toDto(softcover))
                    .errorMessage(AbeBooksRateLimitedException.MESSAGE)
                    .build();
        } catch (Exception ex) {
            log.warn("AbeBooks lookup failed for book {}", book.getId(), ex);
            String message = ex.getMessage() == null ? "AbeBooks lookup failed" : ex.getMessage();
            String truncated = truncate(message, 500);
            hardcover = saveError(book, BookCoverType.HARDCOVER, truncated);
            softcover = saveError(book, BookCoverType.SOFTCOVER, truncated);
        }
        boolean success = hasListing(hardcover) || hasListing(softcover);
        String error = success ? null : joinErrors(hardcover, softcover);
        return BookPriceLookupResultDto.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .success(success)
                .hardcover(toDto(hardcover))
                .softcover(toDto(softcover))
                .errorMessage(error)
                .build();
    }

    private AbeBooksCoverListings findWithBackoff(String title, String author) {
        AbeBooksRateLimitedException last = null;
        int attempts = Math.max(1, rateLimitRetries + 1);
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (attempt > 0) {
                long waitMs = rateLimitBackoffMs <= 0 ? 0 : rateLimitBackoffMs * (1L << (attempt - 1));
                log.info("AbeBooks backoff {} ms before retry {} for title {}", waitMs, attempt, title);
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

    private BookPrice saveError(Book book, BookCoverType cover, String error) {
        BookPrice row = bookPriceRepository.findByBook_IdAndCover(book.getId(), cover)
                .orElseGet(BookPrice::new);
        row.setBook(book);
        row.setCover(cover);
        row.setPriceDollars(null);
        row.setShippingDollars(null);
        row.setCondition(null);
        row.setDetailsUrl(null);
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
        return sb.length() == 0 ? "No matching listing" : sb.toString();
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
