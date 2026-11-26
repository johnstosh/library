/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.MonthDay;

/**
 * Stores a recurring month and day as a PostgreSQL DATE in leap year 2000
 * so February 29 fits. The year is not a feast year and is not shown.
 */
@Converter
public class FeastMonthDayConverter implements AttributeConverter<MonthDay, LocalDate> {

    static final int SENTINEL_YEAR = 2000;

    @Override
    public LocalDate convertToDatabaseColumn(MonthDay monthDay) {
        if (monthDay == null) {
            return null;
        }
        return monthDay.atYear(SENTINEL_YEAR);
    }

    @Override
    public MonthDay convertToEntityAttribute(LocalDate date) {
        if (date == null) {
            return null;
        }
        return MonthDay.from(date);
    }

    /** A blank explanation is stored as null. One feast date needs no explanation. */
    public static String explanationOrNull(String explanation) {
        if (explanation == null || explanation.isBlank()) {
            return null;
        }
        return explanation;
    }
}
