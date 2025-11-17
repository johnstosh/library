/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * Enum representing the available library card design options.
 * Each design features St Martin de Porres in different artistic styles.
 */
public enum LibraryCardDesign {
    COUNTRYSIDE_YOUTH,    // Young St Martin with broom in countryside setting
    SACRED_HEART_PORTRAIT, // Adult St Martin portrait with "Sacred Heart Library" label
    RADIANT_BLESSING,     // St Martin with golden halo and serene expression
    PATRON_OF_CREATURES,  // Older St Martin with mice gathering wheat
    CLASSICAL_DEVOTION;   // St Martin in arched setting with cross (DEFAULT)

    /**
     * Returns the default library card design.
     */
    public static LibraryCardDesign getDefault() {
        return CLASSICAL_DEVOTION;
    }

    /**
     * Returns a user-friendly display name for the design.
     */
    public String getDisplayName() {
        return switch (this) {
            case COUNTRYSIDE_YOUTH -> "Countryside Youth";
            case SACRED_HEART_PORTRAIT -> "Sacred Heart Portrait";
            case RADIANT_BLESSING -> "Radiant Blessing";
            case PATRON_OF_CREATURES -> "Patron of Creatures";
            case CLASSICAL_DEVOTION -> "Classical Devotion";
        };
    }

    /**
     * Returns the filename for the image associated with this design.
     */
    public String getImageFilename() {
        return this.name().toLowerCase() + ".jpg";
    }
}
