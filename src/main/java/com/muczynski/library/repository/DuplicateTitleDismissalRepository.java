/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.DuplicateTitleDismissal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DuplicateTitleDismissalRepository extends JpaRepository<DuplicateTitleDismissal, Long> {

    Optional<DuplicateTitleDismissal> findByPairKey(String pairKey);
}
