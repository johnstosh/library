/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.DuplicateTitlePairDto;
import com.muczynski.library.dto.DuplicateTitlesResultDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.DuplicateTitleDismissalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The duplicate-title query and scan omit WITHDRAWN books.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DuplicateTitleScanIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private DuplicateTitleDismissalRepository dismissalRepository;

    @Autowired
    private DuplicateTitleService duplicateTitleService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scanOmitsWithdrawnBooks() {
        Author augustine = author("Augustine of Hippo");
        Author melville = author("Herman Melville");

        Book active = book("The Confessions of Augustine", BookStatus.ACTIVE, augustine);
        Book withdrawnNear = book("Confession of Augustine", BookStatus.WITHDRAWN, augustine);
        Book lostNear = book("Confessions of Augustine", BookStatus.LOST, augustine);
        Book withdrawnOnly = book("City of God", BookStatus.WITHDRAWN, augustine);
        Book nullStatus = book("Moby Dick", null, melville);
        bookRepository.flush();

        DuplicateTitlesResultDto result = duplicateTitleService.findDuplicates();

        assertEquals(3, result.getBooksScanned());
        assertEquals(3, result.getRepresentativesCompared());
        assertEquals(1, result.getPairs().size());

        DuplicateTitlePairDto pair = result.getPairs().get(0);
        Set<Long> pairIds = Set.of(pair.getBookAId(), pair.getBookBId());
        assertEquals(Set.of(active.getId(), lostNear.getId()), pairIds);

        Set<Long> scannedIds = bookRepository.findAllForDuplicateTitleScan().stream()
                .map(BookRepository.DuplicateTitleProjection::getId)
                .collect(Collectors.toSet());
        assertTrue(scannedIds.contains(active.getId()));
        assertTrue(scannedIds.contains(lostNear.getId()));
        assertTrue(scannedIds.contains(nullStatus.getId()));
        assertFalse(scannedIds.contains(withdrawnNear.getId()));
        assertFalse(scannedIds.contains(withdrawnOnly.getId()));
    }

    @Test
    void markedTitlesStayHiddenWhenAnotherCopyBecomesTheRepresentative() {
        Author augustine = author("Augustine");
        Author rousseau = author("Jean-Jacques Rousseau");

        Book kept = book("The Confessions of Augustine", BookStatus.ACTIVE, augustine);
        Book copy = book("The Confessions of Augustine, c. 2", BookStatus.ACTIVE, augustine);
        Book near = book("Confession of Augustine", BookStatus.LOST, augustine);
        Book otherA = book("The Confessions of Rousseau", BookStatus.ACTIVE, rousseau);
        Book otherB = book("Confession of Rousseau", BookStatus.LOST, rousseau);
        bookRepository.flush();

        assertTrue(pairsContain(duplicateTitleService.findDuplicates(), kept.getId(), near.getId()));
        assertFalse(pairsContain(duplicateTitleService.findDuplicates(), copy.getId(), near.getId()));
        assertTrue(pairsContain(duplicateTitleService.findDuplicates(), otherA.getId(), otherB.getId()));

        duplicateTitleService.dismiss(copy.getTitle(), augustine.getName(), near.getTitle(), augustine.getName());
        duplicateTitleService.dismiss(near.getTitle(), augustine.getName(), kept.getTitle(), augustine.getName());
        assertEquals(1, dismissalRepository.count());

        DuplicateTitlesResultDto hidden = duplicateTitleService.findDuplicates();
        assertFalse(pairsContain(hidden, kept.getId(), near.getId()));
        assertFalse(pairsContain(hidden, copy.getId(), near.getId()));
        assertTrue(pairsContain(hidden, otherA.getId(), otherB.getId()));

        kept.setStatus(BookStatus.WITHDRAWN);
        bookRepository.saveAndFlush(kept);

        DuplicateTitlesResultDto afterWithdraw = duplicateTitleService.findDuplicates();
        assertFalse(pairsContain(afterWithdraw, copy.getId(), near.getId()));
        assertTrue(pairsContain(afterWithdraw, otherA.getId(), otherB.getId()));

        duplicateTitleService.restore(kept.getTitle(), augustine.getName(), near.getTitle(), augustine.getName());
        assertEquals(0, dismissalRepository.count());

        DuplicateTitlesResultDto restored = duplicateTitleService.findDuplicates();
        assertTrue(pairsContain(restored, copy.getId(), near.getId()));
        assertFalse(pairsContain(restored, kept.getId(), near.getId()));
        assertTrue(pairsContain(restored, otherA.getId(), otherB.getId()));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void dismissAndUndoEndpointsRoundTrip() throws Exception {
        Author augustine = author("Augustine");
        Book kept = book("The Confessions of Augustine", BookStatus.ACTIVE, augustine);
        book("The Confessions of Augustine, c. 2", BookStatus.ACTIVE, augustine);
        Book near = book("Confession of Augustine", BookStatus.LOST, augustine);
        bookRepository.flush();

        String body = """
                {"titleA":"%s","authorA":"Augustine","titleB":"%s","authorB":"Augustine"}
                """.formatted(kept.getTitle().replace("\"", ""), near.getTitle());

        mockMvc.perform(post("/api/maintenance/duplicate-titles/dismissals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
        assertFalse(pairsContain(duplicateTitleService.findDuplicates(), kept.getId(), near.getId()));

        mockMvc.perform(post("/api/maintenance/duplicate-titles/dismissals/undo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
        assertTrue(pairsContain(duplicateTitleService.findDuplicates(), kept.getId(), near.getId()));
    }

    @Test
    @WithMockUser(authorities = "LIBRARIAN")
    void blankTitleDismissalIsRejected() throws Exception {
        mockMvc.perform(post("/api/maintenance/duplicate-titles/dismissals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleA\":\"   \",\"authorA\":\"Augustine\",\"titleB\":\"Confession\",\"authorB\":\"Augustine\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(0, dismissalRepository.count());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void userCannotMarkAPair() throws Exception {
        mockMvc.perform(post("/api/maintenance/duplicate-titles/dismissals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleA\":\"Confessions\",\"authorA\":\"Augustine\",\"titleB\":\"Confession\",\"authorB\":\"Augustine\"}"))
                .andExpect(status().isForbidden());
        assertEquals(0, dismissalRepository.count());
    }

    private static boolean pairsContain(DuplicateTitlesResultDto result, Long leftId, Long rightId) {
        Set<Long> expected = Set.of(leftId, rightId);
        return result.getPairs().stream()
                .anyMatch(pair -> expected.equals(Set.of(pair.getBookAId(), pair.getBookBId())));
    }

    private Author author(String name) {
        Author author = new Author();
        author.setName(name);
        return authorRepository.save(author);
    }

    private Book book(String title, BookStatus status, Author author) {
        Book book = new Book();
        book.setTitle(title);
        book.setStatus(status);
        book.setAuthor(author);
        return bookRepository.save(book);
    }
}
