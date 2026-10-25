/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.AbeSeller;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AbeSellerRepository extends JpaRepository<AbeSeller, Long> {

    Optional<AbeSeller> findByVendorId(Long vendorId);

    List<AbeSeller> findAllByOrderByNameAscIdAsc();

    @Query("SELECT s.id FROM AbeSeller s WHERE s.id > :lastId ORDER BY s.id ASC")
    List<Long> findIdsAfterId(@Param("lastId") Long lastId, Pageable pageable);
}
