/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One patron email on one list at one frequency.
 * The unsubscribe token is an unguessable secret. Do not log it.
 */
@Entity
@Table(
        name = "mailing_list_subscription",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mailing_list_email_topic", columnNames = {"email", "topic"})
        },
        indexes = {
                @Index(name = "idx_mailing_list_active_frequency", columnList = "active, frequency")
        }
)
@Getter
@Setter
public class MailingListSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MailingListTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MailingFrequency frequency;

    @Column(name = "unsubscribe_token", nullable = false, unique = true, length = 64)
    private String unsubscribeToken;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime subscribedAt;

    private LocalDateTime unsubscribedAt;

    /**
     * Eastern scheduled occurrence already delivered, for example
     * {@code DAILY:2026-11-25} or {@code WEEKLY:2026-11-22}.
     * A signup on that occurrence counts, so 4am does not send it again.
     */
    @Column(name = "last_occurrence_key", length = 40)
    private String lastOccurrenceKey;
}
