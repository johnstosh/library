/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

import com.muczynski.library.dto.ErrorResponse;
import com.muczynski.library.dto.ValidationErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for all controllers
 * Provides standardized error responses and proper HTTP status codes
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        logger.debug("Validation error on path {}: {}", request.getDescription(false), ex.getMessage());

        ValidationErrorResponse response = new ValidationErrorResponse("Validation failed");

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            response.addFieldError(error.getField(), error.getDefaultMessage());
        }

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        logger.debug("Resource not found on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle book already loaned exceptions
     */
    @ExceptionHandler(BookAlreadyLoanedException.class)
    public ResponseEntity<ErrorResponse> handleBookAlreadyLoanedException(
            BookAlreadyLoanedException ex, WebRequest request) {
        logger.debug("Book already loaned on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("BOOK_ALREADY_LOANED", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle insufficient permissions exceptions
     */
    @ExceptionHandler(InsufficientPermissionsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPermissionsException(
            InsufficientPermissionsException ex, WebRequest request) {
        logger.debug("Insufficient permissions on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("INSUFFICIENT_PERMISSIONS", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle Spring Security authorization denied exceptions (Spring Security 6+)
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex, WebRequest request) {
        logger.debug("Authorization denied on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("ACCESS_DENIED", "You do not have permission to access this resource");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle Spring Security access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        logger.debug("Access denied on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("ACCESS_DENIED", "You do not have permission to access this resource");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle illegal argument exceptions (bad requests)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        logger.debug("Illegal argument on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("INVALID_ARGUMENT", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle generic runtime exceptions (internal server errors)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        logger.error("Runtime exception on path {}: {}", request.getDescription(false), ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse("INTERNAL_ERROR", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected exception on path {}: {}", request.getDescription(false), ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
