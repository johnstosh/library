/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.GlobalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalSettingsRepository extends JpaRepository<GlobalSettings, Long> {

    /**
     * Find the global settings singleton.
     * There should only ever be one row in this table.
     */
    Optional<GlobalSettings> findFirstByOrderByIdAsc();
}
