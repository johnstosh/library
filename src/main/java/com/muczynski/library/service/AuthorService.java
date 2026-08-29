/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.AuthorAvailabilityDto;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.AuthorEnrichmentResultDto;
import com.muczynski.library.dto.AuthorSummaryDto;
import com.muczynski.library.dto.BulkDeleteResultDto;
import com.muczynski.library.mapper.AuthorMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorService.class);

    private static final String CATALOG_SYSTEM_PROMPT = """
            You write Catholic library catalog cards. The prose fields are essays, not blurbs or jacket copy.
            Be frank, polite, and charitable. Do not hedge with a balanced viewpoint.
            Do not add any text outside the JSON object.""";

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorMapper authorMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AskGrok askGrok;

    @Autowired
    private ObjectMapper objectMapper;

    public AuthorDto createAuthor(AuthorDto authorDto) {
        String name = authorDto.getName();
        if (name != null && !authorRepository.findAllByNameOrderByIdAsc(name).isEmpty()) {
            throw new LibraryException("An author named \"" + name + "\" already exists");
        }
        Author author = authorMapper.toEntity(authorDto);
        Author savedAuthor = authorRepository.save(author);
        return authorMapper.toDto(savedAuthor);
    }

    public List<AuthorDto> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public AuthorDto getAuthorById(Long id) {
        return authorRepository.findByIdWithBooks(id)
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author, true);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .orElse(null);
    }

    public AuthorDto updateAuthor(Long id, AuthorDto authorDto) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new LibraryException("Author not found: " + id));
        Author updatedAuthor = authorMapper.toEntity(authorDto);
        updatedAuthor.setId(id);
        Author savedAuthor = authorRepository.save(updatedAuthor);
        return authorMapper.toDto(savedAuthor);
    }

    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new LibraryException("Author not found: " + id);
        }
        long bookCount = bookRepository.countByAuthorId(id);
        if (bookCount > 0) {
            throw new LibraryException("Cannot delete author because it has " + bookCount + " associated books.");
        }
        authorRepository.deleteById(id);
    }

    /**
     * Delete authors that can be deleted; skip authors with associated books.
     * Returns counts and error details for failed deletions.
     */
    public BulkDeleteResultDto deleteBulkAuthors(List<Long> authorIds) {
        List<Long> deletedIds = new ArrayList<>();
        List<BulkDeleteResultDto.BulkDeleteFailureDto> failures = new ArrayList<>();

        for (Long id : authorIds) {
            try {
                deleteAuthor(id);
                deletedIds.add(id);
            } catch (LibraryException e) {
                String name = authorRepository.findById(id)
                        .map(Author::getName)
                        .orElse("Unknown");
                failures.add(BulkDeleteResultDto.BulkDeleteFailureDto.builder()
                        .id(id)
                        .title(name)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return BulkDeleteResultDto.builder()
                .deletedCount(deletedIds.size())
                .failedCount(failures.size())
                .deletedIds(deletedIds)
                .failures(failures)
                .build();
    }

    /**
     * Fill blank catalog fields for an author using a Grok prompt.
     * Existing non-blank values are never overwritten. Name and grokipediaUrl are never changed.
     */
    public AuthorEnrichmentResultDto generateMissingData(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new LibraryException("Author not found: " + id));

        String name = author.getName();
        if (isBlank(name)) {
            return AuthorEnrichmentResultDto.builder()
                    .authorId(id)
                    .name(name)
                    .success(false)
                    .skipped(false)
                    .filledFields(List.of())
                    .errorMessage("Author has no name")
                    .updatedAuthor(toDtoWithBookCount(author))
                    .build();
        }

        Map<String, String> missingFieldSpecs = missingFieldSpecs(author);
        if (missingFieldSpecs.isEmpty()) {
            return AuthorEnrichmentResultDto.builder()
                    .authorId(id)
                    .name(name)
                    .success(true)
                    .skipped(true)
                    .filledFields(List.of())
                    .errorMessage(null)
                    .updatedAuthor(toDtoWithBookCount(author))
                    .build();
        }

        try {
            String question = buildGenerateMissingPrompt(author, missingFieldSpecs);
            String response = askGrok.askQuestion(question, CATALOG_SYSTEM_PROMPT);
            Map<String, Object> jsonData = extractJsonFromResponse(response);
            List<String> filledFields = applyMissingFields(author, missingFieldSpecs, jsonData);

            Author saved = filledFields.isEmpty() ? author : authorRepository.save(author);
            return AuthorEnrichmentResultDto.builder()
                    .authorId(id)
                    .name(name)
                    .success(true)
                    .skipped(false)
                    .filledFields(filledFields)
                    .errorMessage(null)
                    .updatedAuthor(toDtoWithBookCount(saved))
                    .build();
        } catch (Exception e) {
            logger.warn("Failed to generate missing data for author ID {}: {}", id, e.getMessage(), e);
            return AuthorEnrichmentResultDto.builder()
                    .authorId(id)
                    .name(name)
                    .success(false)
                    .skipped(false)
                    .filledFields(List.of())
                    .errorMessage(e.getMessage())
                    .updatedAuthor(toDtoWithBookCount(author))
                    .build();
        }
    }

    public int deleteAuthorsWithNoBooks() {
        List<Author> allAuthors = authorRepository.findAll();
        int deletedCount = 0;

        for (Author author : allAuthors) {
            long bookCount = bookRepository.countByAuthorId(author.getId());
            if (bookCount == 0) {
                authorRepository.deleteById(author.getId());
                deletedCount++;
            }
        }

        return deletedCount;
    }

    /**
     * Find or create an author by name
     * @param name Author name
     * @return The existing or newly created author
     */
    public Author findOrCreateAuthor(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "John Doe";
        }

        List<Author> existingAuthors = authorRepository.findAllByNameOrderByIdAsc(name);
        if (!existingAuthors.isEmpty()) {
            return existingAuthors.get(0);
        }

        // Create new author
        Author newAuthor = new Author();
        newAuthor.setName(name);
        return authorRepository.save(newAuthor);
    }

    /**
     * Get authors without a biographical essay
     */
    public List<AuthorDto> getAuthorsWithoutDescription() {
        return authorRepository.findAll().stream()
                .filter(author -> author.getBiographicalEssay() == null || author.getBiographicalEssay().trim().isEmpty())
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * Get authors with zero books
     */
    public List<AuthorDto> getAuthorsWithZeroBooks() {
        return authorRepository.findAll().stream()
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    long bookCount = bookRepository.countByAuthorId(author.getId());
                    dto.setBookCount(bookCount);
                    return dto;
                })
                .filter(dto -> dto.getBookCount() == 0)
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * Get authors without a Grokipedia URL
     */
    public List<AuthorDto> getAuthorsWithoutGrokipedia() {
        return authorRepository.findAll().stream()
                .filter(author -> author.getGrokipediaUrl() == null || author.getGrokipediaUrl().trim().isEmpty())
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * Get authors who have books added on the most recent day
     */
    public List<AuthorDto> getAuthorsFromMostRecentDay() {
        LocalDateTime maxDateTime = bookRepository.findMaxDateAddedToLibrary();
        if (maxDateTime == null) {
            return List.of();
        }
        // Get the date portion only (start of day)
        LocalDateTime startOfDay = maxDateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // Get all books from the most recent day
        List<Long> authorIds = bookRepository.findByDateAddedToLibraryBetweenOrderByDateAddedDesc(startOfDay, endOfDay).stream()
                .map(book -> book.getAuthor() != null ? book.getAuthor().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // Get authors for these IDs
        return authorRepository.findAllById(authorIds).stream()
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * Get all author summaries (id + lastModified) for caching.
     * Uses a projection so @Lob biography fields are not loaded.
     */
    public List<AuthorSummaryDto> getAllAuthorSummaries() {
        return authorRepository.findAllSummaries().stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    public long countAuthors() {
        return authorRepository.count();
    }

    /**
     * Get summaries for authors without a biographical essay.
     */
    public List<AuthorSummaryDto> getSummariesWithoutDescription() {
        return authorRepository.findSummariesWithoutDescription().stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get summaries for authors with zero books.
     */
    public List<AuthorSummaryDto> getSummariesWithZeroBooks() {
        return authorRepository.findSummariesWithZeroBooks().stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get summaries for authors without a Grokipedia URL.
     */
    public List<AuthorSummaryDto> getSummariesWithoutGrokipedia() {
        return authorRepository.findSummariesWithoutGrokipedia().stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get summaries for authors who have books added on the most recent day.
     */
    public List<AuthorSummaryDto> getSummariesFromMostRecentDay() {
        LocalDateTime maxDateTime = bookRepository.findMaxDateAddedToLibrary();
        if (maxDateTime == null) {
            return List.of();
        }
        LocalDateTime startOfDay = maxDateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Long> authorIds = bookRepository.findByDateAddedToLibraryBetweenOrderByDateAddedDesc(startOfDay, endOfDay).stream()
                .map(book -> book.getAuthor() != null ? book.getAuthor().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (authorIds.isEmpty()) {
            return List.of();
        }
        return authorRepository.findSummariesByIds(authorIds).stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Roll up each author's YDL/EMU book/ebook/audio holdings from their books.
     * Authors with no true flags are omitted; the frontend treats a missing id as all false.
     */
    public List<AuthorAvailabilityDto> getAuthorAvailability() {
        return bookRepository.countAvailabilityByAuthor().stream()
                .map(this::projectionToAvailabilityDto)
                .filter(this::hasAnyAvailability)
                .collect(Collectors.toList());
    }

    private AuthorAvailabilityDto projectionToAvailabilityDto(BookRepository.AuthorAvailabilityProjection projection) {
        AuthorAvailabilityDto dto = new AuthorAvailabilityDto();
        dto.setAuthorId(projection.getAuthorId());
        dto.setHasYdlBook(positive(projection.getYdlPaperCount()));
        dto.setHasYdlEbook(positive(projection.getYdlEbookCount()));
        dto.setHasYdlAudio(positive(projection.getYdlAudioCount()));
        dto.setHasEmuBook(positive(projection.getEmuPaperCount()));
        dto.setHasEmuEbook(positive(projection.getEmuEbookCount()));
        dto.setHasEmuAudio(positive(projection.getEmuAudioCount()));
        return dto;
    }

    private boolean hasAnyAvailability(AuthorAvailabilityDto dto) {
        return Boolean.TRUE.equals(dto.getHasYdlBook())
                || Boolean.TRUE.equals(dto.getHasYdlEbook())
                || Boolean.TRUE.equals(dto.getHasYdlAudio())
                || Boolean.TRUE.equals(dto.getHasEmuBook())
                || Boolean.TRUE.equals(dto.getHasEmuEbook())
                || Boolean.TRUE.equals(dto.getHasEmuAudio());
    }

    private static boolean positive(Long count) {
        return count != null && count > 0;
    }

    private AuthorSummaryDto projectionToSummaryDto(AuthorRepository.AuthorSummaryProjection projection) {
        AuthorSummaryDto dto = new AuthorSummaryDto();
        dto.setId(projection.getId());
        dto.setLastModified(projection.getLastModified());
        return dto;
    }

    /**
     * Get authors by IDs for batch fetching.
     */
    public List<AuthorDto> getAuthorsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return authorRepository.findAllById(ids).stream()
                .map(author -> {
                    AuthorDto dto = authorMapper.toDto(author);
                    dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
                    return dto;
                })
                .sorted(Comparator.comparing(author -> {
                    if (author == null || author.getName() == null || author.getName().trim().isEmpty()) {
                        return null;
                    }
                    String[] nameParts = author.getName().trim().split("\\s+");
                    return nameParts.length > 0 ? nameParts[nameParts.length - 1] : "";
                }, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    private AuthorDto toDtoWithBookCount(Author author) {
        AuthorDto dto = authorMapper.toDto(author);
        dto.setBookCount(bookRepository.countByAuthorId(author.getId()));
        return dto;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Maps AuthorDto field names to prompt descriptions for currently blank fields.
     */
    private Map<String, String> missingFieldSpecs(Author author) {
        Map<String, String> specs = new LinkedHashMap<>();
        if (author.getDateOfBirth() == null) {
            specs.put("dateOfBirth", "birth date in YYYY-MM-DD format, or null if unknown");
        }
        if (author.getDateOfDeath() == null) {
            specs.put("dateOfDeath", "death date in YYYY-MM-DD format, or null if alive or unknown");
        }
        if (isBlank(author.getReligiousAffiliation())) {
            specs.put("religiousAffiliation", "the author's religious affiliation; be frank if they were heretics or lapsed");
        }
        if (isBlank(author.getBirthCountry())) {
            specs.put("birthCountry", "the author's country of birth");
        }
        if (isBlank(author.getNationality())) {
            specs.put("nationality", "the author's nationality, or nationalities");
        }
        if (isBlank(author.getBiographicalEssay())) {
            specs.put("biographicalEssay", "a frank Catholic biography highlighting virtues, public sins, and conversion. Write 2-4 paragraphs, about 200-400 words. Not a one-sentence blurb.");
        }
        return specs;
    }

    private String buildGenerateMissingPrompt(Author author, Map<String, String> missingFieldSpecs) {
        List<String> bookTitles = bookRepository.findByAuthorIdOrderByTitleAsc(author.getId()).stream()
                .map(Book::getTitle)
                .filter(title -> !isBlank(title))
                .collect(Collectors.toList());

        String quotedTitles = bookTitles.stream()
                .map(title -> "\"" + title + "\"")
                .collect(Collectors.joining(", "));
        String booksPart = bookTitles.isEmpty()
                ? "This library has no cataloged books by this author, so there are no titles here to distinguish them from anyone else with the same name."
                : "This is specifically the author of " + quotedTitles
                    + ". Other people may share this name; identify this author by those works and do not mix in biography, dates, or affiliation from a namesake.";

        StringBuilder known = new StringBuilder();
        if (author.getDateOfBirth() != null) {
            known.append("- dateOfBirth: ").append(author.getDateOfBirth()).append('\n');
        }
        if (author.getDateOfDeath() != null) {
            known.append("- dateOfDeath: ").append(author.getDateOfDeath()).append('\n');
        }
        if (!isBlank(author.getReligiousAffiliation())) {
            known.append("- religiousAffiliation: ").append(author.getReligiousAffiliation().trim()).append('\n');
        }
        if (!isBlank(author.getBirthCountry())) {
            known.append("- birthCountry: ").append(author.getBirthCountry().trim()).append('\n');
        }
        if (!isBlank(author.getNationality())) {
            known.append("- nationality: ").append(author.getNationality().trim()).append('\n');
        }
        if (!isBlank(author.getBiographicalEssay())) {
            known.append("- biographicalEssay: already present (do not rewrite)\n");
        }

        StringBuilder missing = new StringBuilder();
        StringBuilder jsonKeys = new StringBuilder();
        boolean firstJson = true;
        for (Map.Entry<String, String> entry : missingFieldSpecs.entrySet()) {
            missing.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
            if (!firstJson) {
                jsonKeys.append(", ");
            }
            jsonKeys.append('"').append(entry.getKey()).append("\": value");
            firstJson = false;
        }

        String knownBlock = known.length() == 0
                ? "None; every catalog field below is blank."
                : known.toString().trim();

        return String.format("""
                Research the author named "%s". %s
                Provide a card catalog entry from a Catholic perspective.
                Be frank in your assessments, without providing a balanced view. Emphasize Catholic teachings,
                saints, and doctrine where applicable.

                Already known (do not change these, and do not change the author's name):
                %s

                Fill only these missing fields:
                %s
                Respond only with a JSON object with this structure (no text before or after):
                {%s}""",
                author.getName().trim(),
                booksPart,
                knownBlock,
                missing.toString().trim(),
                jsonKeys);
    }

    private List<String> applyMissingFields(Author author, Map<String, String> missingFieldSpecs, Map<String, Object> jsonData) {
        List<String> filled = new ArrayList<>();

        if (missingFieldSpecs.containsKey("dateOfBirth")) {
            LocalDate date = parseJsonDate(jsonData, "dateOfBirth");
            if (date != null) {
                author.setDateOfBirth(date);
                filled.add("dateOfBirth");
            }
        }
        if (missingFieldSpecs.containsKey("dateOfDeath")) {
            LocalDate date = parseJsonDate(jsonData, "dateOfDeath");
            if (date != null) {
                author.setDateOfDeath(date);
                filled.add("dateOfDeath");
            }
        }
        if (missingFieldSpecs.containsKey("religiousAffiliation")) {
            String value = firstNonBlankString(jsonData, "religiousAffiliation");
            if (value != null) {
                author.setReligiousAffiliation(value);
                filled.add("religiousAffiliation");
            }
        }
        if (missingFieldSpecs.containsKey("birthCountry")) {
            String value = firstNonBlankString(jsonData, "birthCountry");
            if (value != null) {
                author.setBirthCountry(value);
                filled.add("birthCountry");
            }
        }
        if (missingFieldSpecs.containsKey("nationality")) {
            String value = firstNonBlankString(jsonData, "nationality");
            if (value != null) {
                author.setNationality(value);
                filled.add("nationality");
            }
        }
        if (missingFieldSpecs.containsKey("biographicalEssay")) {
            String value = firstNonBlankString(jsonData, "biographicalEssay", "briefBiography");
            if (value != null) {
                author.setBiographicalEssay(value);
                filled.add("biographicalEssay");
            }
        }

        return filled;
    }

    private LocalDate parseJsonDate(Map<String, Object> jsonData, String key) {
        String value = firstNonBlankString(jsonData, key);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            logger.debug("Could not parse {} date: {}", key, value);
            return null;
        }
    }

    private static String firstNonBlankString(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String) {
                String trimmed = ((String) value).trim();
                if (!trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed)) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractJsonFromResponse(String response) {
        if (response == null) {
            throw new LibraryException("No valid JSON found in response - empty response");
        }
        String trimmedResponse = response.trim();

        int startIndex = trimmedResponse.indexOf('{');
        if (startIndex == -1) {
            logger.debug("No opening brace found in AI response: {}", trimmedResponse);
            throw new LibraryException("No valid JSON found in response - no opening brace");
        }

        int braceCount = 0;
        int endIndex = -1;
        for (int i = startIndex; i < trimmedResponse.length(); i++) {
            char c = trimmedResponse.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIndex = i;
                    break;
                }
            }
        }

        if (endIndex == -1) {
            logger.debug("No closing brace found in AI response: {}", trimmedResponse);
            throw new LibraryException("No valid JSON found in response - unbalanced braces");
        }

        String jsonSubstring = trimmedResponse.substring(startIndex, endIndex + 1);
        try {
            return objectMapper.readValue(jsonSubstring, Map.class);
        } catch (Exception e) {
            logger.debug("Failed to parse JSON from AI response substring: {}", jsonSubstring, e);
            throw new LibraryException("Failed to parse JSON from response: " + e.getMessage(), e);
        }
    }

}
