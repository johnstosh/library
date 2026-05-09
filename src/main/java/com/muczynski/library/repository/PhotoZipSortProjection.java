/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

/**
 * Lightweight projection used solely to assign alphabetic ZIP parts.
 * Returns a photo's ID and the name used to derive its sort key (book title,
 * author name, or the book title reachable via a loan — whichever applies).
 * Image bytes are never loaded.
 */
public interface PhotoZipSortProjection {
    Long getId();
    /** COALESCE(book.title, author.name, loanBook.title) — null if none available */
    String getSortName();
}
