/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores reading-difficulty keys in lowercase. */
@Converter
public class ReadingDifficultyConverter implements AttributeConverter<ReadingDifficulty, String> {
    @Override
    public String convertToDatabaseColumn(ReadingDifficulty value) {
        return value == null ? ReadingDifficulty.UNSET.getKey() : value.getKey();
    }

    @Override
    public ReadingDifficulty convertToEntityAttribute(String value) {
        return ReadingDifficulty.fromString(value);
    }
}
