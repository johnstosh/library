package com.muczynski.library.repository;

import com.muczynski.library.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByBookId(Long bookId);
}