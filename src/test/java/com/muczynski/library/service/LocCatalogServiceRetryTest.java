/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.model.LocSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocCatalogServiceRetryTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void getLocCallNumber_tooManyRequests_backsOffBeforeTheNextTry() {
        List<Long> pauses = new ArrayList<>();
        LocCatalogService service = new LocCatalogService(restTemplate, 2, 2_000, 0, pauses::add);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(tooMany())
                .thenReturn("");
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("Test Book");

        assertThrows(ResponseStatusException.class, () -> service.getLocCallNumber(request));

        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class));
        assertEquals(1, pauses.size());
        assertTrue(Math.abs(pauses.get(0) - 2_000) <= 250, "pause was " + pauses.get(0));
    }

    private static HttpClientErrorException tooMany() {
        return HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8);
    }
}
