/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.muczynski.library.domain.PublicLibrary;
import com.muczynski.library.domain.RejectedHolding;
import com.muczynski.library.domain.RejectedMatches;
import com.muczynski.library.dto.CatalogMatchRef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Stable ids from the catalog payloads this app already parses.
 * A record with no id cannot be rejected and is still used.
 */
public final class CatalogMatchKeys {

    private CatalogMatchKeys() {
    }

    public static String ydlKey(JsonNode entry) {
        String id = text(entry.path("id"));
        if (!id.isEmpty()) {
            return id;
        }
        return text(entry.path("entity").path("id"));
    }

    /** Primo {@code pnx.control.recordid}, then {@code sourcerecordid}. */
    public static String primoKey(JsonNode entry) {
        JsonNode control = entry.path("pnx").path("control");
        String id = text(control.path("recordid"));
        if (!id.isEmpty()) {
            return id;
        }
        return text(control.path("sourcerecordid"));
    }

    public static String msuKey(JsonNode record) {
        return text(record.path("id"));
    }

    /**
     * BiblioCommons bib id. {@code filterMatches} copies the object key onto
     * {@code _recordKey} because walking {@code elements()} drops it.
     */
    public static String aclaKey(JsonNode entry) {
        String key = text(entry.path("_recordKey"));
        if (!key.isEmpty()) {
            return key;
        }
        return text(entry.path("id"));
    }

    public static List<CatalogMatchRef> keep(ArrayNode destination, JsonNode matches,
                                              List<RejectedHolding> rejected, PublicLibrary library,
                                              Function<JsonNode, String> keyOf) {
        List<CatalogMatchRef> refs = new ArrayList<>();
        if (matches == null || !matches.isArray()) {
            return refs;
        }
        for (JsonNode entry : matches) {
            String key = keyOf.apply(entry);
            if (key != null && !key.isBlank() && RejectedMatches.rejectsHolding(rejected, library, key)) {
                continue;
            }
            destination.add(entry);
            if (key != null && !key.isBlank()) {
                refs.add(new CatalogMatchRef(key, null));
            }
        }
        return refs;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isArray()) {
            return node.isEmpty() ? "" : text(node.get(0));
        }
        String value = node.asText("");
        return value == null ? "" : value.trim();
    }
}
