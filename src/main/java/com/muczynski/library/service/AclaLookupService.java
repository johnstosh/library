/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.muczynski.library.domain.AuthorNames;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.PublicLibrary;
import com.muczynski.library.dto.AclaLookupResultDto;
import com.muczynski.library.dto.CatalogMatchRef;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.service.BooksFromFeedService;
import com.muczynski.library.util.LookupErrorMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

/**
 * Looks up book availability (audio, paper, ebook) at ACLA (Allegheny County Library
 * Association) via the public BiblioCommons catalog search API used by
 * {@code acl.bibliocommons.com}, and stores the result on the Book entity.
 *
 * <p>BiblioCommons returns each format as a separate bib rather than grouping them
 * the way YDL's FormatGroup material tabs do. After an unfiltered title search,
 * missing format categories are filled in with format-filtered follow-up searches
 * so popular titles whose first page is all print still record ebook/audio holdings.
 *
 * <p>BiblioCommons (and its CDN) may 403 a burst of searches — typical of the
 * Books-page bulk carousel, which looks up many books back-to-back, each of which
 * can make several HTTP calls. HTTP 403/429/502/503/504 and I/O timeouts are
 * retried by {@link CatalogLookupRetry}: exponential backoff, at least
 * {@code Retry-After}, and that wait is reserved for the next title. A follow-up
 * format search that still fails is skipped so
 * holdings already found on the first page are kept rather than recording
 * {@code Error: HTTP 403 Forbidden} for the whole book.
 */
@Service
@Slf4j
@Transactional
public class AclaLookupService {

    private static final String SEARCH_URL = "https://gateway.bibliocommons.com/v2/libraries/acl/bibs/search";

    /**
     * Matches trailing physical-format labels our own titles often carry that ACLA's
     * catalog titles don't have, e.g. " (DVD)", ", Audio CD". Copy-number suffixes
     * (", c. 2") are stripped separately via {@link Book#stripCopySuffix(String)}.
     * Applied repeatedly so stacked suffixes (", c. 2, Audio CD") are all stripped
     * before the title is used to search or match against ACLA.
     */
    private static final Pattern TRAILING_FORMAT_SUFFIX_PATTERN = Pattern.compile(
            "(?i)(\\s*\\((?:dvd|cd|vhs|blu-?ray|book on cd|large print|audiobook)\\)"
                    + "|,\\s*(?:audio\\s*cd|book\\s*on\\s*cd|large\\s*print|dvd|cd|vhs))\\s*$");

    private static final String[] AUDIO_FORMAT_FILTERS = {"EAUDIOBOOK", "BOOK_CD", "MP3_CD"};
    private static final String[] EBOOK_FORMAT_FILTERS = {"EBOOK"};
    private static final String[] PAPER_FORMAT_FILTERS = {"BK", "LPRINT"};

    private final BookRepository bookRepository;
    private final RestTemplate aclaRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CatalogLookupRetry http;

    public AclaLookupService(BookRepository bookRepository,
                             @Qualifier("aclaRestTemplate") RestTemplate aclaRestTemplate) {
        this(bookRepository, aclaRestTemplate, 0, 0, 0);
    }

    @Autowired
    public AclaLookupService(BookRepository bookRepository,
                             @Qualifier("aclaRestTemplate") RestTemplate aclaRestTemplate,
                             @Value("${acla.lookup.retries:3}") int retries,
                             @Value("${acla.lookup.backoff-ms:2000}") long backoffMs,
                             @Value("${acla.lookup.request-delay-ms:1000}") long requestDelayMs) {
        this(bookRepository, aclaRestTemplate, retries, backoffMs, requestDelayMs, null);
    }

    AclaLookupService(BookRepository bookRepository,
                      RestTemplate aclaRestTemplate,
                      int retries,
                      long backoffMs,
                      long requestDelayMs,
                      LongConsumer pauser) {
        this.bookRepository = bookRepository;
        this.aclaRestTemplate = aclaRestTemplate;
        this.http = new CatalogLookupRetry("ACLA", retries, backoffMs, requestDelayMs, pauser);
    }

    /**
     * Lookup and update ACLA availability for a single book.
     */
    public AclaLookupResultDto lookupAndUpdateBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new com.muczynski.library.exception.LibraryException("Book not found: " + bookId));

        return performAclaLookup(book);
    }

    private AclaLookupResultDto performAclaLookup(Book book) {
        List<String> titles = Book.titlesToSearch(book);
        // Temporary-title skip on primary only
        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            log.info("Skipping ACLA lookup for temporary title: {}", book.getTitle());
            book.setAclaLastChecked(LocalDateTime.now());
            book.setAclaLookupError("Not Ready - Temporary title");
            bookRepository.save(book);
            return AclaLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage("Not Ready - Temporary title")
                    .build();
        }

        String primaryTitle = book.getTitle();

        try {
            JsonNode matches = objectMapper.createArrayNode();
            String matchedCandidate = primaryTitle != null ? cleanTitle(primaryTitle) : "";
            String matchedLastName = null;
            List<CatalogMatchRef> matchedRefs = new ArrayList<>();
            for (String searchTitle : titles) {
                String cleaned = cleanTitle(searchTitle);
                for (String candidateTitle : buildTitleCandidates(cleaned)) {
                    for (String authorLastName : AuthorNames.lookupLastNames(book.getAuthor())) {
                        JsonNode filtered = filterMatches(search(candidateTitle, null), candidateTitle, authorLastName);
                        ArrayNode kept = objectMapper.createArrayNode();
                        List<CatalogMatchRef> refs = CatalogMatchKeys.keep(
                                kept, filtered, book.getInvalidHoldings(), PublicLibrary.ACLA, CatalogMatchKeys::aclaKey);
                        if (!kept.isEmpty()) {
                            matches = kept;
                            matchedRefs = refs;
                            matchedCandidate = candidateTitle;
                            matchedLastName = authorLastName;
                            break;
                        }
                    }
                    if (!matches.isEmpty()) {
                        break;
                    }
                }
                if (!matches.isEmpty()) {
                    break;
                }
            }

            if (matches.isEmpty()) {
                // "Not held" only if both titles empty. On primary exception still try alternate; write merged flags once (do not clear mid-merge).
                book.setAclaAudioAvailable(false);
                book.setAclaPaperAvailable(false);
                book.setAclaEbookAvailable(false);
                book.setAclaLastChecked(LocalDateTime.now());
                book.setAclaLookupError("Not held by ACLA");
                bookRepository.save(book);
                return AclaLookupResultDto.builder()
                        .bookId(book.getId())
                        .success(false)
                        .audioAvailable(false)
                        .paperAvailable(false)
                        .ebookAvailable(false)
                        .errorMessage("Not held by ACLA")
                        .build();
            }

            boolean audio = false;
            boolean paper = false;
            boolean ebook = false;
            String matchedTitle = firstMatchedTitle(matches, matchedCandidate);

            FormatFlags flags = classifyMatches(matches);
            audio = flags.audio;
            paper = flags.paper;
            ebook = flags.ebook;

            // BiblioCommons paginates at 25 and does not group formats, so a popular
            // title's first page may be all print. Follow up per missing category.
            if (!audio) {
                JsonNode extra = searchMissingFormats(matchedCandidate, matchedLastName, AUDIO_FORMAT_FILTERS, book);
                audio = hasFormat(extra, "audio");
                matchedRefs.addAll(refsOf(extra));
            }
            if (!ebook) {
                JsonNode extra = searchMissingFormats(matchedCandidate, matchedLastName, EBOOK_FORMAT_FILTERS, book);
                ebook = hasFormat(extra, "ebook");
                matchedRefs.addAll(refsOf(extra));
            }
            if (!paper) {
                JsonNode extra = searchMissingFormats(matchedCandidate, matchedLastName, PAPER_FORMAT_FILTERS, book);
                paper = hasFormat(extra, "paper");
                matchedRefs.addAll(refsOf(extra));
            }

            book.setAclaAudioAvailable(audio);
            book.setAclaPaperAvailable(paper);
            book.setAclaEbookAvailable(ebook);
            book.setAclaLastChecked(LocalDateTime.now());
            book.setAclaLookupError(null);
            bookRepository.save(book);

            log.info("ACLA lookup for book {} ('{}'): audio={}, paper={}, ebook={}",
                    book.getId(), book.getTitle(), audio, paper, ebook);

            return AclaLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(true)
                    .audioAvailable(audio)
                    .paperAvailable(paper)
                    .ebookAvailable(ebook)
                    .matchedTitle(matchedTitle)
                    .matches(matchedRefs)
                    .build();

        } catch (Exception e) {
            log.error("Error during ACLA lookup for book {}: {}", book.getId(), e.getMessage());
            String lookupError = LookupErrorMessages.fromException(e);
            book.setAclaLastChecked(LocalDateTime.now());
            book.setAclaLookupError(lookupError);
            bookRepository.save(book);
            return AclaLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage(lookupError)
                    .build();
        }
    }

    /**
     * Builds the ordered list of title candidates to search for: the cleaned title, then, if
     * it has a colon-delimited subtitle, the truncated core title too - handling cases where
     * our catalog's title includes a subtitle ACLA's doesn't carry (or vice versa). Each
     * candidate must still be an exact match (after normalization) against an ACLA result to
     * count - this only widens what we search for, not what counts as a match.
     */
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

    /**
     * Strips trailing catalog noise (copy numbers, physical format labels) from a title
     * before it's used to search or match against ACLA.
     * See {@link Book#stripCopySuffix(String)} and {@link #TRAILING_FORMAT_SUFFIX_PATTERN}.
     */
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
     * Calls the ACLA BiblioCommons search API and returns the {@code entities.bibs} object.
     * Pass a pre-built URI rather than a String: RestTemplate treats a String URL as a URI
     * template and re-encodes it, which double-encodes the query we've already percent-encoded.
     */
    private JsonNode search(String title, String formatFilter) {
        String encodedQuery = urlEncode("\"" + title + "\"");
        StringBuilder url = new StringBuilder(SEARCH_URL)
                .append("?searchType=keyword&query=")
                .append(encodedQuery);
        if (formatFilter != null) {
            url.append("&f_FORMAT=").append(urlEncode(formatFilter));
        }

        URI uri = URI.create(url.toString());
        String response = http.call(uri.toString(), () -> aclaRestTemplate.getForObject(uri, String.class));
        try {
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            return root.path("entities").path("bibs");
        } catch (Exception e) {
            throw new com.muczynski.library.exception.LibraryException("Failed to parse ACLA response", e);
        }
    }

    private JsonNode searchMissingFormats(String title, String authorLastName, String[] formatFilters, Book book) {
        ArrayNode combined = objectMapper.createArrayNode();
        for (String formatFilter : formatFilters) {
            try {
                JsonNode matches = filterMatches(search(title, formatFilter), title, authorLastName);
                ArrayNode kept = objectMapper.createArrayNode();
                CatalogMatchKeys.keep(kept, matches, book.getInvalidHoldings(), PublicLibrary.ACLA, CatalogMatchKeys::aclaKey);
                for (JsonNode match : kept) {
                    combined.add(match);
                }
            } catch (Exception e) {
                // Bulk lookups often 403 on follow-up format searches after the
                // unfiltered title search already succeeded. Keep what we have.
                log.warn("ACLA follow-up format {} failed for '{}': {}",
                        formatFilter, title, e.getMessage());
                break;
            }
            if (!combined.isEmpty()) {
                break;
            }
        }
        return combined;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new com.muczynski.library.exception.LibraryException("Failed to encode ACLA query", e);
        }
    }

    /**
     * Filters the search results down to bibs that exactly match the book being looked up.
     *
     * <p>No fuzzy/substring matching of any kind: the title must be exactly equal (after
     * normalization) to count at all - this is what stops a short, generic title/name like
     * "Ellen" or "Joshua" from matching an unrelated title that merely starts with or contains
     * the same word. Beyond that, an exact title match by itself is only trusted when the
     * title is more than 4 words long, distinctive enough on its own; a title of 4 words or
     * fewer additionally requires the book's author's last name to exactly match one of the
     * words in one of the ACLA bib's author fields (again no substring matching).
     */
    private JsonNode filterMatches(JsonNode bibs, String title, String authorLastName) {
        com.fasterxml.jackson.databind.node.ArrayNode matches = objectMapper.createArrayNode();
        String normalizedTitle = normalize(title);
        int titleWordCount = normalizedTitle.isEmpty() ? 0 : normalizedTitle.split(" ").length;

        if (bibs != null && bibs.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = bibs.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode entry = field.getValue();
                if (entry instanceof ObjectNode object && !object.has("_recordKey")) {
                    object.put("_recordKey", field.getKey());
                }
                if (aclaTitleMatches(entry, normalizedTitle, titleWordCount, authorLastName)) {
                    matches.add(entry);
                }
            }
            return matches;
        }
        Iterator<JsonNode> entries = bibs.elements();
        while (entries.hasNext()) {
            JsonNode entry = entries.next();
            if (aclaTitleMatches(entry, normalizedTitle, titleWordCount, authorLastName)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    private boolean aclaTitleMatches(JsonNode entry, String normalizedTitle, int titleWordCount, String authorLastName) {
        JsonNode briefInfo = entry.path("briefInfo");
        String entryTitle = normalize(briefInfo.path("title").asText(""));
        if (entryTitle.isEmpty() || !entryTitle.equals(normalizedTitle)) {
            return false;
        }
        if (titleWordCount <= 4) {
            if (authorLastName == null) {
                return false;
            }
            if (!jsonArrayHasExactAuthorWordMatch(briefInfo.path("authors"), authorLastName)) {
                return false;
            }
        }
        return true;
    }

    private List<CatalogMatchRef> refsOf(JsonNode matches) {
        List<CatalogMatchRef> refs = new ArrayList<>();
        if (matches == null) {
            return refs;
        }
        for (JsonNode entry : matches) {
            String key = CatalogMatchKeys.aclaKey(entry);
            if (key != null && !key.isBlank()) {
                refs.add(new CatalogMatchRef(key, null));
            }
        }
        return refs;
    }

    /**
     * True if {@code authorLastName} exactly matches one of the (normalized) words across any
     * element of the given author-name array - a whole-word match, not a substring match, so a
     * last name like "White" doesn't match "Whitehead" or "Fitzwhite".
     */
    private boolean jsonArrayHasExactAuthorWordMatch(JsonNode array, String authorLastName) {
        String normalizedLastName = normalize(authorLastName);
        if (normalizedLastName.isEmpty()) {
            return false;
        }
        for (JsonNode element : array) {
            for (String word : normalize(element.asText("")).split(" ")) {
                if (word.equals(normalizedLastName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    private FormatFlags classifyMatches(JsonNode matches) {
        FormatFlags flags = new FormatFlags();
        for (JsonNode entry : matches) {
            String category = classifyEntry(entry);
            if ("audio".equals(category)) {
                flags.audio = true;
            } else if ("ebook".equals(category)) {
                flags.ebook = true;
            } else if ("paper".equals(category)) {
                flags.paper = true;
            }
        }
        return flags;
    }

    private boolean hasFormat(JsonNode matches, String category) {
        for (JsonNode entry : matches) {
            if (category.equals(classifyEntry(entry))) {
                return true;
            }
        }
        return false;
    }

    private String firstMatchedTitle(JsonNode matches, String fallback) {
        if (matches.isEmpty()) {
            return fallback;
        }
        String title = matches.get(0).path("briefInfo").path("title").asText("");
        return title.isEmpty() ? fallback : title;
    }

    /**
     * Classifies a matched BiblioCommons bib into "audio", "ebook", "paper", or "other"
     * based on format code, superFormats, and consumptionFormat.
     */
    private String classifyEntry(JsonNode entry) {
        JsonNode briefInfo = entry.path("briefInfo");
        String format = briefInfo.path("format").asText("").toUpperCase(Locale.ROOT);
        String consumption = briefInfo.path("consumptionFormat").asText("").toUpperCase(Locale.ROOT);
        boolean audiobookSuper = jsonArrayContains(briefInfo.path("superFormats"), "AUDIOBOOKS_SPOKEN_WORD");
        boolean electronicSuper = jsonArrayContains(briefInfo.path("superFormats"), "ELECTRONIC_FORMATS");
        boolean booksSuper = jsonArrayContains(briefInfo.path("superFormats"), "BOOKS");

        if ("LISTEN".equals(consumption) || audiobookSuper
                || format.equals("EAUDIOBOOK") || format.equals("BOOK_CD") || format.equals("MP3_CD")
                || format.equals("PLAYAWAY")) {
            return "audio";
        }
        if (format.equals("EBOOK") || ("READ".equals(consumption) && electronicSuper)) {
            return "ebook";
        }
        if (format.equals("BK") || format.equals("LPRINT") || format.equals("PICTURE_BOOK")
                || format.equals("GRAPHIC_NOVEL") || format.equals("BOARD_BK") || format.equals("BOOK_PDVD")
                || booksSuper) {
            return "paper";
        }
        return "other";
    }

    private boolean jsonArrayContains(JsonNode array, String value) {
        for (JsonNode element : array) {
            if (value.equalsIgnoreCase(element.asText(""))) {
                return true;
            }
        }
        return false;
    }

    private static final class FormatFlags {
        boolean audio;
        boolean paper;
        boolean ebook;
    }
}
