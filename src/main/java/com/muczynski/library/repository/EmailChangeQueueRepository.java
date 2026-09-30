/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.EmailChangeQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EmailChangeQueueRepository extends JpaRepository<EmailChangeQueueEntry, Long> {

    Optional<EmailChangeQueueEntry> findBySubjectKey(String subjectKey);

    List<EmailChangeQueueEntry> findBySendAfterLessThanEqualOrderBySendAfterAsc(Instant sendAfter);
}
