/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.EmuLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Looks up book availability (audio, paper, ebook) at EMU's Halle Library via the public
 * search API used by its Ex Libris Primo VE catalog frontend, and stores the result on the
 * Book entity. Only "local" (context == "L", i.e. actually held/subscribed by EMU) results are
 * considered — the wider "PC" central-discovery-index results (e.g. journal articles indexed
 * but not held) are excluded.
 */
@Service
@Slf4j
@Transactional
public class EmuLookupService {

    private static final String SEARCH_URL = "https://emich.primo.exlibrisgroup.com/primaws/rest/pub/pnxs";
    private static final String INSTITUTION = "01EMU_INST";
    private static final String VID = "01EMU_INST:EMU";

    /**
     * Matches trailing catalog noise our own titles often carry (copy numbers, physical
     * format labels) that EMU's catalog titles don't have, e.g. ", c. 2", " (DVD)",
     * ", Audio CD". Applied repeatedly so stacked suffixes (", c. 2, Audio CD") are all
     * stripped before the title is used to search or match against EMU.
     */
    private static final Pattern TRAILING_SUFFIX_PATTERN = Pattern.compile(
            "(?i)(,?\\s*c\\.?\\s*\\d+"
                    + "|\\s*\\((?:dvd|cd|vhs|blu-?ray|book on cd|large print|audiobook)\\)"
                    + "|,\\s*(?:audio\\s*cd|book\\s*on\\s*cd|large\\s*print|dvd|cd|vhs))\\s*$");

    private final BookRepository bookRepository;
    private final RestTemplate emuRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmuLookupService(BookRepository bookRepository,
                             @Qualifier("emuRestTemplate") RestTemplate emuRestTemplate) {
        this.bookRepository = bookRepository;
        this.emuRestTemplate = emuRestTemplate;
    }

    /**
     * Lookup and update EMU Halle Library availability for a single book.
     */
    public EmuLookupResultDto lookupAndUpdateBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new com.muczynski.library.exception.LibraryException("Book not found: " + bookId));

        return performEmuLookup(book);
    }

    private EmuLookupResultDto performEmuLookup(Book book) {
        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            log.info("Skipping EMU lookup for temporary title: {}", book.getTitle());
            book.setEmuLastChecked(LocalDateTime.now());
            book.setEmuLookupError("Not Ready - Temporary title");
            bookRepository.save(book);
            return EmuLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage("Not Ready - Temporary title")
                    .build();
        }

        String cleanedTitle = cleanTitle(book.getTitle());
        String authorLastName = null;
        if (book.getAuthor() != null && book.getAuthor().getName() != null) {
            String[] parts = book.getAuthor().getName().trim().split("\\s+");
            if (parts.length > 0) {
                authorLastName = parts[parts.length - 1];
            }
        }

        try {
            JsonNode matches = objectMapper.createArrayNode();
            for (String candidateTitle : buildTitleCandidates(cleanedTitle)) {
                for (String queryAuthor : buildQueryAuthors(authorLastName)) {
                    JsonNode entries = search(candidateTitle, queryAuthor);
                    // Verification always checks the book's real author, regardless of whether
                    // this particular query included it in the search text - the query variants
                    // exist only to improve EMU's hit rate, not to relax what counts as a match.
                    matches = filterMatches(entries, candidateTitle, authorLastName);
                    if (!matches.isEmpty()) {
                        break;
                    }
                }
                if (!matches.isEmpty()) {
                    break;
                }
            }

            if (matches.isEmpty()) {
                book.setEmuLastChecked(LocalDateTime.now());
                book.setEmuLookupError("Not held by EMU Halle Library");
                bookRepository.save(book);
                return EmuLookupResultDto.builder()
                        .bookId(book.getId())
                        .success(false)
                        .errorMessage("Not held by EMU Halle Library")
                        .build();
            }

            boolean audio = false;
            boolean paper = false;
            boolean ebook = false;
            String matchedTitle = matches.get(0).path("pnx").path("display").path("title").path(0).asText(cleanedTitle);

            for (JsonNode entry : matches) {
                String category = classifyEntry(entry);
                if ("audio".equals(category)) {
                    audio = true;
                } else if ("ebook".equals(category)) {
                    ebook = true;
                } else if ("paper".equals(category)) {
                    paper = true;
                }
            }

            book.setEmuAudioAvailable(audio);
            book.setEmuPaperAvailable(paper);
            book.setEmuEbookAvailable(ebook);
            book.setEmuLastChecked(LocalDateTime.now());
            book.setEmuLookupError(null);
            bookRepository.save(book);

            log.info("EMU lookup for book {} ('{}'): audio={}, paper={}, ebook={}",
                    book.getId(), book.getTitle(), audio, paper, ebook);

            return EmuLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(true)
                    .audioAvailable(audio)
                    .paperAvailable(paper)
                    .ebookAvailable(ebook)
                    .matchedTitle(matchedTitle)
                    .build();

        } catch (Exception e) {
            log.error("Error during EMU lookup for book {}: {}", book.getId(), e.getMessage());
            book.setEmuLastChecked(LocalDateTime.now());
            book.setEmuLookupError("Error: " + e.getMessage());
            bookRepository.save(book);
            return EmuLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Builds the ordered list of title candidates to search for: the cleaned title, then, if
     * it has a colon-delimited subtitle, the truncated core title too - handling cases where
     * our catalog's title includes a subtitle EMU's doesn't carry (or vice versa). Each
     * candidate must still be an exact match (after normalization) against an EMU result to
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
     * Builds the ordered list of author values to include in the EMU search text itself
     * (title + author search first, since it's more likely to rank the real match at the top;
     * title-only as a fallback). This only affects what we search for - {@link #filterMatches}
     * always verifies against the book's real author regardless of which query found the
     * result.
     */
    private List<String> buildQueryAuthors(String authorLastName) {
        List<String> queryAuthors = new ArrayList<>();
        if (authorLastName != null) {
            queryAuthors.add(authorLastName);
        }
        queryAuthors.add(null);
        return queryAuthors;
    }

    /**
     * Strips trailing catalog noise (copy numbers, physical format labels) from a title
     * before it's used to search or match against EMU. See {@link #TRAILING_SUFFIX_PATTERN}.
     */
    private String cleanTitle(String title) {
        if (title == null) {
            return "";
        }
        String cleaned = title.trim();
        String previous;
        do {
            previous = cleaned;
            cleaned = TRAILING_SUFFIX_PATTERN.matcher(cleaned).replaceAll("").trim();
        } while (!cleaned.equals(previous) && !cleaned.isEmpty());
        return cleaned.isEmpty() ? title.trim() : cleaned;
    }

    /**
     * Calls the EMU Primo pnxs search API and returns the "docs" array of matched records.
     */
    private JsonNode search(String title, String authorLastName) {
        String queryValue = "any,contains,\"" + title + "\"" + (authorLastName != null ? " " + authorLastName : "");
        String encodedQuery = urlEncode(queryValue);

        String url = SEARCH_URL
                + "?blendFacetsSeparately=false&disableCache=false&getMore=0"
                + "&inst=" + INSTITUTION
                + "&lang=en&limit=10&offset=0"
                + "&q=" + encodedQuery
                + "&qExclude=&qInclude=&rapido=false&scope=MyInst_and_CI"
                + "&skipDelivery=Y&sort=rank&tab=Everything"
                + "&vid=" + urlEncode(VID);

        // Pass a pre-built URI rather than a String: RestTemplate treats a String URL as a URI
        // template and re-encodes it via UriComponentsBuilder, which double-encodes the query
        // string we've already percent-encoded above and makes Primo reject the request with a
        // 400. A URI is used as-is, with no further encoding.
        String response = emuRestTemplate.getForObject(URI.create(url), String.class);
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("docs");
        } catch (Exception e) {
            throw new com.muczynski.library.exception.LibraryException("Failed to parse EMU response", e);
        }
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new com.muczynski.library.exception.LibraryException("Failed to encode EMU query", e);
        }
    }

    /**
     * Filters the search results down to locally-held entries (context == "L") that exactly
     * match the book being looked up.
     *
     * <p>No fuzzy/substring matching of any kind: the title must be exactly equal (after
     * normalization) to count at all - this is what stops a short, generic title/name like
     * "Ellen" or "Joshua" from matching an unrelated title that merely starts with or contains
     * the same word. Beyond that, an exact title match by itself is only trusted when the
     * title is more than 4 words long, distinctive enough on its own; a title of 4 words or
     * fewer additionally requires the book's author's last name to exactly match one of the
     * words in one of the EMU record's author fields (again no substring matching).
     */
    private JsonNode filterMatches(JsonNode entries, String title, String authorLastName) {
        com.fasterxml.jackson.databind.node.ArrayNode matches = objectMapper.createArrayNode();
        String normalizedTitle = normalize(title);
        int titleWordCount = normalizedTitle.isEmpty() ? 0 : normalizedTitle.split(" ").length;

        for (JsonNode entry : entries) {
            if (!"L".equals(entry.path("context").asText(""))) {
                continue;
            }
            JsonNode display = entry.path("pnx").path("display");
            String entryTitle = normalize(display.path("title").path(0).asText(""));
            if (entryTitle.isEmpty() || !entryTitle.equals(normalizedTitle)) {
                continue;
            }

            if (titleWordCount <= 4) {
                if (authorLastName == null) {
                    continue;
                }
                JsonNode addata = entry.path("pnx").path("addata");
                boolean authorMatches = jsonArrayHasExactAuthorWordMatch(addata.path("au"), authorLastName)
                        || jsonArrayHasExactAuthorWordMatch(addata.path("addau"), authorLastName);
                if (!authorMatches) {
                    continue;
                }
            }

            matches.add(entry);
        }
        return matches;
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

    /**
     * Classifies a matched Primo record into "audio", "ebook", "paper", or "other" based on
     * its display type ("book"/"audio") and format text (electronic resources describe
     * themselves as "online resource").
     */
    private String classifyEntry(JsonNode entry) {
        JsonNode display = entry.path("pnx").path("display");
        String type = display.path("type").path(0).asText("").toLowerCase(Locale.ROOT);
        String format = display.path("format").path(0).asText("").toLowerCase(Locale.ROOT);

        if ("audio".equals(type)) {
            return "audio";
        }
        if ("book".equals(type)) {
            return format.contains("online resource") ? "ebook" : "paper";
        }
        return "other";
    }
}
