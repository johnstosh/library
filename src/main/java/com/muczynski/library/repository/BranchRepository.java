/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Library, Long> {
    /** @deprecated Use findAllByBranchNameOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<Library> findByBranchName(String branchName);
    List<Library> findAllByBranchNameOrderByIdAsc(String branchName);
}
