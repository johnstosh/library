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
    COUNTRYSIDE_YOUTH("Countryside Youth", "Cartoon child St. Martin in Dominican habit holding a broom, with a mouse, sunny countryside in the background"),
    SACRED_HEART_PORTRAIT("Sacred Heart Portrait", "Comic-style adult St. Martin holding a broom with a mouse on his shoulder, set in a circular halo on a gray card"),
    RADIANT_BLESSING("Radiant Blessing", "Illustrated St. Martin with golden halo and broom, framed by lush green botanical leaves in a horizontal layout"),
    PATRON_OF_CREATURES("Patron of Creatures", "St. Martin with broom gesturing to three mice at his feet, with cheerful watercolor green and yellow splashes"),
    CLASSICAL_DEVOTION("Classical Devotion", "Detailed pencil-and-watercolor St. Martin with broom and two mice, soft pastel background with arched doorway"),
    CLARES_LIBRARY_CARD("Clare's Library Card", "A real hand-drawn library card made by Clare, with branch list, her name and number, and a crayon barcode drawing"),
    GRANDFATHER_LIBRARY_CARD("Grandfather Library Card", "The child's pencil drawing of \"grand father\" mounted on the Sacred Heart Library System card template with Name and Number fields"),
    GRANDFATHER("Grandfather at the Library", "Sacred Heart Library System card with sepia pencil vignettes of grandfather reading by a fireplace, browsing the shelves, sharing books with children, and working the card catalog"),
    LITTLE_FLOWER_BANK("Little Flower Bank", "Hand-drawn marker card for the League of the Little Flower Bank, with a flower sketch and wavy border"),
    LITTLE_FLOWER("Little Flower", "Hand-lettered blue ink card for the League of the Little Flower, with a floral vine decoration down the right side"),
    MOTHER_ANGELICA("Mother Angelica", "Quote card with Mother Angelica's words on reading Scripture and the lives of saints"),
    BLUE_AND_GOLD("Blue and Gold", "Portrait of St. Martin de Porres on a clean navy and gold professional ID-style card"),
    FLEUR_DE_LIS_COLORFUL("Colorful Fleur de Lis", "Stained-glass radial Sacred Heart with crown of thorns, surrounded by colorful fleur de lis in a circular frame"),
    SACRED_HEART_FLAMES("Sacred Heart Flames", "Blazing Sacred Heart radiating white light, framed by richly colored fire and ornate borders"),
    SACRED_HEART_BLOOD_RED("Blood Red Sacred Heart", "Deep crimson background with gold fleur de lis corners and a glowing Sacred Heart with crown of thorns"),
    SACRED_HEART_ORNATE("Sacred Heart Ornate", "Luminous Sacred Heart with thorns, surrounded by colorful heart medallions in each corner of a gilded frame"),
    MAROON_ON_PARCHMENT("Maroon on Parchment", "Vintage-style maroon on aged parchment with radiating sunburst and ornate scroll borders"),
    MAROON_AND_GOLD_RADIAL("Maroon and Gold Radial", "Maroon and gold radial sunburst with central compass rose medallion and baroque gold scroll frame"),
    SACRED_HEART_COLORFUL("Sacred Heart Colorful", "Sacred Heart with thorns radiating golden light, framed by an iridescent blue and gold acanthus scroll border"),
    SACRED_HEART_THORNS("Sacred Heart with Thorns", "Sacred Heart with golden cross and thorns radiating glory beams in a parchment-toned baroque scroll frame"),
    ST_MARTIN_SERVUS_PAUPERUM("St. Martin Servus Pauperum", "Portrait-format St. Martin in a circular medallion ringed with his symbols — broom, rosary, sacred heart — inscribed \"Servus Pauperum\""),
    ST_MARTIN_BROOM("St. Martin with Broom", "St. Martin de Porres with halo and broom on deep maroon, with motto \"Serving All with Humility and Charity\""),
    ST_MARTIN_BROOMS_CROSSES("St. Martin Brooms and Crosses", "St. Martin de Porres flanked by maroon side panels bearing brooms, crosses, and hearts"),
    ST_MARTIN_MAROON("St. Martin Maroon", "St. Martin holding a broom in a maroon radiant sunburst, flanked by ornate corner scrollwork"),
    VERY_COLORFUL_ORNATE("Very Colorful Ornate", "Jewel-toned cathedral-ceiling sunburst with deep red ornate border and rich blue and gold accents");

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
