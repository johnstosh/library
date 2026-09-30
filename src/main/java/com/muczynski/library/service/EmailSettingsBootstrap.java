/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * On first startup, when no SMTP password is saved yet, copy Gmail SMTP
 * settings from the environment so the deploy is ready to send.
 */
@Component
@ConditionalOnProperty(name = "app.email.seed-from-env", havingValue = "true", matchIfMissing = true)
public class EmailSettingsBootstrap implements ApplicationRunner {

    private final GlobalSettingsService globalSettingsService;

    public EmailSettingsBootstrap(GlobalSettingsService globalSettingsService) {
        this.globalSettingsService = globalSettingsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        globalSettingsService.seedSmtpFromEnvironmentIfUnset();
    }
}
