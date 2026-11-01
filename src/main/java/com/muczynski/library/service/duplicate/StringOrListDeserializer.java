/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a Duplicate Titles snapshot field that used to be one string and is now a list.
 * An old {@code fixBookAAlternate}, {@code fixBookBAlternate}, or {@code alternateTitle}
 * string becomes one entry. The current {@code alternateTitles} field must be a list.
 */
public class StringOrListDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String field = parser.currentName();
        JsonNode node = parser.readValueAsTree();
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            if ("alternateTitles".equals(field)) {
                throw JsonMappingException.from(parser, "Alternate titles must be a list of strings");
            }
            String value = node.asText();
            if (value == null || value.isBlank()) {
                return null;
            }
            return List.of(value);
        }
        if (!node.isArray()) {
            throw JsonMappingException.from(parser, "Alternate titles must be a string or a list of strings");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) {
                continue;
            }
            if (!item.isTextual()) {
                throw JsonMappingException.from(parser, "Alternate title must be a string");
            }
            values.add(item.asText());
        }
        return values;
    }
}
