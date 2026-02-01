/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    /** @deprecated Use findAllByNameOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<Authority> findByName(String name);
    List<Authority> findAllByNameOrderByIdAsc(String name);
}
