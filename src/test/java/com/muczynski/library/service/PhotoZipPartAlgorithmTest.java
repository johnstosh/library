/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.dto.PhotoZipPartDto;
import com.muczynski.library.repository.PhotoZipSortProjection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.muczynski.library.service.PhotoExportService.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the alphabetic ZIP-part algorithm — no Spring context, no database.
 */
class PhotoZipPartAlgorithmTest {

    // -------------------------------------------------------------------------
    // stripLeadingArticle
    // -------------------------------------------------------------------------

    @Test
    void stripsThe() {
        assertThat(stripLeadingArticle("The Wheel on the School")).isEqualTo("Wheel on the School");
    }

    @Test
    void stripsAn() {
        assertThat(stripLeadingArticle("An American Dream")).isEqualTo("American Dream");
    }

    @Test
    void stripsA() {
        assertThat(stripLeadingArticle("A Tale of Two Cities")).isEqualTo("Tale of Two Cities");
    }

    @Test
    void doesNotStripMidWordThe() {
        assertThat(stripLeadingArticle("Theology")).isEqualTo("Theology");
    }

    @Test
    void caseInsensitiveStrip() {
        assertThat(stripLeadingArticle("THE Lord of the Rings")).isEqualTo("Lord of the Rings");
        assertThat(stripLeadingArticle("the Hobbit")).isEqualTo("Hobbit");
    }

    @Test
    void noArticle() {
        assertThat(stripLeadingArticle("Beloved")).isEqualTo("Beloved");
    }

    @Test
    void nullAndEmpty() {
        assertThat(stripLeadingArticle(null)).isEqualTo("");
        assertThat(stripLeadingArticle("")).isEqualTo("");
    }

    // -------------------------------------------------------------------------
    // getSortKey
    // -------------------------------------------------------------------------

    @Test
    void sortKeyForRegularTitle() {
        assertThat(getSortKey("Beloved")).isEqualTo('B');
    }

    @Test
    void sortKeyStripsArticleFirst() {
        assertThat(getSortKey("The Wheel on the School")).isEqualTo('W');
        assertThat(getSortKey("A Tale of Two Cities")).isEqualTo('T');
    }

    @Test
    void sortKeyForNumericTitle() {
        assertThat(getSortKey("33 Days to Morning Glory")).isEqualTo('0');
        assertThat(getSortKey("101 Dalmatians")).isEqualTo('0');
    }

    @Test
    void sortKeyForNullBlankAndSymbol() {
        assertThat(getSortKey(null)).isEqualTo('0');
        assertThat(getSortKey("   ")).isEqualTo('0');
        assertThat(getSortKey("@Special")).isEqualTo('0'); // symbol folds into digits bucket
    }

    @Test
    void sortKeyIsUppercase() {
        assertThat(getSortKey("silence")).isEqualTo('S');
    }

    // -------------------------------------------------------------------------
    // buildRangeLabel
    // -------------------------------------------------------------------------

    @Test
    void singleLetterRange() {
        assertThat(buildRangeLabel('S', 'S')).isEqualTo("S");
    }

    @Test
    void letterOnlyRange() {
        assertThat(buildRangeLabel('I', 'R')).isEqualTo("I-R");
    }

    @Test
    void digitOnlyRange() {
        assertThat(buildRangeLabel('0', '0')).isEqualTo("0-9");
    }

    @Test
    void digitToLetterRange() {
        assertThat(buildRangeLabel('0', 'H')).isEqualTo("0-9, A-H");
        assertThat(buildRangeLabel('0', 'A')).isEqualTo("0-9, A");
    }

    @Test
    void digitToLetterOnlyA() {
        assertThat(buildRangeLabel('0', 'A')).isEqualTo("0-9, A");
    }

    // -------------------------------------------------------------------------
    // computeZipPartsFromSortData
    // -------------------------------------------------------------------------

    @Test
    void emptyCollectionProducesNoParts() {
        assertThat(computeZipPartsFromSortData(List.of())).isEmpty();
    }

    @Test
    void singlePhotoProducesOnePart() {
        List<PhotoZipPartDto> parts = computeZipPartsFromSortData(List.of(row("Beloved")));
        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).getPartNumber()).isEqualTo(1);
        assertThat(parts.get(0).getTotalParts()).isEqualTo(1);
        assertThat(parts.get(0).getPhotoCount()).isEqualTo(1);
    }

    @Test
    void productionScaleProducesThreeParts() {
        // Reproduce the production distribution (836 photos, 1.2 MB assumed → 3 parts)
        List<PhotoZipSortProjection> data = new ArrayList<>();
        // Numeric titles: 15
        addRows(data, "33 Days to Morning Glory", 15);
        // A-K distribution from production data (article-stripped)
        addRows(data, "Adventure",   37); // A
        addRows(data, "Beloved",     45); // B
        addRows(data, "Christmas",   62); // C
        addRows(data, "Desert",      22); // D
        addRows(data, "Empire",      23); // E
        addRows(data, "Fantastic",   42); // F
        addRows(data, "Garden",      26); // G
        addRows(data, "Harbor",      43); // H
        addRows(data, "Island",      34); // I
        addRows(data, "Journey",     11); // J
        addRows(data, "Kingdom",     12); // K
        // L-Z distribution
        addRows(data, "Library",     68); // L
        addRows(data, "Mountain",    52); // M
        addRows(data, "Nature",      11); // N
        addRows(data, "Ocean",       25); // O
        addRows(data, "Paradise",    42); // P (includes 4 loan photos)
        addRows(data, "Quest",        2); // Q
        addRows(data, "River",       32); // R
        addRows(data, "Summer",     148); // S
        addRows(data, "Travel",      24); // T
        addRows(data, "Universe",     8); // U
        addRows(data, "Valley",       8); // V
        addRows(data, "Winter",      25); // W
        addRows(data, "Yearbook",    15); // Y
        // total = 832 (book photos); 4 loan photos are separate in production but not needed here
        addRows(data, "Purgatory",    1); // loan photo → P
        addRows(data, "Children Everywhere", 1); // loan photo → C
        addRows(data, "The Wheel on the School", 1); // loan photo → W (article stripped)
        addRows(data, "Pennyweather Luck", 1); // loan photo → P
        // total = 836

        List<PhotoZipPartDto> parts = computeZipPartsFromSortData(data);

        assertThat(parts).hasSize(3);
        assertThat(parts.stream().mapToInt(PhotoZipPartDto::getPhotoCount).sum()).isEqualTo(836);

        // All parts are covered; verify totalParts is set consistently
        parts.forEach(p -> assertThat(p.getTotalParts()).isEqualTo(3));

        // No part should exceed 400 MB with 1 MB assumption
        parts.forEach(p -> assertThat(p.getEstimatedMb()).isLessThanOrEqualTo(400));

        // Range labels are non-empty and part numbers are sequential
        for (int i = 0; i < parts.size(); i++) {
            assertThat(parts.get(i).getPartNumber()).isEqualTo(i + 1);
            assertThat(parts.get(i).getRangeLabel()).isNotBlank();
        }
    }

    @Test
    void tinyCollectionFitsInOnePart() {
        // 5 photos × 1.2 MB = 6 MB → well under 400 MB → 1 part
        List<PhotoZipSortProjection> data = List.of(
                row("Alice"), row("Bob"), row("Charlie"), row("Dave"), row("Eve"));
        List<PhotoZipPartDto> parts = computeZipPartsFromSortData(data);
        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).getPhotoCount()).isEqualTo(5);
    }

    @Test
    void allPhotosAccountedFor() {
        List<PhotoZipSortProjection> data = new ArrayList<>();
        addRows(data, "Apple", 50);
        addRows(data, "Banana", 50);
        addRows(data, "Cherry", 50);
        List<PhotoZipPartDto> parts = computeZipPartsFromSortData(data);
        int total = parts.stream().mapToInt(PhotoZipPartDto::getPhotoCount).sum();
        assertThat(total).isEqualTo(150);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static PhotoZipSortProjection row(String sortName) {
        return new PhotoZipSortProjection() {
            @Override public Long getId() { return 0L; }
            @Override public String getSortName() { return sortName; }
        };
    }

    private static void addRows(List<PhotoZipSortProjection> list, String name, int count) {
        for (int i = 0; i < count; i++) list.add(row(name));
    }
}
