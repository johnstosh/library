// (c) Copyright 2025 by Muczynski
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    Page<Author> findByNameContainingIgnoreCase(String name, Pageable pageable);
    @Modifying
    @Query("DELETE FROM Author a WHERE a.religiousAffiliation = ?1")
    void deleteByReligiousAffiliation(String religiousAffiliation);
}