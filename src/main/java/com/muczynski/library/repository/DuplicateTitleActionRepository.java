/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.DuplicateTitleAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Append-only store for Duplicate Titles merges and fixes.
 * Callers insert rows. They do not delete or update them.
 */
@Repository
public interface DuplicateTitleActionRepository extends JpaRepository<DuplicateTitleAction, Long> {

    boolean existsByReversesActionId(Long reversesActionId);
}
