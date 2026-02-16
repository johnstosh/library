/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.BookDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FlexibleLocalDateTimeDeserializer to verify it handles both
 * date-only ("2026-01-01") and full datetime ("2026-01-01T14:30:00") strings.
 */
@SpringBootTest
@ActiveProfiles("test")
class FlexibleLocalDateTimeDeserializerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserialize_dateOnlyString_parsesAsStartOfDay() throws Exception {
        String json = "{\"title\":\"Test\",\"dateAddedToLibrary\":\"2026-01-01\"}";

        BookDto dto = objectMapper.readValue(json, BookDto.class);

        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0, 0), dto.getDateAddedToLibrary());
    }

    @Test
    void deserialize_fullDateTimeString_parsesCorrectly() throws Exception {
        String json = "{\"title\":\"Test\",\"dateAddedToLibrary\":\"2026-01-01T14:30:00\"}";

        BookDto dto = objectMapper.readValue(json, BookDto.class);

        assertEquals(LocalDateTime.of(2026, 1, 1, 14, 30, 0), dto.getDateAddedToLibrary());
    }

    @Test
    void deserialize_nullValue_returnsNull() throws Exception {
        String json = "{\"title\":\"Test\",\"dateAddedToLibrary\":null}";

        BookDto dto = objectMapper.readValue(json, BookDto.class);

        assertNull(dto.getDateAddedToLibrary());
    }

    @Test
    void deserialize_missingField_returnsNull() throws Exception {
        String json = "{\"title\":\"Test\"}";

        BookDto dto = objectMapper.readValue(json, BookDto.class);

        assertNull(dto.getDateAddedToLibrary());
    }

    @Test
    void deserialize_dateTimeLocalFormat_withoutSeconds_parsesCorrectly() throws Exception {
        String json = "{\"title\":\"Test\",\"dateAddedToLibrary\":\"2025-01-15T14:30\"}";

        BookDto dto = objectMapper.readValue(json, BookDto.class);

        assertEquals(LocalDateTime.of(2025, 1, 15, 14, 30, 0), dto.getDateAddedToLibrary());
    }

    @Test
    void deserialize_dateTimeWithSeconds_parsesCorrectly() throws Exception {
        String json = "{\"title\":\"Test\",\"dateAddedToLibrary\":\"2025-12-06T09:15:30\"}";

        BookDto dto = objectMapper.readValue(json, BookDto.class);

        assertEquals(LocalDateTime.of(2025, 12, 6, 9, 15, 30), dto.getDateAddedToLibrary());
    }
}
