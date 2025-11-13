/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * Exception thrown when a user attempts an operation they don't have permission for
 */
public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException(String message) {
        super(message);
    }

    public InsufficientPermissionsException() {
        super("Insufficient permissions for this operation");
    }
}
