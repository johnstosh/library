/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * Enum representing the available library card design options.
 * To add a new design: add an entry here and drop the image in
 * src/main/resources/static/images/library-cards/{name().toLowerCase()}.jpg
 * No other code changes are required.
 */
public enum LibraryCardDesign {
    COUNTRYSIDE_YOUTH("Countryside Youth", "Fresh, youthful design with natural elements"),
    SACRED_HEART_PORTRAIT("Sacred Heart Portrait", "Portrait-oriented design with sacred imagery"),
    RADIANT_BLESSING("Radiant Blessing", "Bright design with uplifting elements"),
    PATRON_OF_CREATURES("Patron of Creatures", "Nature-focused design with animal motifs"),
    CLASSICAL_DEVOTION("Classical Devotion", "Traditional design with classic typography"),
    CLARES_LIBRARY_CARD("Clare's Library Card", "Clare's personal library card design"),
    GRANDFATHER("Grandfather", "Grandfather's library card design"),
    LITTLE_FLOWER_BANK("Little Flower Bank", "League of the Little Flower Bank card"),
    LITTLE_FLOWER("Little Flower", "League of the Little Flower card"),
    MOTHER_ANGELICA("Mother Angelica", "Mother Angelica library card design");

    private final String displayName;
    private final String description;

    LibraryCardDesign(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public static LibraryCardDesign getDefault() {
        return CLASSICAL_DEVOTION;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getImageFilename() {
        return this.name().toLowerCase() + ".jpg";
    }
}
