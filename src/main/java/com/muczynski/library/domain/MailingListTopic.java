/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.domain;

import java.util.List;

/**
 * Patron mailing lists. Tag strings are catalog tags from {@link BookLabels},
 * not these display names.
 */
public enum MailingListTopic {
    LIVES_OF_SAINTS("Lives of Saints"),
    LIVES_OF_MEN_SAINTS("Lives of Men Saints"),
    LIVES_OF_WOMEN_SAINTS("Lives of Women Saints"),
    CHILDRENS_FICTION("Childrens Fiction"),
    READ_ALOUD_BOOKS("Read-aloud Books"),
    NEW_CATALOG_ENTRIES("New Catalog Entries"),
    ADULT_FICTION("Adult Fiction");

    private final String displayName;

    MailingListTopic(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Tags a random-pick list requires, using catalog tag strings.
     * Empty for lists that include every applicable book.
     */
    public List<String> requiredTags() {
        return switch (this) {
            case CHILDRENS_FICTION -> List.of("childrens", "fiction");
            case READ_ALOUD_BOOKS -> List.of("read-aloud");
            case ADULT_FICTION -> List.of("adult", "fiction");
            case LIVES_OF_SAINTS, LIVES_OF_MEN_SAINTS, LIVES_OF_WOMEN_SAINTS, NEW_CATALOG_ENTRIES -> List.of();
        };
    }

    public boolean recommendsOneBook() {
        return !requiredTags().isEmpty();
    }
}
