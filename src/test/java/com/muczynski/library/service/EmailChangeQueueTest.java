/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.muczynski.library.domain.EmailChangeQueueEntry;
import com.muczynski.library.email.EmailChangeHandler;
import com.muczynski.library.email.EmailChangeKinds;
import com.muczynski.library.email.EmailSendException;
import com.muczynski.library.email.LoanMailSnapshot;
import com.muczynski.library.repository.EmailChangeQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EmailChangeQueueTest {

    @Mock
    private EmailChangeQueueRepository repository;

    private final Map<String, EmailChangeQueueEntry> rows = new HashMap<>();
    private final List<String> sent = new ArrayList<>();
    private Instant now;
    private EmailChangeQueue queue;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-09-30T12:00:00Z");
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        EmailChangeHandler handler = new EmailChangeHandler() {
            @Override
            public String kind() {
                return EmailChangeKinds.LOAN;
            }

            @Override
            public void send(String beforeJson, String afterJson) {
                if (afterJson != null && afterJson.contains("\"bookTitle\":\"Fail\"")) {
                    throw new EmailSendException("smtp down");
                }
                sent.add(beforeJson + " => " + afterJson);
            }
        };
        queue = new EmailChangeQueue(repository, mapper, List.of(handler), Duration.ofMinutes(5),
                Clock.fixed(now, ZoneOffset.UTC));

        lenient().when(repository.findBySubjectKey(any())).thenAnswer(invocation ->
                Optional.ofNullable(rows.get(invocation.getArgument(0))));
        lenient().when(repository.save(any())).thenAnswer(invocation -> {
            EmailChangeQueueEntry entry = invocation.getArgument(0);
            rows.put(entry.getSubjectKey(), entry);
            return entry;
        });
        lenient().doAnswer(invocation -> {
            EmailChangeQueueEntry entry = invocation.getArgument(0);
            rows.remove(entry.getSubjectKey());
            return null;
        }).when(repository).delete(any());
        lenient().when(repository.findBySendAfterLessThanEqualOrderBySendAfterAsc(any())).thenAnswer(invocation -> {
            Instant deadline = invocation.getArgument(0);
            return rows.values().stream()
                    .filter(row -> !row.getSendAfter().isAfter(deadline))
                    .toList();
        });
    }

    @Test
    void laterEditKeepsOriginalBeforeAndRestartsTheWait() {
        LoanMailSnapshot created = loan("Out to the user", LocalDate.of(2026, 10, 1));
        LoanMailSnapshot dueMoved = loan("Out to the user", LocalDate.of(2026, 10, 8));

        queue.stage("loan:1", EmailChangeKinds.LOAN, null, created);
        queue.stage("loan:1", EmailChangeKinds.LOAN, created, dueMoved);

        EmailChangeQueueEntry row = rows.get("loan:1");
        assertNotNull(row);
        assertTrue(row.getBeforeJson().contains("null") || row.getBeforeJson().equals("null"));
        assertTrue(row.getAfterJson().contains("2026-10-08"));
        assertEquals(now.plus(Duration.ofMinutes(5)), row.getSendAfter());
        assertTrue(sent.isEmpty());
    }

    @Test
    void undoneChangeSendsNothing() {
        LoanMailSnapshot created = loan("Out to the user", LocalDate.of(2026, 10, 1));
        queue.stage("loan:1", EmailChangeKinds.LOAN, null, created);
        queue.stage("loan:1", EmailChangeKinds.LOAN, created, null);

        assertTrue(rows.isEmpty());
        queue.flushDue();
        assertTrue(sent.isEmpty());
    }

    @Test
    void flushSendsOnceTheQuietPeriodHasPassed() {
        LoanMailSnapshot created = loan("Out to the user", LocalDate.of(2026, 10, 1));
        queue.stage("loan:1", EmailChangeKinds.LOAN, null, created);
        rows.get("loan:1").setSendAfter(now.minusSeconds(1));

        queue.flushDue();

        assertEquals(1, sent.size());
        assertTrue(sent.get(0).contains("Out to the user"));
        assertTrue(rows.isEmpty());
    }

    @Test
    void deliveryFailureLeavesTheRowForAnotherTry() {
        LoanMailSnapshot created = loan("Fail", LocalDate.of(2026, 10, 1));
        queue.stage("loan:1", EmailChangeKinds.LOAN, null, created);
        rows.get("loan:1").setSendAfter(now.minusSeconds(1));

        queue.flushDue();

        assertTrue(sent.isEmpty());
        assertNotNull(rows.get("loan:1"));
    }

    private static LoanMailSnapshot loan(String title, LocalDate due) {
        return new LoanMailSnapshot(1L, title, "Pat", "pat@example.com",
                LocalDate.of(2026, 9, 30), due, null);
    }
}
