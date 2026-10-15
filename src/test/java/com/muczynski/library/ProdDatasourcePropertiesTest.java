// (c) Copyright 2026 by Muczynski
package com.muczynski.library;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProdDatasourcePropertiesTest {

    @Test
    void prodUrlUsesTheCloudRunUnixSocket() throws IOException {
        try (var in = getClass().getResourceAsStream("/application-prod.properties")) {
            assertThat(in).isNotNull();
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(text).contains("org.newsclub.net.unix.AFUNIXSocketFactory$FactoryArg");
            assertThat(text).contains(
                    "/cloudsql/${GCP_PROJECT_ID}:${GCP_REGION}:scrabble-db/.s.PGSQL.5432");
            assertThat(text).doesNotContain("com.google.cloud.sql");
        }
    }
}
