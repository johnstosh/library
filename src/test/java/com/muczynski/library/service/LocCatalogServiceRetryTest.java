/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.model.LocCallNumberResponse;
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

    @Test
    void getLocCallNumber_retriesWhenLocOmitsTheRecordPayload() {
        LocCatalogService service = new LocCatalogService(restTemplate, 0, 0, 0, ignored -> { });
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(DIAGNOSTIC_WITHOUT_RECORDS)
                .thenReturn(RECORD_WITH_CALL_NUMBER);
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("Catholic Study Bible");

        LocCallNumberResponse response = service.getLocCallNumber(request);

        assertEquals("BS192.3 .A1 2020", response.getCallNumber());
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void getLocCallNumber_omittedPayloadStopsAfterThreeTries() {
        LocCatalogService service = new LocCatalogService(restTemplate, 0, 0, 0, ignored -> { });
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(DIAGNOSTIC_WITHOUT_RECORDS);
        LocSearchRequest request = new LocSearchRequest();
        request.setTitle("Catholic Study Bible");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getLocCallNumber(request));

        assertEquals(404, ex.getStatusCode().value());
        verify(restTemplate, times(3)).getForObject(anyString(), eq(String.class));
    }

    private static final String DIAGNOSTIC_WITHOUT_RECORDS = """
            <?xml version="1.0"?>
            <zs:searchRetrieveResponse xmlns:zs="http://www.loc.gov/zing/srw/">
              <zs:version>1.1</zs:version>
              <zs:numberOfRecords>42803</zs:numberOfRecords>
              <zs:diagnostics xmlns:diag="http://www.loc.gov/zing/srw/diagnostic/">
                <diag:diagnostic><diag:message>System temporarily unavailable</diag:message></diag:diagnostic>
              </zs:diagnostics>
            </zs:searchRetrieveResponse>
            """;

    private static final String RECORD_WITH_CALL_NUMBER = """
            <?xml version="1.0"?>
            <zs:searchRetrieveResponse xmlns:zs="http://www.loc.gov/zing/srw/">
              <zs:version>1.1</zs:version>
              <zs:numberOfRecords>1</zs:numberOfRecords>
              <zs:records><zs:record><zs:recordData>
                <record xmlns="http://www.loc.gov/MARC21/slim">
                  <datafield tag="050" ind1=" " ind2=" ">
                    <subfield code="a">BS192.3</subfield>
                    <subfield code="b">.A1 2020</subfield>
                  </datafield>
                </record>
              </zs:recordData></zs:record></zs:records>
            </zs:searchRetrieveResponse>
            """;

    private static HttpClientErrorException tooMany() {
        return HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8);
    }
}
