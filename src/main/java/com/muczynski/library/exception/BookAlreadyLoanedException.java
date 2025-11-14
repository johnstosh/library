/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * Exception thrown when attempting to loan a book that is already on loan
 */
public class BookAlreadyLoanedException extends RuntimeException {
    public BookAlreadyLoanedException(Long bookId) {
        super("Book with ID " + bookId + " is already on loan");
    }

    public BookAlreadyLoanedException(String message) {
        super(message);
    }
}
