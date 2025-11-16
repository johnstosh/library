/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * Custom exception for library-specific business logic errors.
 * This exception is handled globally and returns HTTP 422 (Unprocessable Entity).
 */
public class LibraryException extends RuntimeException {

    /**
     * Constructs a new LibraryException with the specified detail message.
     *
     * @param message the detail message
     */
    public LibraryException(String message) {
        super(message);
    }

    /**
     * Constructs a new LibraryException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new LibraryException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public LibraryException(Throwable cause) {
        super(cause);
    }
}
