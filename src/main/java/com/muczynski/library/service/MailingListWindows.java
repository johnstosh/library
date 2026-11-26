/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.MailingFrequency;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * Content windows for mailing-list issues. Dates are Eastern calendar days.
 * {@code Book.dateAddedToLibrary} is stored as a UTC local date-time.
 */
public final class MailingListWindows {

    public static final ZoneId ZONE = ZoneId.of(MailingListSchedule.ZONE);

    public enum SendOrigin {
        SCHEDULED,
        SIGNUP
    }

    public record DayWindow(LocalDate startInclusive, LocalDate endInclusive) {
    }

    public record InstantWindow(LocalDateTime startUtcInclusive, LocalDateTime endUtcExclusive) {
    }

    private MailingListWindows() {
    }

    public static LocalDate easternDate(java.time.ZonedDateTime moment) {
        return moment.withZoneSameInstant(ZONE).toLocalDate();
    }

    /**
     * Lives of Saints. Scheduled daily is tomorrow; signup daily is yesterday.
     * Scheduled weekly is that Sunday through Saturday. Signup weekly is the
     * most recently completed Sunday–Saturday week. Scheduled monthly is the
     * run's calendar month. Signup monthly is the previous calendar month.
     */
    public static DayWindow saintDays(MailingFrequency frequency, SendOrigin origin, LocalDate easternToday) {
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(easternToday, "easternToday");
        return switch (frequency) {
            case DAILY -> {
                LocalDate day = origin == SendOrigin.SCHEDULED
                        ? easternToday.plusDays(1)
                        : easternToday.minusDays(1);
                yield new DayWindow(day, day);
            }
            case WEEKLY -> {
                if (origin == SendOrigin.SCHEDULED) {
                    LocalDate sunday = easternToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                    yield new DayWindow(sunday, sunday.plusDays(6));
                }
                LocalDate saturday = easternToday.with(TemporalAdjusters.previous(DayOfWeek.SATURDAY));
                yield new DayWindow(saturday.minusDays(6), saturday);
            }
            case MONTHLY -> {
                LocalDate month = origin == SendOrigin.SCHEDULED
                        ? easternToday
                        : easternToday.minusMonths(1);
                LocalDate start = month.withDayOfMonth(1);
                yield new DayWindow(start, start.withDayOfMonth(start.lengthOfMonth()));
            }
        };
    }

    /**
     * New Catalog Entries. Scheduled and signup use the same window:
     * yesterday, the previous seven Eastern days not including today,
     * or the previous Eastern calendar month.
     */
    public static InstantWindow catalogAdded(MailingFrequency frequency, SendOrigin origin, LocalDate easternToday) {
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(easternToday, "easternToday");
        LocalDate startDate;
        LocalDate endExclusive;
        switch (frequency) {
            case DAILY -> {
                startDate = easternToday.minusDays(1);
                endExclusive = easternToday;
            }
            case WEEKLY -> {
                startDate = easternToday.minusDays(7);
                endExclusive = easternToday;
            }
            case MONTHLY -> {
                startDate = easternToday.minusMonths(1).withDayOfMonth(1);
                endExclusive = YearMonth.from(startDate).plusMonths(1).atDay(1);
            }
            default -> throw new IllegalStateException("Unknown frequency " + frequency);
        }
        return new InstantWindow(toUtc(startDate), toUtc(endExclusive));
    }

    /**
     * True when this feast falls on a calendar day in the window.
     * A February issue also includes February 29 in a non-leap year, because
     * the feast is a month-day stored with sentinel year 2000.
     */
    public static boolean feastInWindow(MonthDay feast, DayWindow window) {
        if (feast == null || window == null) {
            return false;
        }
        LocalDate cursor = window.startInclusive();
        while (!cursor.isAfter(window.endInclusive())) {
            if (MonthDay.from(cursor).equals(feast)) {
                return true;
            }
            cursor = cursor.plusDays(1);
        }
        return feast.equals(MonthDay.of(Month.FEBRUARY, 29)) && wholeFebruary(window);
    }

    /**
     * Idempotency key for the Eastern scheduled slot that contains this day.
     * Daily is that date, weekly is that week's Sunday, monthly is that month.
     */
    public static String occurrenceKey(MailingFrequency frequency, LocalDate easternToday) {
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(easternToday, "easternToday");
        return switch (frequency) {
            case DAILY -> "DAILY:" + easternToday;
            case WEEKLY -> "WEEKLY:" + easternToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            case MONTHLY -> "MONTHLY:" + YearMonth.from(easternToday);
        };
    }

    private static boolean wholeFebruary(DayWindow window) {
        LocalDate start = window.startInclusive();
        LocalDate end = window.endInclusive();
        if (start.getMonth() != Month.FEBRUARY || start.getDayOfMonth() != 1) {
            return false;
        }
        if (end.getYear() != start.getYear() || end.getMonth() != Month.FEBRUARY) {
            return false;
        }
        return end.getDayOfMonth() == end.lengthOfMonth();
    }

    private static LocalDateTime toUtc(LocalDate easternDay) {
        return easternDay.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
