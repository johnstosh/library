/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class LocCatalogOnlineProvider implements FreeTextProvider {

    private static final String SRU_BASE_URL = "http://lx2.loc.gov:210/LCDB";

    private final RestTemplate restTemplate;

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

            String onlineUrl = extractOnlineResourceUrl(marcXml);
            if (onlineUrl != null) {
                return FreeTextLookupResult.success(getProviderName(), onlineUrl);
            }

            return FreeTextLookupResult.error(getProviderName(), "No online version available");

        } catch (Exception e) {
            log.warn("LOC Catalog search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
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

    private String extractOnlineResourceUrl(String marcXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(marcXml.getBytes(StandardCharsets.UTF_8)));

            // Look for 856 datafields (electronic location/access)
            NodeList datafields = doc.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", "datafield");
            if (datafields.getLength() == 0) {
                datafields = doc.getElementsByTagName("datafield");
            }

            for (int i = 0; i < datafields.getLength(); i++) {
                Element datafield = (Element) datafields.item(i);
                String tag = datafield.getAttribute("tag");

                if ("856".equals(tag)) {
                    // Get subfield u (URI)
                    NodeList subfields = datafield.getElementsByTagNameNS("http://www.loc.gov/MARC21/slim", "subfield");
                    if (subfields.getLength() == 0) {
                        subfields = datafield.getElementsByTagName("subfield");
                    }

                    for (int j = 0; j < subfields.getLength(); j++) {
                        Element subfield = (Element) subfields.item(j);
                        String code = subfield.getAttribute("code");
                        if ("u".equals(code)) {
                            String url = subfield.getTextContent().trim();
                            if (!url.isEmpty() && url.startsWith("http")) {
                                return url;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Failed to parse MARC XML for online resources: {}", e.getMessage());
        }

        return null;
    }
}
