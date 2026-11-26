/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailingListScheduleTest {

    @Test
    void dailyWeeklyAndMonthlyCronsFireAt4amEastern() {
        CronExpression daily = CronExpression.parse(MailingListSchedule.DAILY_CRON);
        CronExpression weekly = CronExpression.parse(MailingListSchedule.WEEKLY_CRON);
        CronExpression monthly = CronExpression.parse(MailingListSchedule.MONTHLY_CRON);

        ZonedDateTime beforeDaily = ZonedDateTime.of(2026, 11, 25, 3, 59, 0, 0, MailingListWindows.ZONE);
        ZonedDateTime dailyRun = daily.next(beforeDaily);
        assertEquals(ZonedDateTime.of(2026, 11, 25, 4, 0, 0, 0, MailingListWindows.ZONE), dailyRun);
        assertEquals(LocalDate.of(2026, 11, 26), daily.next(dailyRun).toLocalDate());

        ZonedDateTime saturday = ZonedDateTime.of(2026, 2, 28, 12, 0, 0, 0, MailingListWindows.ZONE);
        ZonedDateTime sunday = weekly.next(saturday);
        assertEquals(ZonedDateTime.of(2026, 3, 1, 4, 0, 0, 0, MailingListWindows.ZONE), sunday);
        assertEquals(LocalDate.of(2026, 3, 8), weekly.next(sunday).toLocalDate());

        ZonedDateTime january31 = ZonedDateTime.of(2026, 1, 31, 12, 0, 0, 0, MailingListWindows.ZONE);
        ZonedDateTime february1 = monthly.next(january31);
        assertEquals(ZonedDateTime.of(2026, 2, 1, 4, 0, 0, 0, MailingListWindows.ZONE), february1);
        assertEquals(LocalDate.of(2026, 3, 1), monthly.next(february1).toLocalDate());
    }
}
