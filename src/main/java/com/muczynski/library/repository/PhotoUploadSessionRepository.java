/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.PhotoUploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PhotoUploadSessionRepository extends JpaRepository<PhotoUploadSession, Long> {
    Optional<PhotoUploadSession> findByUploadId(String uploadId);

    @Transactional
    void deleteByLastActivityAtBefore(Instant cutoff);
}
