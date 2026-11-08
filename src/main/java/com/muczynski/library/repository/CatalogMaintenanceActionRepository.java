/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.CatalogMaintenanceAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CatalogMaintenanceActionRepository extends JpaRepository<CatalogMaintenanceAction, Long> {

    boolean existsByReversesActionId(Long reversesActionId);

    @Query("""
            SELECT a FROM CatalogMaintenanceAction a
            WHERE a.actionKind = :kind
            AND NOT EXISTS (
                SELECT u.id FROM CatalogMaintenanceAction u WHERE u.reversesActionId = a.id
            )
            ORDER BY a.id DESC
            """)
    List<CatalogMaintenanceAction> findUndoable(@Param("kind") String kind, Pageable pageable);
}
