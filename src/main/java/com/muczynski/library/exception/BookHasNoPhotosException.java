/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * A photo-driven AI action (Book from Image / Book from First Photo) was run on a book
 * with no photos. The book is left unchanged. Handled globally as HTTP 400.
 */
public class BookHasNoPhotosException extends RuntimeException {

    public static final String BOOK_FROM_IMAGE_MESSAGE =
            "This book has no photos, so Book from Image has nothing to read.";
    public static final String BOOK_FROM_FIRST_PHOTO_MESSAGE =
            "This book has no photos, so Book from First Photo has nothing to read.";

    public BookHasNoPhotosException(String message) {
        super(message);
    }
}
