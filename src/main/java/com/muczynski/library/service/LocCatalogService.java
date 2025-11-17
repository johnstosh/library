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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
 * http://lx2.loc.gov:210/LCDB</p>
 *
 * <p>Note: Port 210 is the Z39.50/SRU port and uses HTTP, not HTTPS</p>
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

    private static final String SRU_BASE_URL = "http://lx2.loc.gov:210/LCDB";
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

        // Use quotes for multi-word phrases
        Optional.ofNullable(request.getTitle())
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .ifPresent(t -> conditions.add("dc.title=\"" + escapeCql(t) + "\""));

        Optional.ofNullable(request.getAuthor())
                .map(String::trim)
                .filter(a -> !a.isEmpty())
                .ifPresent(a -> conditions.add("dc.creator=\"" + escapeCql(a) + "\""));

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
                .queryParam("recordSchema", "marcxml")  // Use recordSchema instead of recordSyntax
                .queryParam("maximumRecords", safeMax)
                .queryParam("query", cqlQuery)
                .build()  // Don't call .encode() to avoid double-encoding
                .toUriString();
    }

    private List<String> extractLocCallNumbers(String sruResponse) {
        List<String> callNumbers = new ArrayList<>();

        try {
            log.debug("Parsing SRU response (first 500 chars): {}",
                sruResponse.substring(0, Math.min(500, sruResponse.length())));

            // First parse the SRU wrapper
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(sruResponse.getBytes(StandardCharsets.UTF_8)));

            // Check number of records
            NodeList numRecordsNodes = doc.getElementsByTagNameNS("http://www.loc.gov/zing/srw/", "numberOfRecords");
            if (numRecordsNodes.getLength() > 0) {
                int numRecords = Integer.parseInt(numRecordsNodes.item(0).getTextContent());
                log.debug("SRU response contains {} records", numRecords);

                if (numRecords == 0) {
                    return callNumbers; // Empty list
                }
            }

            // Extract MARC records from within the SRU response
            NodeList recordDataNodes = doc.getElementsByTagNameNS("http://www.loc.gov/zing/srw/", "recordData");

            for (int i = 0; i < recordDataNodes.getLength(); i++) {
                Element recordDataElement = (Element) recordDataNodes.item(i);

                // Get the MARC record (should be a <record> element)
                NodeList marcRecords = recordDataElement.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", "record");

                if (marcRecords.getLength() == 0) {
                    // Try without namespace
                    marcRecords = recordDataElement.getElementsByTagName("record");
                }

                if (marcRecords.getLength() > 0) {
                    // Extract call numbers from this MARC record
                    extractCallNumbersFromMarcElement((Element) marcRecords.item(0), callNumbers);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse SRU response. First 1000 chars of response: {}",
                sruResponse.substring(0, Math.min(1000, sruResponse.length())), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse catalog response");
        }

        return callNumbers;
    }

    private void extractCallNumbersFromMarcElement(Element recordElement, List<String> callNumbers) {
        // Look for 050 datafields (LOC call number)
        NodeList datafields = recordElement.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", "datafield");

        if (datafields.getLength() == 0) {
            datafields = recordElement.getElementsByTagName("datafield");
        }

        for (int i = 0; i < datafields.getLength(); i++) {
            Element datafield = (Element) datafields.item(i);
            String tag = datafield.getAttribute("tag");

            if ("050".equals(tag)) {
                StringBuilder callNumber = new StringBuilder();

                // Get subfields
                NodeList subfields = datafield.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", "subfield");
                if (subfields.getLength() == 0) {
                    subfields = datafield.getElementsByTagName("subfield");
                }

                for (int j = 0; j < subfields.getLength(); j++) {
                    Element subfield = (Element) subfields.item(j);
                    String code = subfield.getAttribute("code");
                    String data = subfield.getTextContent().trim();

                    if ("a".equals(code)) {
                        callNumber.append(data).append(" ");
                    } else if ("b".equals(code)) {
                        callNumber.append(data);
                    }
                }

                String full = callNumber.toString().trim();
                if (!full.isEmpty()) {
                    callNumbers.add(full);
                }
            }
        }
    }
}
