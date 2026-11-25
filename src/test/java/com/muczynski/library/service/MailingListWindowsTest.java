/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.MailingFrequency;
import com.muczynski.library.service.MailingListWindows.DayWindow;
import com.muczynski.library.service.MailingListWindows.InstantWindow;
import com.muczynski.library.service.MailingListWindows.SendOrigin;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailingListWindowsTest {

    private static final MonthDay FEB_28 = MonthDay.of(2, 28);
    private static final MonthDay FEB_29 = MonthDay.of(2, 29);
    private static final MonthDay MAR_1 = MonthDay.of(3, 1);
    private static final MonthDay DEC_25 = MonthDay.of(12, 25);

    @Test
    void dailySaintsTomorrowWhenScheduledAndYesterdayWhenSigningUp() {
        LocalDate wednesday = LocalDate.of(2026, 11, 25);

        DayWindow scheduled = MailingListWindows.saintDays(MailingFrequency.DAILY, SendOrigin.SCHEDULED, wednesday);
        DayWindow signup = MailingListWindows.saintDays(MailingFrequency.DAILY, SendOrigin.SIGNUP, wednesday);

        assertEquals(new DayWindow(LocalDate.of(2026, 11, 26), LocalDate.of(2026, 11, 26)), scheduled);
        assertEquals(new DayWindow(LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 24)), signup);
    }

    @Test
    void scheduledWeeklyOnSundayRunsThroughSaturday() {
        LocalDate sunday = LocalDate.of(2026, 3, 1);

        DayWindow week = MailingListWindows.saintDays(MailingFrequency.WEEKLY, SendOrigin.SCHEDULED, sunday);

        assertEquals(LocalDate.of(2026, 3, 1), week.startInclusive());
        assertEquals(LocalDate.of(2026, 3, 7), week.endInclusive());
    }

    @Test
    void signupWeeklyOnSundayUsesTheCompletedWeekAcrossTheMonthBoundary() {
        LocalDate sunday = LocalDate.of(2026, 3, 1);

        DayWindow completed = MailingListWindows.saintDays(MailingFrequency.WEEKLY, SendOrigin.SIGNUP, sunday);

        assertEquals(LocalDate.of(2026, 2, 22), completed.startInclusive());
        assertEquals(LocalDate.of(2026, 2, 28), completed.endInclusive());
        assertFalse(MailingListWindows.feastInWindow(FEB_29, completed));
        assertTrue(MailingListWindows.feastInWindow(FEB_28, completed));
    }

    @Test
    void scheduledWeeklyCrossingIntoDecemberIncludesOnlyThatSundayThroughSaturday() {
        LocalDate sunday = LocalDate.of(2026, 11, 29);

        DayWindow week = MailingListWindows.saintDays(MailingFrequency.WEEKLY, SendOrigin.SCHEDULED, sunday);

        assertEquals(LocalDate.of(2026, 11, 29), week.startInclusive());
        assertEquals(LocalDate.of(2026, 12, 5), week.endInclusive());
        assertTrue(MailingListWindows.feastInWindow(MonthDay.of(12, 1), week));
        assertFalse(MailingListWindows.feastInWindow(MonthDay.of(11, 28), week));
    }

    @Test
    void monthlySaintsAreThisMonthWhenScheduledAndThePreviousMonthWhenSigningUp() {
        LocalDate newYearsDay = LocalDate.of(2026, 1, 1);

        DayWindow scheduled = MailingListWindows.saintDays(MailingFrequency.MONTHLY, SendOrigin.SCHEDULED, newYearsDay);
        DayWindow signup = MailingListWindows.saintDays(MailingFrequency.MONTHLY, SendOrigin.SIGNUP, newYearsDay);

        assertEquals(new DayWindow(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)), scheduled);
        assertEquals(new DayWindow(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)), signup);
        assertTrue(MailingListWindows.feastInWindow(DEC_25, signup));
        assertFalse(MailingListWindows.feastInWindow(MonthDay.of(1, 1), signup));
    }

    @Test
    void februaryFeastDayIsIncludedForTheWholeMonthEvenWhenTheYearIsNotALeapYear() {
        DayWindow scheduled = MailingListWindows.saintDays(
                MailingFrequency.MONTHLY, SendOrigin.SCHEDULED, LocalDate.of(2026, 2, 1));
        DayWindow signup = MailingListWindows.saintDays(
                MailingFrequency.MONTHLY, SendOrigin.SIGNUP, LocalDate.of(2026, 3, 1));

        assertEquals(scheduled, signup);
        assertEquals(LocalDate.of(2026, 2, 28), scheduled.endInclusive());
        assertTrue(MailingListWindows.feastInWindow(FEB_29, scheduled));
        assertTrue(MailingListWindows.feastInWindow(FEB_28, scheduled));
        assertFalse(MailingListWindows.feastInWindow(MAR_1, scheduled));
    }

    @Test
    void february29IsInTheLeapYearWeekThatContainsItAndNotInThePriorWeek() {
        LocalDate leapSunday = LocalDate.of(2028, 2, 27);

        DayWindow scheduled = MailingListWindows.saintDays(MailingFrequency.WEEKLY, SendOrigin.SCHEDULED, leapSunday);
        DayWindow signupThatSunday = MailingListWindows.saintDays(MailingFrequency.WEEKLY, SendOrigin.SIGNUP, leapSunday);
        DayWindow signupAfter = MailingListWindows.saintDays(
                MailingFrequency.WEEKLY, SendOrigin.SIGNUP, LocalDate.of(2028, 3, 5));

        assertEquals(new DayWindow(LocalDate.of(2028, 2, 27), LocalDate.of(2028, 3, 4)), scheduled);
        assertTrue(MailingListWindows.feastInWindow(FEB_29, scheduled));
        assertFalse(MailingListWindows.feastInWindow(FEB_29, signupThatSunday));
        assertEquals(scheduled, signupAfter);
        assertTrue(MailingListWindows.feastInWindow(FEB_29, signupAfter));
    }

    @Test
    void dailyFebruary29FollowsTheCalendarDay() {
        DayWindow scheduled = MailingListWindows.saintDays(
                MailingFrequency.DAILY, SendOrigin.SCHEDULED, LocalDate.of(2028, 2, 28));
        DayWindow signup = MailingListWindows.saintDays(
                MailingFrequency.DAILY, SendOrigin.SIGNUP, LocalDate.of(2028, 3, 1));
        DayWindow nonLeapSignup = MailingListWindows.saintDays(
                MailingFrequency.DAILY, SendOrigin.SIGNUP, LocalDate.of(2026, 3, 1));

        assertTrue(MailingListWindows.feastInWindow(FEB_29, scheduled));
        assertTrue(MailingListWindows.feastInWindow(FEB_29, signup));
        assertEquals(LocalDate.of(2026, 2, 28), nonLeapSignup.startInclusive());
        assertFalse(MailingListWindows.feastInWindow(FEB_29, nonLeapSignup));
    }

    @Test
    void catalogWindowsAreTheSameForScheduledAndSignup() {
        LocalDate sunday = LocalDate.of(2026, 11, 1);
        for (MailingFrequency frequency : MailingFrequency.values()) {
            assertEquals(
                    MailingListWindows.catalogAdded(frequency, SendOrigin.SCHEDULED, sunday),
                    MailingListWindows.catalogAdded(frequency, SendOrigin.SIGNUP, sunday),
                    frequency.name());
        }
    }

    @Test
    void catalogDailyIsYesterdayAndWeeklyOnTheFirstReachesIntoThePreviousMonth() {
        LocalDate sunday = LocalDate.of(2026, 11, 1);

        assertEasternDays(MailingListWindows.catalogAdded(MailingFrequency.DAILY, SendOrigin.SCHEDULED, sunday),
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1));
        assertEasternDays(MailingListWindows.catalogAdded(MailingFrequency.WEEKLY, SendOrigin.SIGNUP, sunday),
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 1));
    }

    @Test
    void catalogMonthlyIsThePreviousEasternMonthIncludingANonLeapFebruary() {
        InstantWindow february = MailingListWindows.catalogAdded(
                MailingFrequency.MONTHLY, SendOrigin.SCHEDULED, LocalDate.of(2026, 3, 1));
        InstantWindow december = MailingListWindows.catalogAdded(
                MailingFrequency.MONTHLY, SendOrigin.SIGNUP, LocalDate.of(2026, 1, 15));

        assertEasternDays(february, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));
        assertEasternDays(december, LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 1));
    }

    @Test
    void catalogWindowsCanBeBuiltOnDaylightSavingBoundaries() {
        assertEasternDays(MailingListWindows.catalogAdded(
                        MailingFrequency.DAILY, SendOrigin.SIGNUP, LocalDate.of(2026, 3, 8)),
                LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 8));
        assertEasternDays(MailingListWindows.catalogAdded(
                        MailingFrequency.DAILY, SendOrigin.SCHEDULED, LocalDate.of(2026, 11, 1)),
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1));
    }

    @Test
    void occurrenceKeyUsesTheEasternDayWeekOrMonth() {
        LocalDate wednesday = LocalDate.of(2026, 11, 25);
        LocalDate sunday = LocalDate.of(2026, 11, 29);

        assertEquals("DAILY:2026-11-25", MailingListWindows.occurrenceKey(MailingFrequency.DAILY, wednesday));
        assertEquals("WEEKLY:2026-11-22", MailingListWindows.occurrenceKey(MailingFrequency.WEEKLY, wednesday));
        assertEquals("WEEKLY:2026-11-29", MailingListWindows.occurrenceKey(MailingFrequency.WEEKLY, sunday));
        assertEquals("MONTHLY:2026-11", MailingListWindows.occurrenceKey(MailingFrequency.MONTHLY, wednesday));
        assertEquals("MONTHLY:2026-11", MailingListWindows.occurrenceKey(MailingFrequency.MONTHLY, LocalDate.of(2026, 11, 1)));
    }

    private static void assertEasternDays(InstantWindow window, LocalDate start, LocalDate endExclusive) {
        ZonedDateTime startEastern = window.startUtcInclusive().atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(MailingListWindows.ZONE);
        ZonedDateTime endEastern = window.endUtcExclusive().atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(MailingListWindows.ZONE);
        assertEquals(start, startEastern.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, startEastern.toLocalTime());
        assertEquals(endExclusive, endEastern.toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, endEastern.toLocalTime());
    }
}
