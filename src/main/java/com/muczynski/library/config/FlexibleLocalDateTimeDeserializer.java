/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Custom deserializer that handles both date-only strings ("2026-01-01")
 * and full ISO datetime strings ("2026-01-01T00:00:00") for LocalDateTime fields.
 * HTML date inputs send date-only strings which the default JavaTimeModule cannot
 * parse as LocalDateTime.
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().trim();
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDate.parse(value).atStartOfDay();
    }
}
