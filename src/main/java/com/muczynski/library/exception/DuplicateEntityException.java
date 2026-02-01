/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

import lombok.Getter;

/**
 * Exception thrown when attempting to create a duplicate entity.
 * Handled globally to return HTTP 409 (Conflict) with enriched error details.
 */
@Getter
public class DuplicateEntityException extends LibraryException {

    private final String entityType;
    private final String entityName;
    private final Long existingEntityId;

    public DuplicateEntityException(String entityType, String entityName, Long existingEntityId) {
        super("Duplicate " + entityType + ": '" + entityName + "' already exists (ID: " + existingEntityId + ")");
        this.entityType = entityType;
        this.entityName = entityName;
        this.existingEntityId = existingEntityId;
    }

    public DuplicateEntityException(String entityType, String entityName) {
        super("Duplicate " + entityType + ": '" + entityName + "' already exists");
        this.entityType = entityType;
        this.entityName = entityName;
        this.existingEntityId = null;
    }
}
