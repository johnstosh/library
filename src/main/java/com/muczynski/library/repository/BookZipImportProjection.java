/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

/**
 * Lightweight projection for photo ZIP import — loads only id and title, skipping @Lob fields.
 */
public interface BookZipImportProjection {
    Long getId();
    String getTitle();
}
