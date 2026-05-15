/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

/**
 * Lightweight projection for photo ZIP import — loads only id and name, skipping @Lob fields.
 */
public interface AuthorZipImportProjection {
    Long getId();
    String getName();
}
