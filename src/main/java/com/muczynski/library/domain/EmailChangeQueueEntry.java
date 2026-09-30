/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One pending email for a loan or an application. {@code beforeJson} stays as
 * it was at the start of the quiet period. {@code afterJson} is replaced on
 * every later edit. When the two match, the row is deleted and nothing is sent.
 */
@Entity
@Table(
        name = "email_change_queue",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_change_queue_subject", columnNames = "subject_key"),
        indexes = @Index(name = "idx_email_change_queue_send_after", columnList = "send_after")
)
@Getter
@Setter
public class EmailChangeQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_key", nullable = false, length = 120)
    private String subjectKey;

    @Column(nullable = false, length = 40)
    private String kind;

    @Column(columnDefinition = "text")
    private String beforeJson;

    @Column(columnDefinition = "text")
    private String afterJson;

    @Column(name = "send_after", nullable = false)
    private Instant sendAfter;
}
