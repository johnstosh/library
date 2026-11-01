/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
import com.muczynski.library.freetext.TitleMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
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

/**
 * Provider for Library of Congress Online Catalog.
 * Uses the SRU API to search for books and check if they have online resources (MARC field 856).
 */
@Component
@Slf4j
public class LocCatalogOnlineProvider implements FreeTextProvider {

    private static final String SRU_BASE_URL = "http://lx2.loc.gov:210/LCDB";

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "LOC Online Catalog";
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String cqlQuery = buildCqlQuery(title, authorName);
            String url = buildSruUrl(cqlQuery);

            String marcXml = restTemplate.getForObject(url, String.class);
            if (marcXml == null || marcXml.trim().isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from catalog");
            }

            String onlineUrl = extractOnlineResourceUrl(marcXml, title, authorName);
            if (onlineUrl != null) {
                return FreeTextLookupResult.success(getProviderName(), onlineUrl);
            }

            return FreeTextLookupResult.error(getProviderName(), "No online version available");

        } catch (Exception e) {
            // Get root cause for better error messages (e.g., SocketTimeoutException)
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("LOC Catalog search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
        }
    }

    private String buildCqlQuery(String title, String authorName) {
        List<String> conditions = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            conditions.add("dc.title=\"" + escapeCql(title) + "\"");
        }

        if (authorName != null && !authorName.isBlank()) {
            conditions.add("dc.creator=\"" + escapeCql(authorName) + "\"");
        }

        return String.join(" AND ", conditions);
    }

    private String escapeCql(String input) {
        return input.replace("\"", "\\\"");
    }

    private String buildSruUrl(String cqlQuery) {
        return UriComponentsBuilder.fromHttpUrl(SRU_BASE_URL)
                .queryParam("operation", "searchRetrieve")
                .queryParam("version", "1.1")
                .queryParam("recordSchema", "marcxml")
                .queryParam("maximumRecords", 5)
                .queryParam("query", cqlQuery)
                .build()
                .toUriString();
    }

    /**
     * Return an 856 full-text URL from a record whose title and author are the
     * book we asked for. The first 856 in a multi-record response is often a
     * different work that shared a word of the query.
     */
    private String extractOnlineResourceUrl(String marcXml, String title, String authorName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(marcXml.getBytes(StandardCharsets.UTF_8)));

            NodeList records = elements(doc, "record");
            for (int r = 0; r < records.getLength(); r++) {
                Element record = (Element) records.item(r);
                if (!(record instanceof Element)) {
                    continue;
                }
                String url = urlFromMatchingRecord((Element) record, title, authorName);
                if (url != null) {
                    return url;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse MARC XML for online resources: {}", e.getMessage());
        }
        return null;
    }

    private String urlFromMatchingRecord(Element record, String title, String authorName) {
        String recordTitle = (fieldText(record, "245", "a") + " " + fieldText(record, "245", "b")).trim();
        if (recordTitle.isBlank() || !TitleMatcher.titleMatches(recordTitle, title)) {
            return null;
        }
        List<String> authors = new ArrayList<>();
        for (String tag : List.of("100", "110", "700", "710")) {
            authors.addAll(fieldTexts(record, tag, "a"));
        }
        if (authorName != null && !authorName.isBlank()) {
            if (authors.isEmpty()) {
                if (!TitleMatcher.sameTitle(recordTitle, title)) {
                    return null;
                }
            } else if (authors.stream().noneMatch(name -> TitleMatcher.authorMatches(name, authorName))) {
                return null;
            }
        }
        return firstFullTextUrl(record);
    }

    private String firstFullTextUrl(Element record) {
        NodeList datafields = elements(record, "datafield");
        for (int i = 0; i < datafields.getLength(); i++) {
            if (!(datafields.item(i) instanceof Element datafield)) {
                continue;
            }
            if (!"856".equals(datafield.getAttribute("tag"))) {
                continue;
            }
            // Indicator 2 means "Related resource" (catalogs, biographies), not the text.
            if ("2".equals(datafield.getAttribute("ind2"))) {
                continue;
            }
            String url = null;
            String publicNote = null;
            NodeList subfields = elements(datafield, "subfield");
            for (int j = 0; j < subfields.getLength(); j++) {
                if (!(subfields.item(j) instanceof Element subfield)) {
                    continue;
                }
                String code = subfield.getAttribute("code");
                if ("u".equals(code)) {
                    url = subfield.getTextContent().trim();
                } else if ("z".equals(code) || "3".equals(code)) {
                    publicNote = subfield.getTextContent().trim().toLowerCase();
                }
            }
            if (url == null || url.isEmpty() || !url.startsWith("http")) {
                continue;
            }
            String urlLower = url.toLowerCase();
            if (urlLower.contains("/catdir/") || urlLower.contains("/bios/")
                    || urlLower.contains("/toc/") || urlLower.contains("/sample/")
                    || urlLower.contains("/cover/")
                    || urlLower.contains("contributor biographical")) {
                continue;
            }
            if (publicNote != null && (publicNote.contains("biography")
                    || publicNote.contains("table of contents")
                    || publicNote.contains("sample")
                    || publicNote.contains("cover")
                    || publicNote.contains("publisher description"))) {
                continue;
            }
            return url;
        }
        return null;
    }

    private String fieldText(Element record, String tag, String code) {
        List<String> texts = fieldTexts(record, tag, code);
        return texts.isEmpty() ? "" : texts.get(0);
    }

    private List<String> fieldTexts(Element record, String tag, String code) {
        List<String> texts = new ArrayList<>();
        NodeList datafields = elements(record, "datafield");
        for (int i = 0; i < datafields.getLength(); i++) {
            if (!(datafields.item(i) instanceof Element datafield)) {
                continue;
            }
            if (!tag.equals(datafield.getAttribute("tag"))) {
                continue;
            }
            // Only this record's own fields. A parent SRU record also contains the MARC record.
            if (datafield.getParentNode() != record) {
                continue;
            }
            NodeList subfields = elements(datafield, "subfield");
            for (int j = 0; j < subfields.getLength(); j++) {
                if (!(subfields.item(j) instanceof Element subfield)) {
                    continue;
                }
                if (code.equals(subfield.getAttribute("code"))) {
                    String text = subfield.getTextContent().trim();
                    if (!text.isEmpty()) {
                        texts.add(text);
                    }
                }
            }
        }
        return texts;
    }

    private NodeList elements(org.w3c.dom.Node parent, String localName) {
        if (parent instanceof Document document) {
            NodeList namespaced = document.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", localName);
            if (namespaced.getLength() > 0) {
                return namespaced;
            }
            return document.getElementsByTagName(localName);
        }
        Element element = (Element) parent;
        NodeList namespaced = element.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", localName);
        if (namespaced.getLength() > 0) {
            return namespaced;
        }
        return element.getElementsByTagName(localName);
    }
}
