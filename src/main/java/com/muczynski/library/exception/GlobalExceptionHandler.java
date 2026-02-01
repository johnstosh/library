/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

import com.muczynski.library.dto.ErrorResponse;
import com.muczynski.library.dto.ValidationErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        logger.debug("Missing request parameter on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("MISSING_PARAMETER",
            "Required parameter '" + ex.getParameterName() + "' is missing");
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
     * Handle file upload size exceeded (413 Payload Too Large)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, WebRequest request) {
        long maxSize = ex.getMaxUploadSize();
        String maxSizeStr = maxSize > 0 ? formatBytes(maxSize) : "100 MB";
        String message = "File size exceeds the maximum upload limit of " + maxSizeStr
                + ". Use the streaming endpoint for larger files, or reduce file size.";
        logger.warn("Upload size exceeded on path {}: {} (max: {})",
                request.getDescription(false), ex.getMessage(), maxSizeStr);

        ErrorResponse response = new ErrorResponse("UPLOAD_TOO_LARGE", message);
        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) return String.format("%.0f GB", bytes / (1024.0 * 1024 * 1024));
        if (bytes >= 1024 * 1024) return String.format("%.0f MB", bytes / (1024.0 * 1024));
        if (bytes >= 1024) return String.format("%.0f KB", bytes / 1024.0);
        return bytes + " bytes";
    }

    /**
     * Handle duplicate entity exceptions (409 Conflict) with enriched error details
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEntityException(
            DuplicateEntityException ex, WebRequest request) {
        logger.warn("Duplicate entity on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("DUPLICATE_ENTITY", ex.getMessage(),
                ex.getEntityType(), ex.getEntityName(), ex.getExistingEntityId());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle database unique constraint violations (409 Conflict)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        String message = extractConstraintMessage(ex);
        logger.warn("Data integrity violation on path {}: {}", request.getDescription(false), message);

        ErrorResponse response = new ErrorResponse("DUPLICATE_ENTITY", message);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Extract a user-friendly message from a DataIntegrityViolationException.
     * Attempts to identify the constraint name from the root cause.
     */
    private String extractConstraintMessage(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();
        if (rootMessage == null) {
            return "A duplicate entry was detected";
        }

        // Try to extract constraint name from PostgreSQL error message
        // Format: "ERROR: duplicate key value violates unique constraint "uk_xxx""
        if (rootMessage.contains("unique constraint")) {
            int start = rootMessage.indexOf('"');
            int end = rootMessage.indexOf('"', start + 1);
            if (start >= 0 && end > start) {
                String constraintName = rootMessage.substring(start + 1, end);
                return "Duplicate entry violates constraint: " + constraintName;
            }
        }

        return "A duplicate entry was detected: " + rootMessage;
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
     * Handle library-specific business logic exceptions
     * Returns HTTP 422 (Unprocessable Entity) for business rule violations
     */
    @ExceptionHandler(LibraryException.class)
    public ResponseEntity<ErrorResponse> handleLibraryException(
            LibraryException ex, WebRequest request) {
        logger.warn("Library exception on path {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse response = new ErrorResponse("BUSINESS_RULE_VIOLATION", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
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

        // Include the actual error message to help with debugging
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unexpected error occurred: " + ex.getClass().getSimpleName();
        }
        ErrorResponse response = new ErrorResponse("INTERNAL_ERROR", message);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
