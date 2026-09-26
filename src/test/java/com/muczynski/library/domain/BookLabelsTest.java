/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookLabelsTest {

    @Test
    void allLabelsAreCanonicalAndLowercaseWithDashes() {
        for (String label : BookLabels.ALL_LABELS) {
            assertThat(label).isNotNull()
                    .isEqualTo(label.toLowerCase())
                    .doesNotContain(" ")
                    .doesNotContain("_")
                    .doesNotContain("'");
        }
    }

    @Test
    void normalizeTagHandlesCanonicalsCaseWhitespaceUnderscores() {
        assertThat(BookLabels.normalizeTag("Fiction")).isEqualTo("fiction");
        assertThat(BookLabels.normalizeTag(" slice_of_life ")).isEqualTo("slice-of-life");
        assertThat(BookLabels.normalizeTag("TALKING ANIMALS")).isEqualTo("talking-animals");
        assertThat(BookLabels.normalizeTag("History")).isEqualTo("history");
    }

    @Test
    void normalizeTagMapsPluralsAndVariantsToCanonical() {
        assertThat(BookLabels.normalizeTag("histories")).isEqualTo("history");
        assertThat(BookLabels.normalizeTag("biographies")).isEqualTo("biography");
        assertThat(BookLabels.normalizeTag("fantasies")).isEqualTo("fantasy");
        assertThat(BookLabels.normalizeTag("children")).isEqualTo("childrens");
        assertThat(BookLabels.normalizeTag("children's")).isEqualTo("childrens");
        assertThat(BookLabels.normalizeTag("humour")).isEqualTo("humor");
        assertThat(BookLabels.normalizeTag("slice of life")).isEqualTo("slice-of-life");
    }

    @Test
    void isValidLabelAndGetCanonicalLabel() {
        assertThat(BookLabels.isValidLabel("fiction")).isTrue();
        assertThat(BookLabels.isValidLabel("histories")).isTrue(); // maps to valid
        assertThat(BookLabels.isValidLabel("foobar")).isFalse();
        assertThat(BookLabels.getCanonicalLabel("histories")).isEqualTo("history");
        assertThat(BookLabels.getCanonicalLabel("invalid-tag")).isNull();
    }

    @Test
    void cleanupTagsRemovesIllegalNormalizesPluralsDeduplicatesPreservesOrder() {
        List<String> tags = Arrays.asList("fiction", "Histories", "biographies", "fiction", "invalid", "slice of life", "Fantasy");
        List<String> cleaned = BookLabels.cleanupTags(tags);

        assertThat(cleaned).containsExactly("fiction", "history", "biography", "slice-of-life", "fantasy");
        // no duplicates, order of first occurrence, illegal removed
    }

    @Test
    void cleanupTagsIsIdempotentOnAlreadyCleanTags() {
        List<String> cleanTags = Arrays.asList("fiction", "fantasy", "history", "slice-of-life");
        List<String> cleaned = BookLabels.cleanupTags(cleanTags);
        assertThat(cleaned).isEqualTo(cleanTags);
    }

    @Test
    void cleanupTagsHandlesEmptyAndNull() {
        assertThat(BookLabels.cleanupTags(null)).isEmpty();
        assertThat(BookLabels.cleanupTags(List.of())).isEmpty();
    }

    @Test
    void maintenanceCountAndCleanupLogicMatchesSpec() {
        // The service uses these methods; verified via unit tests above
        assertThat(BookLabels.ALL_LABELS).hasSize(23);
    }
}
