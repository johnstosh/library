/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.muczynski.library.domain.AuthorNames;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.MsuLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.util.LookupErrorMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

/**
 * Looks up book availability (audio, paper, ebook) at Michigan State University Libraries
 * and stores the result on the Book entity.
 *
 * <p>The public catalog is VuFind 11 at {@code catalog.lib.msu.edu} (FOLIO), not Primo.
 * Search uses the keyless read-only API {@code GET /api/v1/search}. A hit in that primary
 * index is an MSU catalog record. Format labels are the display values from MSU's
 * {@code format_map.properties}: {@code Book} is paper unless the record is also
 * {@code eBook} or {@code Electronic} (those are ebooks, matching MSU's own material-type
 * rules). {@code Audio}, {@code CD}, {@code Cassette}, {@code Vinyl Record}, and
 * {@code Shellac Record} are audio.
 *
 * <p>The Books-page carousel looks up titles back-to-back, and each title can
 * search more than once. VuFind answers that burst with HTTP 429. A 429 used
 * to be stored on the first response, so the next title went out immediately
 * and the catalog stayed limited. HTTP 403/429/502/503/504 and I/O timeouts
 * are retried. The wait is exponential from {@code msu.lookup.backoff-ms} and
 * at least the {@code Retry-After} value, capped at {@link #MAX_BACKOFF_MS}.
 * A pause ({@code msu.lookup.request-delay-ms}) is inserted between searches.
 */
@Service
@Slf4j
@Transactional
public class MsuLookupService {

    static final String SEARCH_URL = "https://catalog.lib.msu.edu/api/v1/search";
    /** One retry waits at most this long, even when Retry-After is longer. */
    static final long MAX_BACKOFF_MS = 60_000L;
    private static final String NOT_HELD = "Not held by Michigan State University";

    private static final LongConsumer THREAD_SLEEP = millis -> {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new com.muczynski.library.exception.LibraryException("MSU lookup interrupted", ex);
        }
    };

    /**
     * Display labels from MSU Libraries catalog {@code format_map.properties}
     * ({@code MusicRecording} and {@code SoundRecording} both display as Audio).
     */
    private static final Set<String> AUDIO_FORMATS = Set.of(
            "audio", "cd", "cassette", "vinyl record", "shellac record");

    private static final Pattern TRAILING_FORMAT_SUFFIX_PATTERN = Pattern.compile(
            "(?i)(\\s*\\((?:dvd|cd|vhs|blu-?ray|book on cd|large print|audiobook)\\)"
                    + "|,\\s*(?:audio\\s*cd|book\\s*on\\s*cd|large\\s*print|dvd|cd|vhs))\\s*$");

    private final BookRepository bookRepository;
    private final RestTemplate msuRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int retries;
    private final long backoffMs;
    private final long requestDelayMs;
    private final LongConsumer pauser;
    private final Object paceLock = new Object();
    /** Earliest time the next outbound search may start. Shared by every lookup on this instance. */
    private long nextAllowedAtMs;

    @Autowired
    public MsuLookupService(BookRepository bookRepository,
                             @Qualifier("msuRestTemplate") RestTemplate msuRestTemplate,
                             @Value("${msu.lookup.retries:3}") int retries,
                             @Value("${msu.lookup.backoff-ms:2000}") long backoffMs,
                             @Value("${msu.lookup.request-delay-ms:1000}") long requestDelayMs) {
        this(bookRepository, msuRestTemplate, retries, backoffMs, requestDelayMs, THREAD_SLEEP);
    }

    MsuLookupService(BookRepository bookRepository,
                     RestTemplate msuRestTemplate,
                     int retries,
                     long backoffMs,
                     long requestDelayMs,
                     LongConsumer pauser) {
        this.bookRepository = bookRepository;
        this.msuRestTemplate = msuRestTemplate;
        this.retries = Math.max(0, retries);
        this.backoffMs = Math.max(0, backoffMs);
        this.requestDelayMs = Math.max(0, requestDelayMs);
        this.pauser = pauser == null ? THREAD_SLEEP : pauser;
    }

    /**
     * Lookup and update Michigan State University availability for a single book.
     */
    public MsuLookupResultDto lookupAndUpdateBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new com.muczynski.library.exception.LibraryException("Book not found: " + bookId));
        return performMsuLookup(book);
    }

    private MsuLookupResultDto performMsuLookup(Book book) {
        List<String> titles = Book.titlesToSearch(book);
        if (titles.isEmpty()) {
            return MsuLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage("Book has no title")
                    .build();
        }
        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            log.info("Skipping MSU lookup for temporary title: {}", book.getTitle());
            book.setMsuLastChecked(LocalDateTime.now());
            book.setMsuLookupError("Not Ready - Temporary title");
            bookRepository.save(book);
            return MsuLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage("Not Ready - Temporary title")
                    .build();
        }

        List<String> authorLastNames = AuthorNames.lookupLastNames(book.getAuthor());

        try {
            ArrayNode matches = objectMapper.createArrayNode();
            String matchedTitle = null;
            for (String searchTitle : titles) {
                String cleaned = cleanTitle(searchTitle);
                for (String candidateTitle : buildTitleCandidates(cleaned)) {
                    JsonNode records = search(candidateTitle);
                    MatchBatch filtered = filterMatches(records, candidateTitle, authorLastNames);
                    if (!filtered.matches.isEmpty()) {
                        matches = filtered.matches;
                        matchedTitle = filtered.matchedTitle;
                        break;
                    }
                }
                if (!matches.isEmpty()) {
                    break;
                }
            }

            if (matches.isEmpty()) {
                book.setMsuAudioAvailable(false);
                book.setMsuPaperAvailable(false);
                book.setMsuEbookAvailable(false);
                book.setMsuLastChecked(LocalDateTime.now());
                book.setMsuLookupError(NOT_HELD);
                bookRepository.save(book);
                return MsuLookupResultDto.builder()
                        .bookId(book.getId())
                        .success(false)
                        .audioAvailable(false)
                        .paperAvailable(false)
                        .ebookAvailable(false)
                        .errorMessage(NOT_HELD)
                        .build();
            }

            boolean audio = false;
            boolean paper = false;
            boolean ebook = false;
            for (JsonNode record : matches) {
                Holding holding = classify(record.path("formats"));
                audio = audio || holding.audio;
                paper = paper || holding.paper;
                ebook = ebook || holding.ebook;
            }

            book.setMsuAudioAvailable(audio);
            book.setMsuPaperAvailable(paper);
            book.setMsuEbookAvailable(ebook);
            book.setMsuLastChecked(LocalDateTime.now());
            book.setMsuLookupError(null);
            bookRepository.save(book);

            log.info("MSU lookup for book {} ('{}'): audio={}, paper={}, ebook={}",
                    book.getId(), book.getTitle(), audio, paper, ebook);

            return MsuLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(true)
                    .audioAvailable(audio)
                    .paperAvailable(paper)
                    .ebookAvailable(ebook)
                    .matchedTitle(matchedTitle)
                    .build();
        } catch (Exception e) {
            log.error("Error during MSU lookup for book {}: {}", book.getId(), e.getMessage());
            String lookupError = LookupErrorMessages.fromException(e);
            book.setMsuLastChecked(LocalDateTime.now());
            book.setMsuLookupError(lookupError);
            bookRepository.save(book);
            return MsuLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage(lookupError)
                    .build();
        }
    }

    private List<String> buildTitleCandidates(String cleanedTitle) {
        List<String> candidates = new ArrayList<>();
        candidates.add(cleanedTitle);
        int colonIndex = cleanedTitle.indexOf(':');
        if (colonIndex > 0) {
            String truncated = cleanedTitle.substring(0, colonIndex).trim();
            if (!truncated.isEmpty() && !truncated.equalsIgnoreCase(cleanedTitle)) {
                candidates.add(truncated);
            }
        }
        return candidates;
    }

    private String cleanTitle(String title) {
        if (title == null) {
            return "";
        }
        String cleaned = title.trim();
        String previous;
        do {
            previous = cleaned;
            cleaned = Book.stripCopySuffix(cleaned);
            cleaned = TRAILING_FORMAT_SUFFIX_PATTERN.matcher(cleaned).replaceAll("").trim();
        } while (!cleaned.equals(previous) && !cleaned.isEmpty());
        return cleaned.isEmpty() ? title.trim() : cleaned;
    }

    /**
     * Title search against the VuFind primary index. The URI is built once so RestTemplate
     * does not treat the URL as a template and encode it a second time.
     */
    private JsonNode search(String title) {
        URI uri = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("lookfor", title)
                .queryParam("type", "Title")
                .queryParam("limit", 20)
                .queryParam("field[]", "title", "shortTitle", "formats", "authors", "id")
                .build()
                .toUri();
        String response = getWithRetry(uri);
        if (response == null || !response.trim().startsWith("{")) {
            throw new com.muczynski.library.exception.LibraryException(
                    "MSU catalog returned a non-JSON response");
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            if ("ERROR".equalsIgnoreCase(root.path("status").asText(""))) {
                String message = root.path("statusMessage").asText("MSU catalog search failed");
                throw new com.muczynski.library.exception.LibraryException(message);
            }
            return root.path("records");
        } catch (com.muczynski.library.exception.LibraryException e) {
            throw e;
        } catch (Exception e) {
            throw new com.muczynski.library.exception.LibraryException("Failed to parse MSU response", e);
        }
    }

    /**
     * GET with a pause between searches and backoff on rate-limit and gateway
     * statuses. The wait is applied by {@link #pace()} on the next attempt, and
     * is also reserved for the next book so a stored 429 does not immediately
     * send another title.
     */
    String getWithRetry(URI uri) {
        int attempts = retries + 1;
        RuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            pace();
            try {
                return msuRestTemplate.getForObject(uri, String.class);
            } catch (RestClientResponseException e) {
                last = e;
                if (!isRetryableStatus(e.getStatusCode().value()) || attempt == attempts) {
                    if (isRetryableStatus(e.getStatusCode().value())) {
                        delayNextRequest(backoffMs(attempt, backoffMs, e));
                    }
                    throw e;
                }
                long wait = backoffMs(attempt, backoffMs, e);
                log.warn("MSU HTTP {} for {} (attempt {}/{}); backing off {} ms",
                        e.getStatusCode().value(), uri, attempt, attempts, wait);
                delayNextRequest(wait);
            } catch (ResourceAccessException e) {
                last = e;
                if (attempt == attempts) {
                    delayNextRequest(backoffMs(attempt, backoffMs, null));
                    throw e;
                }
                long wait = backoffMs(attempt, backoffMs, null);
                log.warn("MSU I/O for {} (attempt {}/{}): {}; backing off {} ms",
                        uri, attempt, attempts, e.getMessage(), wait);
                delayNextRequest(wait);
            }
        }
        throw last != null ? last
                : new com.muczynski.library.exception.LibraryException("MSU lookup failed");
    }

    static boolean isRetryableStatus(int statusCode) {
        return statusCode == 403 || statusCode == 429
                || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    /**
     * Wait before another try. Exponential from {@code baseMs} on the first
     * failure, doubled each further failure, and never shorter than Retry-After.
     * A zero base and a missing Retry-After wait 0 — that is the bug that stored
     * {@code Error: HTTP 429 Too Many Requests} and sent the next title at once.
     */
    static long backoffMs(int failedAttempt, long baseMs, RestClientResponseException exception) {
        long exponential = 0;
        if (baseMs > 0 && failedAttempt > 0) {
            exponential = baseMs;
            for (int i = 1; i < failedAttempt && exponential < MAX_BACKOFF_MS; i++) {
                exponential = Math.min(MAX_BACKOFF_MS, exponential * 2);
            }
        }
        return Math.max(exponential, retryAfterMs(exception));
    }

    static long retryAfterMs(RestClientResponseException exception) {
        if (exception == null || exception.getResponseHeaders() == null) {
            return 0;
        }
        String raw = exception.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        raw = raw.trim();
        try {
            long seconds = Long.parseLong(raw);
            if (seconds <= 0) {
                return 0;
            }
            return Math.min(seconds * 1000L, MAX_BACKOFF_MS);
        } catch (NumberFormatException ignored) {
            try {
                ZonedDateTime when = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME);
                long ms = Duration.between(Instant.now(), when.toInstant()).toMillis();
                if (ms <= 0) {
                    return 0;
                }
                return Math.min(ms, MAX_BACKOFF_MS);
            } catch (DateTimeParseException ex) {
                return 0;
            }
        }
    }

    private void pace() {
        long wait;
        synchronized (paceLock) {
            long now = System.currentTimeMillis();
            wait = Math.max(0, nextAllowedAtMs - now);
            long start = now + wait;
            nextAllowedAtMs = start + requestDelayMs;
        }
        sleep(wait);
    }

    private void delayNextRequest(long waitMs) {
        if (waitMs <= 0) {
            return;
        }
        synchronized (paceLock) {
            long until = System.currentTimeMillis() + waitMs;
            if (until > nextAllowedAtMs) {
                nextAllowedAtMs = until;
            }
        }
    }

    private void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        pauser.accept(ms);
    }

    private MatchBatch filterMatches(JsonNode records, String title, List<String> authorLastNames) {
        ArrayNode matches = objectMapper.createArrayNode();
        String matchedTitle = null;
        String normalizedTitle = normalize(title);
        int titleWordCount = normalizedTitle.isEmpty() ? 0 : normalizedTitle.split(" ").length;

        if (records != null && records.isArray()) {
            for (JsonNode record : records) {
                String display = matchingDisplayTitle(record, normalizedTitle);
                if (display == null) {
                    continue;
                }
                if (titleWordCount <= 4 && !authorMatches(record, authorLastNames)) {
                    continue;
                }
                matches.add(record);
                if (matchedTitle == null) {
                    matchedTitle = display;
                }
            }
        }
        return new MatchBatch(matches, matchedTitle);
    }

    /**
     * Exact normalized match against the record's short title, the full title, the title
     * before a statement of responsibility ({@code / }), or the title before a subtitle colon.
     * VuFind {@code title} is often the full 245 statement, which will not equal our catalog title.
     */
    private String matchingDisplayTitle(JsonNode record, String normalizedTitle) {
        if (normalizedTitle.isEmpty()) {
            return null;
        }
        String shortTitle = record.path("shortTitle").asText("");
        if (normalizedTitle.equals(normalize(shortTitle))) {
            return shortTitle;
        }
        String full = record.path("title").asText("");
        if (normalizedTitle.equals(normalize(full))) {
            return full;
        }
        String beforeSlash = beforeSeparator(full, " / ");
        if (!beforeSlash.equals(full) && normalizedTitle.equals(normalize(beforeSlash))) {
            return beforeSlash;
        }
        String beforeColon = beforeSeparator(full, ":");
        if (!beforeColon.equals(full) && normalizedTitle.equals(normalize(beforeColon))) {
            return beforeColon.trim();
        }
        return null;
    }

    private String beforeSeparator(String value, String separator) {
        int index = value.indexOf(separator);
        if (index <= 0) {
            return value;
        }
        return value.substring(0, index).trim();
    }

    private boolean authorMatches(JsonNode record, List<String> authorLastNames) {
        List<String> texts = new ArrayList<>();
        collectAuthorText(record.path("authors"), texts);
        for (String lastName : authorLastNames) {
            if (lastName == null) {
                continue;
            }
            String normalizedLastName = normalize(lastName);
            if (normalizedLastName.isEmpty()) {
                continue;
            }
            for (String text : texts) {
                for (String word : normalize(text).split(" ")) {
                    if (word.equals(normalizedLastName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * VuFind author maps use the person's name as the object key
     * ({@code {"primary": {"Tolkien, J. R. R.": {"role": []}}}}). Empty lists serialize as arrays.
     */
    private void collectAuthorText(JsonNode node, List<String> out) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            out.add(node.asText());
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectAuthorText(child, out);
            }
            return;
        }
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(name -> {
                JsonNode child = node.get(name);
                if (child != null && (child.isObject() || child.isArray())) {
                    out.add(name);
                }
                collectAuthorText(child, out);
            });
        }
    }

    private Holding classify(JsonNode formats) {
        boolean audio = false;
        boolean book = false;
        boolean ebookLabel = false;
        boolean electronic = false;
        if (formats != null && formats.isArray()) {
            for (JsonNode format : formats) {
                String label = format.asText("").toLowerCase(Locale.ROOT).trim();
                if (AUDIO_FORMATS.contains(label)) {
                    audio = true;
                } else if ("ebook".equals(label)) {
                    ebookLabel = true;
                } else if ("book".equals(label)) {
                    book = true;
                } else if ("electronic".equals(label)) {
                    electronic = true;
                }
            }
        }
        boolean ebook = ebookLabel || (book && electronic);
        boolean paper = book && !electronic && !ebookLabel;
        return new Holding(audio, paper, ebook);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    private record MatchBatch(ArrayNode matches, String matchedTitle) {
    }

    private record Holding(boolean audio, boolean paper, boolean ebook) {
    }
}
