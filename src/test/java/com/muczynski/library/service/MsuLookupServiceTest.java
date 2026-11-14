/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.PublicLibrary;
import com.muczynski.library.domain.RejectedHolding;
import com.muczynski.library.dto.MsuLookupResultDto;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MsuLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private RestTemplate msuRestTemplate;

    private MsuLookupService msuLookupService;

    @BeforeEach
    void setUp() {
        msuLookupService = service(0, 0, 0, millis -> {
            fail("unexpected MSU pause of " + millis + " ms");
        });
    }

    private MsuLookupService service(int retries, long backoffMs, long requestDelayMs, LongConsumer pauser) {
        return new MsuLookupService(bookRepository, msuRestTemplate, retries, backoffMs, requestDelayMs, pauser);
    }

    private static final String ALL_FORMATS = """
            {"status":"OK","records":[
              {"title":"Test Book / Author, Test.","shortTitle":"Test Book","formats":["Book"],
               "authors":{"primary":{"Author, Test":{"role":["author"]}}}},
              {"title":"Test Book","shortTitle":"Test Book","formats":["Audio"],
               "authors":{"primary":{"Author, Test":{"role":[]}}}},
              {"title":"Test Book","shortTitle":"Test Book","formats":["eBook"],
               "authors":{"primary":{"Author, Test":{"role":[]}}}},
              {"title":"Test Book","shortTitle":"Test Book","formats":["Book","Electronic"],
               "authors":{"primary":{"Author, Test":{"role":[]}}}},
              {"title":"Test Book","shortTitle":"Test Book","formats":["CD"],
               "authors":{"primary":[]}}
            ]}
            """;

    private static final String NONE = """
            {"status":"OK","resultCount":0,"records":[]}
            """;

    @Test
    void lookupAndUpdateBook_marksPaperEbookAndAudioFromMsuFormatLabels() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertTrue(result.getPaperAvailable());
        assertTrue(result.getEbookAvailable());
        assertTrue(book.getMsuAudioAvailable());
        assertTrue(book.getMsuPaperAvailable());
        assertTrue(book.getMsuEbookAvailable());
        assertNull(book.getMsuLookupError());
        assertNotNull(book.getMsuLastChecked());

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        verify(msuRestTemplate).getForObject(urlCaptor.capture(), eq(String.class));
        String url = urlCaptor.getValue().toString();
        assertTrue(url.startsWith(MsuLookupService.SEARCH_URL));
        assertTrue(url.contains("type=Title"));
        assertTrue(url.contains("lookfor=Test%20Book") || url.contains("lookfor=Test+Book"));
        assertTrue(url.contains("shortTitle"));
        assertFalse(url.contains("primo"));
    }

    @Test
    void lookupAndUpdateBook_bookPlusElectronicIsEbookNotPaper() {
        Book book = bookWithAuthor("Online Only", "Test Author");
        String body = """
                {"status":"OK","records":[
                  {"title":"Online Only","shortTitle":"Online Only","formats":["Book","Electronic"],
                   "authors":{"primary":{"Author, Test":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getEbookAvailable());
        assertFalse(result.getPaperAvailable());
        assertFalse(result.getAudioAvailable());
    }

    @Test
    void lookupAndUpdateBook_matchesShortTitleAndAuthorKey() {
        Book book = bookWithAuthor("Short Title", "Ellen White");
        String body = """
                {"status":"OK","records":[
                  {"title":"Short Title / White, Ellen.","shortTitle":"Short Title","formats":["Book"],
                   "authors":{"primary":{"White, Ellen G.":{"role":["author"]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertEquals("Short Title", result.getMatchedTitle());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchSubstringAuthor() {
        Book book = bookWithAuthor("Short Title", "Ellen White");
        String body = """
                {"status":"OK","records":[
                  {"title":"Short Title","shortTitle":"Short Title","formats":["Book"],
                   "authors":{"primary":{"Whitehead, John":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Michigan State University", result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_doesNotMatchLongerTitle() {
        Book book = bookWithAuthor("Joshua", "Test Author");
        String body = """
                {"status":"OK","records":[
                  {"title":"Joshua in a Troubled World","shortTitle":"Joshua in a Troubled World","formats":["Book"],
                   "authors":{"primary":{"Author, Test":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
    }

    @Test
    void lookupAndUpdateBook_noMatch_clearsStaleHoldings() {
        Book book = bookWithAuthor("Nonexistent Book Title", "Test Author");
        book.setMsuAudioAvailable(true);
        book.setMsuPaperAvailable(true);
        book.setMsuEbookAvailable(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(NONE);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Michigan State University", book.getMsuLookupError());
        assertFalse(book.getMsuAudioAvailable());
        assertFalse(book.getMsuPaperAvailable());
        assertFalse(book.getMsuEbookAvailable());
    }

    @Test
    void lookupAndUpdateBook_htmlBlockPage_isAnErrorNotAMiss() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn("<html><title>Your access to the catalog has been limited</title></html>");
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertNotEquals("Not held by Michigan State University", result.getErrorMessage());
        assertTrue(book.getMsuLookupError().toLowerCase().contains("non-json")
                || book.getMsuLookupError().toLowerCase().contains("msu"));
        assertNull(book.getMsuPaperAvailable());
    }

    @Test
    void lookupAndUpdateBook_fallsBackToTitleBeforeColon() {
        Book book = bookWithAuthor("Crucial Conversations: Tools for Talking When Stakes Are High", "Kerry Patterson");
        String miss = NONE;
        String hit = """
                {"status":"OK","records":[
                  {"title":"Crucial Conversations : tools for talking when stakes are high / Kerry Patterson.",
                   "shortTitle":"Crucial Conversations","formats":["Book"],
                   "authors":{"secondary":[{"name":"Patterson, Kerry"}]}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(miss, hit);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertEquals("Crucial Conversations", result.getMatchedTitle());
    }

    @Test
    void lookupAndUpdateBook_longTitleMatchesWithoutAuthor() {
        Book book = bookWithAuthor("Crucial Conversations Tools For Talking", "Test Author");
        String body = """
                {"status":"OK","records":[
                  {"title":"Crucial Conversations Tools For Talking","formats":["Cassette"]}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
    }

    @Test
    void lookupAndUpdateBook_stripsCopySuffixBeforeSearching() {
        Book book = bookWithAuthor("Test Book, c. 2 (DVD)", "Test Author");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        assertTrue(msuLookupService.lookupAndUpdateBook(1L).isSuccess());

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        verify(msuRestTemplate).getForObject(urlCaptor.capture(), eq(String.class));
        String url = urlCaptor.getValue().toString().toLowerCase();
        assertTrue(url.contains("test%20book") || url.contains("test+book"));
        assertFalse(url.contains("dvd"));
    }

    @Test
    void lookupAndUpdateBook_withAlternateTitle_searchesBoth() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        book.setAlternateTitles(List.of("Other Title"));
        String other = """
                {"status":"OK","records":[
                  {"title":"Other Title","shortTitle":"Other Title","formats":["Vinyl Record"],
                   "authors":{"primary":{"Author, Test":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(NONE, other);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getAudioAvailable());
        assertFalse(result.getPaperAvailable());
    }

    @Test
    void lookupAndUpdateBook_tooManyRequests_backsOffThenSucceeds() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        List<Long> pauses = new ArrayList<>();
        MsuLookupService retrying = service(3, 2_000, 0, pauses::add);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(tooManyRequests("30"))
                .thenReturn(ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = retrying.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertNull(book.getMsuLookupError());
        verify(msuRestTemplate, times(2)).getForObject(any(URI.class), eq(String.class));
        assertEquals(1, pauses.size());
        assertClose(30_000, pauses.get(0));
    }

    @Test
    void lookupAndUpdateBook_tooManyRequests_exhaustedRetriesStoreStatusAndReserveNextWait() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        List<Long> pauses = new ArrayList<>();
        MsuLookupService retrying = service(2, 2_000, 0, pauses::add);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(tooManyRequests(null));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = retrying.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Error: HTTP 429 Too Many Requests after 3 attempts", result.getErrorMessage());
        assertEquals("Error: HTTP 429 Too Many Requests after 3 attempts", book.getMsuLookupError());
        verify(msuRestTemplate, times(3)).getForObject(any(URI.class), eq(String.class));
        assertEquals(2, pauses.size());
        assertClose(2_000, pauses.get(0));
        assertClose(4_000, pauses.get(1));

        pauses.clear();
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(ALL_FORMATS);
        assertTrue(retrying.lookupAndUpdateBook(1L).isSuccess());
        assertEquals(1, pauses.size(), "the next title must wait out the last backoff");
        assertClose(8_000, pauses.get(0));
    }

    @Test
    void lookupAndUpdateBook_ioTimeout_retriesThenSucceeds() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        List<Long> pauses = new ArrayList<>();
        MsuLookupService retrying = service(1, 2_000, 0, pauses::add);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Read timed out"))
                .thenReturn(ALL_FORMATS);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        assertTrue(retrying.lookupAndUpdateBook(1L).isSuccess());
        verify(msuRestTemplate, times(2)).getForObject(any(URI.class), eq(String.class));
        assertEquals(1, pauses.size());
        assertClose(2_000, pauses.get(0));
    }

    @Test
    void lookupAndUpdateBook_badRequest_doesNotRetryOrBackOff() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Error: HTTP 400 Bad Request", result.getErrorMessage());
        verify(msuRestTemplate, times(1)).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    void lookupAndUpdateBook_spacesSearchesForTheNextTitleCandidate() {
        Book book = bookWithAuthor("Crucial Conversations: Tools for Talking When Stakes Are High", "Kerry Patterson");
        List<Long> pauses = new ArrayList<>();
        MsuLookupService paced = service(0, 0, 1_000, pauses::add);
        String hit = """
                {"status":"OK","records":[
                  {"title":"Crucial Conversations","shortTitle":"Crucial Conversations","formats":["Book"],
                   "authors":{"primary":{"Patterson, Kerry":{"role":["author"]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(NONE, hit);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        assertTrue(paced.lookupAndUpdateBook(1L).isSuccess());
        assertEquals(1, pauses.size());
        assertClose(1_000, pauses.get(0));
    }

    @Test
    void backoffMs_growsAndHonorsRetryAfter() {
        assertEquals(0, CatalogLookupRetry.backoffMs(1, 0, null));
        assertEquals(2_000, CatalogLookupRetry.backoffMs(1, 2_000, null));
        assertEquals(4_000, CatalogLookupRetry.backoffMs(2, 2_000, null));
        assertEquals(8_000, CatalogLookupRetry.backoffMs(3, 2_000, null));
        assertEquals(16_000, CatalogLookupRetry.backoffMs(4, 2_000, null));
        assertEquals(32_000, CatalogLookupRetry.backoffMs(5, 2_000, null));
        assertEquals(60_000, CatalogLookupRetry.backoffMs(6, 2_000, null));
        assertEquals(30_000, CatalogLookupRetry.backoffMs(1, 2_000, tooManyRequests("30")));
        assertEquals(60_000, CatalogLookupRetry.backoffMs(1, 2_000, tooManyRequests("600")));
        assertEquals(2_000, CatalogLookupRetry.backoffMs(1, 2_000, tooManyRequests("0")));
        assertEquals(2_000, CatalogLookupRetry.backoffMs(1, 2_000, tooManyRequests("nope")));

        String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(20));
        long dated = CatalogLookupRetry.backoffMs(1, 1_000, tooManyRequests(httpDate));
        assertTrue(dated >= 18_000 && dated <= 20_000, "Retry-After date wait was " + dated);

        assertTrue(CatalogLookupRetry.isRetryableStatus(429));
        assertTrue(CatalogLookupRetry.isRetryableStatus(503));
        assertFalse(CatalogLookupRetry.isRetryableStatus(400));
        assertFalse(CatalogLookupRetry.isRetryableStatus(404));
    }

    private static void assertClose(long expectedMs, long actualMs) {
        assertTrue(Math.abs(expectedMs - actualMs) <= 250,
                "expected pause near " + expectedMs + " ms but was " + actualMs);
    }

    private static HttpClientErrorException tooManyRequests(String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfter != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers,
                new byte[0], StandardCharsets.UTF_8);
    }

    @Test
    void lookupAndUpdateBook_skipsARejectedRecordAndKeepsTheOther() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        book.setInvalidHoldings(List.of(new RejectedHolding(
                PublicLibrary.MSU, "drop-me", null, "different edition", null, null)));
        String body = """
                {"status":"OK","records":[
                  {"id":"keep-me","title":"Test Book","shortTitle":"Test Book","formats":["Book"],
                   "authors":{"primary":{"Author, Test":{"role":["author"]}}}},
                  {"id":"drop-me","title":"Test Book","shortTitle":"Test Book","formats":["Audio"],
                   "authors":{"primary":{"Author, Test":{"role":[]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertTrue(result.getPaperAvailable());
        assertFalse(result.getAudioAvailable());
        assertEquals("keep-me", result.getMatches().get(0).getRecordKey());
    }

    @Test
    void lookupAndUpdateBook_rejectedOnlyRecordIsNotHeld() {
        Book book = bookWithAuthor("Test Book", "Test Author");
        book.setMsuPaperAvailable(true);
        book.setInvalidHoldings(List.of(new RejectedHolding(
                PublicLibrary.MSU, "DROP-ME", null, "wrong book", null, null)));
        String body = """
                {"status":"OK","records":[
                  {"id":"drop-me","title":"Test Book","shortTitle":"Test Book","formats":["Book"],
                   "authors":{"primary":{"Author, Test":{"role":["author"]}}}}
                ]}
                """;
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(msuRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(body);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        MsuLookupResultDto result = msuLookupService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not held by Michigan State University", result.getErrorMessage());
        assertFalse(book.getMsuPaperAvailable());
    }

    private static Book bookWithAuthor(String title, String authorName) {
        Book book = new Book();
        book.setId(1L);
        book.setTitle(title);
        Author author = new Author();
        author.setName(authorName);
        book.setAuthor(author);
        return book;
    }
}
