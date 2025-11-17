package com.muczynski.library.service;

import com.muczynski.library.model.LocCallNumberResponse;
import com.muczynski.library.model.LocSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Production-grade Spring Service for querying the Library of Congress SRU API
 * to retrieve LOC Call Numbers (LCC) from title and author.
 *
 * <p>Uses public SRU endpoint (no credentials required):
 * https://lx2.loc.gov:210/LCDB</p>
 *
 * <p>Dependencies (Gradle coordinates):</p>
 * <pre>
 * // Spring Boot Web (includes RestTemplate)
 * implementation 'org.springframework.boot:spring-boot-starter-web'
 *
 * // Lombok
 * annotationProcessor 'org.projectlombok:lombok'
 * compileOnly 'org.projectlombok:lombok'
 *
 * // Marc4J for parsing MARCXML responses
 * implementation 'org.marc4j:marc4j:2.5.15'
 *
 * // SLF4J (included with Spring Boot)
 * // implementation 'org.springframework.boot:spring-boot-starter-logging'
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocCatalogService {

    private static final String SRU_BASE_URL = "https://lx2.loc.gov:210/LCDB";
    private static final int DEFAULT_MAX_RECORDS = 5;
    private static final int MAX_ALLOWED_RECORDS = 50;

    private final RestTemplate restTemplate;

    /**
     * Searches the LOC catalog by title and/or author and returns the first valid LOC call number.
     *
     * @param request contains title and/or author
     * @return LocCallNumberResponse with call number and metadata
     * @throws ResponseStatusException if no results or API error
     */
    public LocCallNumberResponse getLocCallNumber(LocSearchRequest request) {
        validateRequest(request);

        String cqlQuery = buildCqlQuery(request);
        String url = buildSruUrl(cqlQuery, DEFAULT_MAX_RECORDS);

        log.info("Querying LOC SRU API: {}", url);
        String marcXmlResponse = restTemplate.getForObject(url, String.class);

        if (marcXmlResponse == null || marcXmlResponse.trim().isEmpty()) {
            log.warn("Empty response from LOC API for query: {}", cqlQuery);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No response from LOC catalog");
        }

        List<String> callNumbers = extractLocCallNumbers(marcXmlResponse);
        if (callNumbers.isEmpty()) {
            log.info("No LOC call numbers found for title='{}', author='{}'", request.getTitle(), request.getAuthor());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No LOC call number found for the given title/author");
        }

        String primaryCallNumber = callNumbers.get(0); // Mostly return first (most relevant)
        log.info("Found LOC call number: {}", primaryCallNumber);

        return LocCallNumberResponse.builder()
                .callNumber(primaryCallNumber)
                .source("Library of Congress SRU API")
                .matchCount(callNumbers.size())
                .allCallNumbers(callNumbers)
                .build();
    }

    private void validateRequest(LocSearchRequest request) {
        if ((request.getTitle() == null || request.getTitle().trim().isEmpty()) &&
            (request.getAuthor() == null || request.getAuthor().trim().isEmpty())) {
            throw new IllegalArgumentException("At least one of title or author must be provided");
        }
    }

    private String buildCqlQuery(LocSearchRequest request) {
        List<String> conditions = new ArrayList<>();

        Optional.ofNullable(request.getTitle())
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .ifPresent(t -> conditions.add("title=\"" + escapeCql(t) + "\""));

        Optional.ofNullable(request.getAuthor())
                .map(String::trim)
                .filter(a -> !a.isEmpty())
                .ifPresent(a -> conditions.add("creatorany=\"" + escapeCql(a) + "\""));

        return String.join(" AND ", conditions);
    }

    private String escapeCql(String input) {
        return input.replace("\"", "\\\"");
    }

    private String buildSruUrl(String cqlQuery, int maxRecords) {
        int safeMax = Math.min(maxRecords, MAX_ALLOWED_RECORDS);

        return UriComponentsBuilder.fromHttpUrl(SRU_BASE_URL)
                .queryParam("operation", "searchRetrieve")
                .queryParam("version", "1.1")
                .queryParam("recordSyntax", "marcxml")
                .queryParam("maximumRecords", safeMax)
                .queryParam("query", cqlQuery)
                .encode()
                .toUriString();
    }

    private List<String> extractLocCallNumbers(String marcXml) {
        List<String> callNumbers = new ArrayList<>();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(marcXml.getBytes(StandardCharsets.UTF_8));
             MarcXmlReader reader = new MarcXmlReader(bais, false)) {

            while (reader.hasNext()) {
                Record record = reader.next();
                List<VariableField> fields050 = record.getVariableFields("050");

                for (VariableField field : fields050) {
                    if (!(field instanceof DataField dataField)) continue;

                    StringBuilder callNumber = new StringBuilder();

                    dataField.getSubfields('a').forEach(sf -> callNumber.append(sf.getData().trim()).append(" "));
                    dataField.getSubfields('b').forEach(sf -> callNumber.append(sf.getData().trim()));

                    String full = callNumber.toString().trim();
                    if (!full.isEmpty()) {
                        callNumbers.add(full);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse MARCXML response", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse catalog response");
        }

        return callNumbers;
    }
}
