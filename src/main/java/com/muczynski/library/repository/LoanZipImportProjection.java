/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

/**
 * Lightweight projection for photo ZIP import — loads only loan id, book title, and username.
 * The JOIN in the query excludes loans without a book or user (equivalent to the null guards
 * that were previously applied in Java after a full findAll()).
 */
public interface LoanZipImportProjection {
    Long getId();
    String getBookTitle();
    String getUsername();
}
