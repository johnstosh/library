/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.Applied;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppliedRepository extends JpaRepository<Applied, Long> {
    List<Applied> findAllByNameOrderByIdAsc(String name);
}