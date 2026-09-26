/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Library, Long> {
    /** @deprecated Use findAllByBranchNameOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<Library> findByBranchName(String branchName);
    List<Library> findAllByBranchNameOrderByIdAsc(String branchName);

    /** Keyset id page for chunked JSON export. */
    @Query("SELECT b.id FROM Library b WHERE b.id > :lastId ORDER BY b.id ASC")
    List<Long> findBranchIdsAfterId(@Param("lastId") Long lastId, Pageable pageable);
}
