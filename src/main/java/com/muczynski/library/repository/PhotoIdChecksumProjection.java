/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

/**
 * Lightweight projection for deduplication checks during ZIP import.
 * Returns only the photo ID and checksum — image bytes are never loaded.
 */
public interface PhotoIdChecksumProjection {
    Long getId();
    String getImageChecksum();
}
