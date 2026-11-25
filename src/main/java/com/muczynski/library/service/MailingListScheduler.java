/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.MailingFrequency;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Eastern 4am sends. Daily is every day, weekly is Sunday, monthly is the 1st.
 */
@Component
public class MailingListScheduler {

    private final MailingListService mailingListService;

    public MailingListScheduler(MailingListService mailingListService) {
        this.mailingListService = mailingListService;
    }

    @Scheduled(cron = MailingListSchedule.DAILY_CRON, zone = MailingListSchedule.ZONE)
    public void sendDaily() {
        mailingListService.sendScheduled(MailingFrequency.DAILY);
    }

    @Scheduled(cron = MailingListSchedule.WEEKLY_CRON, zone = MailingListSchedule.ZONE)
    public void sendWeekly() {
        mailingListService.sendScheduled(MailingFrequency.WEEKLY);
    }

    @Scheduled(cron = MailingListSchedule.MONTHLY_CRON, zone = MailingListSchedule.ZONE)
    public void sendMonthly() {
        mailingListService.sendScheduled(MailingFrequency.MONTHLY);
    }
}
