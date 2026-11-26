/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

/**
 * When scheduled mailing-list issues go out. The clock is America/New_York.
 * Signup does not wait for these times; it sends the previous period at once.
 */
public final class MailingListSchedule {

    public static final String ZONE = "America/New_York";

    public static final String DAILY_CRON = "0 0 4 * * *";

    public static final String WEEKLY_CRON = "0 0 4 * * SUN";

    public static final String MONTHLY_CRON = "0 0 4 1 * *";

    private MailingListSchedule() {
    }
}
