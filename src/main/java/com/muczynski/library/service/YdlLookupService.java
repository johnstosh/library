/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.YdlLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Looks up book availability (audio, paper, ebook) at YDL (Ypsilanti District Library) via
 * the public search API used by YDL's own Angular catalog frontend, and stores the result on
 * the Book entity. Interlibrary-loan-only offerings are not represented as material tabs in
 * this API, so any material tab present in a matched result is treated as a genuine holding.
 */
@Service
@Slf4j
@Transactional
public class YdlLookupService {

    private static final String SEARCH_URL = "https://na4.iiivega.com/api/search-result/search/format-groups";
    private static final String CUSTOMER_DOMAIN = "ypsilantidl.na4.iiivega.com";

    /**
     * Matches trailing physical-format labels our own titles often carry that YDL's
     * catalog titles don't have, e.g. " (DVD)", ", Audio CD". Copy-number suffixes
     * (", c. 2") are stripped separately via {@link Book#stripCopySuffix(String)}.
     * Applied repeatedly so stacked suffixes (", c. 2, Audio CD") are all stripped
     * before the title is used to search or match against YDL.
     */
    private static final Pattern TRAILING_FORMAT_SUFFIX_PATTERN = Pattern.compile(
            "(?i)(\\s*\\((?:dvd|cd|vhs|blu-?ray|book on cd|large print|audiobook)\\)"
                    + "|,\\s*(?:audio\\s*cd|book\\s*on\\s*cd|large\\s*print|dvd|cd|vhs))\\s*$");

    private final BookRepository bookRepository;
    private final RestTemplate ydlRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YdlLookupService(BookRepository bookRepository,
                             @Qualifier("ydlRestTemplate") RestTemplate ydlRestTemplate) {
        this.bookRepository = bookRepository;
        this.ydlRestTemplate = ydlRestTemplate;
    }

    /**
     * Lookup and update YDL availability for a single book.
     */
    public YdlLookupResultDto lookupAndUpdateBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new com.muczynski.library.exception.LibraryException("Book not found: " + bookId));

        return performYdlLookup(book);
    }

    private YdlLookupResultDto performYdlLookup(Book book) {
        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            log.info("Skipping YDL lookup for temporary title: {}", book.getTitle());
            book.setYdlLastChecked(LocalDateTime.now());
            book.setYdlLookupError("Not Ready - Temporary title");
            bookRepository.save(book);
            return YdlLookupResultDto.builder()
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
                    // exist only to improve YDL's hit rate, not to relax what counts as a match.
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
                // A completed search that found nothing is a definitive answer - clear any
                // stale availability from a previous lookup rather than leaving it untouched.
                book.setYdlAudioAvailable(false);
                book.setYdlPaperAvailable(false);
                book.setYdlEbookAvailable(false);
                book.setYdlLastChecked(LocalDateTime.now());
                book.setYdlLookupError("Not held by YDL");
                bookRepository.save(book);
                return YdlLookupResultDto.builder()
                        .bookId(book.getId())
                        .success(false)
                        .audioAvailable(false)
                        .paperAvailable(false)
                        .ebookAvailable(false)
                        .errorMessage("Not held by YDL")
                        .build();
            }

            boolean audio = false;
            boolean paper = false;
            boolean ebook = false;
            String matchedTitle = matches.get(0).path("title").asText(cleanedTitle);

            for (JsonNode entry : matches) {
                for (JsonNode tab : entry.path("materialTabs")) {
                    String category = classifyMaterialTab(tab.path("name").asText(""));
                    if ("audio".equals(category)) {
                        audio = true;
                    } else if ("ebook".equals(category)) {
                        ebook = true;
                    } else if ("paper".equals(category)) {
                        paper = true;
                    }
                }
            }

            book.setYdlAudioAvailable(audio);
            book.setYdlPaperAvailable(paper);
            book.setYdlEbookAvailable(ebook);
            book.setYdlLastChecked(LocalDateTime.now());
            book.setYdlLookupError(null);
            bookRepository.save(book);

            log.info("YDL lookup for book {} ('{}'): audio={}, paper={}, ebook={}",
                    book.getId(), book.getTitle(), audio, paper, ebook);

            return YdlLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(true)
                    .audioAvailable(audio)
                    .paperAvailable(paper)
                    .ebookAvailable(ebook)
                    .matchedTitle(matchedTitle)
                    .build();

        } catch (Exception e) {
            log.error("Error during YDL lookup for book {}: {}", book.getId(), e.getMessage());
            book.setYdlLastChecked(LocalDateTime.now());
            book.setYdlLookupError("Error: " + e.getMessage());
            bookRepository.save(book);
            return YdlLookupResultDto.builder()
                    .bookId(book.getId())
                    .success(false)
                    .errorMessage("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Builds the ordered list of title candidates to search for: the cleaned title, then, if
     * it has a colon-delimited subtitle, the truncated core title too - handling cases where
     * our catalog's title includes a subtitle YDL's doesn't carry (or vice versa). Each
     * candidate must still be an exact match (after normalization) against a YDL result to
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
     * Builds the ordered list of author values to include in the YDL search text itself
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
     * before it's used to search or match against YDL.
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
     * Calls the YDL search API and returns the "data" array of matched FormatGroup entries.
     */
    private JsonNode search(String title, String authorLastName) {
        String searchText = authorLastName != null
                ? "\"" + title + "\" " + authorLastName
                : "\"" + title + "\"";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("searchText", searchText);
        body.put("sorting", "relevance");
        body.put("sortOrder", "asc");
        body.put("searchType", "everything");
        body.put("pageNum", 0);
        body.put("pageSize", 10);
        body.put("resourceType", "FormatGroup");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("api-version", "2");
        headers.set("iii-customer-domain", CUSTOMER_DOMAIN);
        headers.set("iii-host-domain", CUSTOMER_DOMAIN);
        headers.set("Referer", "https://" + CUSTOMER_DOMAIN + "/");

        String response = ydlRestTemplate.postForObject(SEARCH_URL, new HttpEntity<>(body, headers), String.class);
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("data");
        } catch (Exception e) {
            throw new com.muczynski.library.exception.LibraryException("Failed to parse YDL response", e);
        }
    }

    /**
     * Filters the search results down to entries that exactly match the book being looked up.
     *
     * <p>No fuzzy/substring matching of any kind: the title must be exactly equal (after
     * normalization) to count at all - this is what stops a short, generic title/name like
     * "Ellen" or "Joshua" from matching an unrelated title that merely starts with or contains
     * the same word (e.g. "Joshua in a Troubled World"). Beyond that, an exact title match by
     * itself is only trusted when the title is more than 4 words long, distinctive enough on
     * its own; a title of 4 words or fewer additionally requires the book's author's last name
     * to exactly match one of the words in the YDL entry's author label (again no substring
     * matching, so "White" doesn't match "Whitehead").
     */
    private JsonNode filterMatches(JsonNode entries, String title, String authorLastName) {
        com.fasterxml.jackson.databind.node.ArrayNode matches = objectMapper.createArrayNode();
        String normalizedTitle = normalize(title);
        int titleWordCount = normalizedTitle.isEmpty() ? 0 : normalizedTitle.split(" ").length;

        for (JsonNode entry : entries) {
            String entryTitle = normalize(entry.path("title").asText(""));
            if (entryTitle.isEmpty() || !entryTitle.equals(normalizedTitle)) {
                continue;
            }

            if (titleWordCount <= 4) {
                if (authorLastName == null) {
                    continue;
                }
                String agentLabel = entry.path("primaryAgent").path("label").asText("");
                if (!hasExactAuthorWordMatch(agentLabel, authorLastName)) {
                    continue;
                }
            }

            matches.add(entry);
        }
        return matches;
    }

    /**
     * True if {@code authorLastName} exactly matches one of the (normalized) words in
     * {@code agentLabel} - a whole-word match, not a substring match, so a last name like
     * "White" doesn't match "Whitehead" or "Fitzwhite".
     */
    private boolean hasExactAuthorWordMatch(String agentLabel, String authorLastName) {
        String normalizedLastName = normalize(authorLastName);
        if (normalizedLastName.isEmpty()) {
            return false;
        }
        for (String word : normalize(agentLabel).split(" ")) {
            if (word.equals(normalizedLastName)) {
                return true;
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
     * Classifies a YDL materialTab name into "audio", "ebook", "paper", or "other".
     */
    private String classifyMaterialTab(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("audio")) {
            return "audio";
        }
        if (n.contains("ebook")) {
            return "ebook";
        }
        if (n.contains("book")) {
            return "paper";
        }
        return "other";
    }
}
