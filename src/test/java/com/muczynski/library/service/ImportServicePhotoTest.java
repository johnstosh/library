/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.*;
import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.dto.importdtos.*;
import com.muczynski.library.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ImportService photo import functionality
 * Tests that photos with permanentId are correctly merged during import
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportServicePhotoTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private EntityManager entityManager;

    private Library testLibrary;
    private Author testAuthor;
    private Book testBook;

    @BeforeEach
    void setUp() {
        // Create test library
        testLibrary = new Library();
        testLibrary.setName("Test Library");
        testLibrary.setHostname("test.library.com");
        testLibrary = libraryRepository.save(testLibrary);

        // Create test author
        testAuthor = new Author();
        testAuthor.setName("Test Author");
        testAuthor = authorRepository.save(testAuthor);

        // Create test book
        testBook = new Book();
        testBook.setTitle("Test Book");
        testBook.setAuthor(testAuthor);
        testBook.setLibrary(testLibrary);
        testBook.setStatus(BookStatus.ACTIVE);
        testBook = bookRepository.save(testBook);
    }

    // @AfterEach not needed - @Transactional on class handles rollback
    // @AfterEach
    // void tearDown() {
    //     photoRepository.deleteAll();
    //     bookRepository.deleteAll();
    //     authorRepository.deleteAll();
    //     libraryRepository.deleteAll();
    // }

    @Test
    void testImportPhoto_WithPermanentId_MergesExistingPhoto() {
        // Arrange: Create an existing photo with a permanentId
        Photo existingPhoto = new Photo();
        existingPhoto.setBook(testBook);
        existingPhoto.setPermanentId("AGyMBsO0HwfW3DeusG-H-existing");
        existingPhoto.setCaption("Old Caption");
        existingPhoto.setPhotoOrder(1);
        existingPhoto.setExportStatus(Photo.ExportStatus.COMPLETED);
        existingPhoto = photoRepository.save(existingPhoto);
        Long existingPhotoId = existingPhoto.getId();

        // Act: Import a photo with the same permanentId but different caption
        ImportRequestDto importDto = new ImportRequestDto();

        // Add library
        LibraryDto libraryDto = new LibraryDto();
        libraryDto.setName(testLibrary.getName());
        libraryDto.setHostname(testLibrary.getHostname());
        importDto.setLibraries(List.of(libraryDto));

        // Add author
        ImportAuthorDto authorDto = new ImportAuthorDto();
        authorDto.setName(testAuthor.getName());
        importDto.setAuthors(List.of(authorDto));

        // Add book
        ImportBookDto bookDto = new ImportBookDto();
        bookDto.setTitle(testBook.getTitle());
        bookDto.setLibraryName(testLibrary.getName());
        ImportAuthorDto bookAuthorDto = new ImportAuthorDto();
        bookAuthorDto.setName(testAuthor.getName());
        bookDto.setAuthor(bookAuthorDto);
        bookDto.setStatus(BookStatus.ACTIVE);
        importDto.setBooks(List.of(bookDto));

        // Add photo with same permanentId but different caption
        ImportPhotoDto photoDto = new ImportPhotoDto();
        photoDto.setBookTitle(testBook.getTitle());
        photoDto.setBookAuthorName(testAuthor.getName());
        photoDto.setPermanentId("AGyMBsO0HwfW3DeusG-H-existing"); // Same permanentId
        photoDto.setCaption("New Caption from Import"); // Different caption
        photoDto.setPhotoOrder(1);
        photoDto.setContentType("image/jpeg");
        photoDto.setExportStatus(Photo.ExportStatus.COMPLETED);
        importDto.setPhotos(List.of(photoDto));

        importService.importData(importDto);

        // Assert: Should have merged (updated) the existing photo, not created a new one
        Optional<Photo> updatedPhotoOpt = photoRepository.findById(existingPhotoId);
        assertTrue(updatedPhotoOpt.isPresent(), "Existing photo should still exist");

        Photo updatedPhoto = updatedPhotoOpt.get();
        assertEquals("New Caption from Import", updatedPhoto.getCaption(), "Caption should be updated");
        assertEquals("AGyMBsO0HwfW3DeusG-H-existing", updatedPhoto.getPermanentId(), "PermanentId should remain same");
        assertEquals(existingPhotoId, updatedPhoto.getId(), "Photo ID should not change (same photo)");

        // Verify only one photo exists with this permanentId
        Optional<Photo> photoByPermanentId = photoRepository.findByPermanentId("AGyMBsO0HwfW3DeusG-H-existing");
        assertTrue(photoByPermanentId.isPresent());
        assertEquals(existingPhotoId, photoByPermanentId.get().getId());

        // Verify total photo count - should still be 1
        long totalPhotos = photoRepository.count();
        assertEquals(1, totalPhotos, "Should only have one photo, not a duplicate");
    }

    @Test
    void testImportPhoto_WithNewPermanentId_CreatesNewPhoto() {
        // Act: Import a photo with a new permanentId
        ImportRequestDto importDto = new ImportRequestDto();

        // Add library
        LibraryDto libraryDto = new LibraryDto();
        libraryDto.setName(testLibrary.getName());
        libraryDto.setHostname(testLibrary.getHostname());
        importDto.setLibraries(List.of(libraryDto));

        // Add author
        ImportAuthorDto authorDto = new ImportAuthorDto();
        authorDto.setName(testAuthor.getName());
        importDto.setAuthors(List.of(authorDto));

        // Add book
        ImportBookDto bookDto = new ImportBookDto();
        bookDto.setTitle(testBook.getTitle());
        bookDto.setLibraryName(testLibrary.getName());
        ImportAuthorDto bookAuthorDto = new ImportAuthorDto();
        bookAuthorDto.setName(testAuthor.getName());
        bookDto.setAuthor(bookAuthorDto);
        bookDto.setStatus(BookStatus.ACTIVE);
        importDto.setBooks(List.of(bookDto));

        // Add photo with new permanentId
        ImportPhotoDto photoDto = new ImportPhotoDto();
        photoDto.setBookTitle(testBook.getTitle());
        photoDto.setBookAuthorName(testAuthor.getName());
        photoDto.setPermanentId("AGyMBsO0HwfW3DeusG-H-newphoto");
        photoDto.setCaption("New Photo Caption");
        photoDto.setPhotoOrder(2);
        photoDto.setContentType("image/jpeg");
        photoDto.setExportStatus(Photo.ExportStatus.COMPLETED);
        importDto.setPhotos(List.of(photoDto));

        importService.importData(importDto);

        // Assert: Should have created a new photo
        Optional<Photo> newPhotoOpt = photoRepository.findByPermanentId("AGyMBsO0HwfW3DeusG-H-newphoto");
        assertTrue(newPhotoOpt.isPresent(), "New photo should be created");

        Photo newPhoto = newPhotoOpt.get();
        assertEquals("New Photo Caption", newPhoto.getCaption());
        assertEquals("AGyMBsO0HwfW3DeusG-H-newphoto", newPhoto.getPermanentId());
        assertEquals(Integer.valueOf(2), newPhoto.getPhotoOrder());

        // Verify total photo count
        long totalPhotos = photoRepository.count();
        assertEquals(1, totalPhotos);
    }

    @Test
    void testImportPhoto_WithoutPermanentId_MatchesByBookAndOrder() {
        // Arrange: Create an existing photo without permanentId
        Photo existingPhoto = new Photo();
        existingPhoto.setBook(testBook);
        existingPhoto.setCaption("Old Caption");
        existingPhoto.setPhotoOrder(3);
        existingPhoto = photoRepository.save(existingPhoto);
        Long existingPhotoId = existingPhoto.getId();

        // Act: Import a photo without permanentId, matching by book + order
        ImportRequestDto importDto = new ImportRequestDto();

        // Add library
        LibraryDto libraryDto = new LibraryDto();
        libraryDto.setName(testLibrary.getName());
        libraryDto.setHostname(testLibrary.getHostname());
        importDto.setLibraries(List.of(libraryDto));

        // Add author
        ImportAuthorDto authorDto = new ImportAuthorDto();
        authorDto.setName(testAuthor.getName());
        importDto.setAuthors(List.of(authorDto));

        // Add book
        ImportBookDto bookDto = new ImportBookDto();
        bookDto.setTitle(testBook.getTitle());
        bookDto.setLibraryName(testLibrary.getName());
        ImportAuthorDto bookAuthorDto = new ImportAuthorDto();
        bookAuthorDto.setName(testAuthor.getName());
        bookDto.setAuthor(bookAuthorDto);
        bookDto.setStatus(BookStatus.ACTIVE);
        importDto.setBooks(List.of(bookDto));

        // Add photo without permanentId, same book and order
        ImportPhotoDto photoDto = new ImportPhotoDto();
        photoDto.setBookTitle(testBook.getTitle());
        photoDto.setBookAuthorName(testAuthor.getName());
        photoDto.setCaption("Updated Caption");
        photoDto.setPhotoOrder(3); // Same order as existing
        photoDto.setContentType("image/jpeg");
        importDto.setPhotos(List.of(photoDto));

        importService.importData(importDto);

        // Assert: Should have merged the existing photo by book + order
        Optional<Photo> updatedPhotoOpt = photoRepository.findById(existingPhotoId);
        assertTrue(updatedPhotoOpt.isPresent(), "Existing photo should still exist");

        Photo updatedPhoto = updatedPhotoOpt.get();
        assertEquals("Updated Caption", updatedPhoto.getCaption(), "Caption should be updated");
        assertEquals(existingPhotoId, updatedPhoto.getId(), "Photo ID should not change");

        // Verify total photo count - should still be 1
        long totalPhotos = photoRepository.count();
        assertEquals(1, totalPhotos, "Should only have one photo, not a duplicate");
    }

    @Test
    void testImportPhoto_UnlinkAndReimport_MergesByPermanentId() {
        // This test simulates the user's scenario:
        // 1. Export photo with permanentId
        // 2. Unlink photo on target server (permanentId removed)
        // 3. Import again - should find by book+order, then update permanentId

        // Arrange: Create a photo that was "unlinked" (has no permanentId)
        Photo unlinkedPhoto = new Photo();
        unlinkedPhoto.setBook(testBook);
        unlinkedPhoto.setCaption("Unlinked Photo");
        unlinkedPhoto.setPhotoOrder(1);
        unlinkedPhoto = photoRepository.save(unlinkedPhoto);
        Long unlinkedPhotoId = unlinkedPhoto.getId();

        // Act: Import with a permanentId for the same book+order
        ImportRequestDto importDto = new ImportRequestDto();

        // Add library
        LibraryDto libraryDto = new LibraryDto();
        libraryDto.setName(testLibrary.getName());
        libraryDto.setHostname(testLibrary.getHostname());
        importDto.setLibraries(List.of(libraryDto));

        // Add author
        ImportAuthorDto authorDto = new ImportAuthorDto();
        authorDto.setName(testAuthor.getName());
        importDto.setAuthors(List.of(authorDto));

        // Add book
        ImportBookDto bookDto = new ImportBookDto();
        bookDto.setTitle(testBook.getTitle());
        bookDto.setLibraryName(testLibrary.getName());
        ImportAuthorDto bookAuthorDto = new ImportAuthorDto();
        bookAuthorDto.setName(testAuthor.getName());
        bookDto.setAuthor(bookAuthorDto);
        bookDto.setStatus(BookStatus.ACTIVE);
        importDto.setBooks(List.of(bookDto));

        // Add photo with permanentId
        ImportPhotoDto photoDto = new ImportPhotoDto();
        photoDto.setBookTitle(testBook.getTitle());
        photoDto.setBookAuthorName(testAuthor.getName());
        photoDto.setPermanentId("AGyMBsO0HwfW3DeusG-H-reimport");
        photoDto.setCaption("Reimported with PermanentId");
        photoDto.setPhotoOrder(1);
        photoDto.setContentType("image/jpeg");
        photoDto.setExportStatus(Photo.ExportStatus.COMPLETED);
        importDto.setPhotos(List.of(photoDto));

        importService.importData(importDto);

        // Assert: Should have updated the unlinked photo with the new permanentId
        Optional<Photo> updatedPhotoOpt = photoRepository.findById(unlinkedPhotoId);
        assertTrue(updatedPhotoOpt.isPresent(), "Original photo should still exist");

        Photo updatedPhoto = updatedPhotoOpt.get();
        assertEquals("AGyMBsO0HwfW3DeusG-H-reimport", updatedPhoto.getPermanentId(), "PermanentId should be set");
        assertEquals("Reimported with PermanentId", updatedPhoto.getCaption(), "Caption should be updated");
        assertEquals(unlinkedPhotoId, updatedPhoto.getId(), "Photo ID should not change");

        // Verify we can find it by permanentId now
        Optional<Photo> byPermanentId = photoRepository.findByPermanentId("AGyMBsO0HwfW3DeusG-H-reimport");
        assertTrue(byPermanentId.isPresent());
        assertEquals(unlinkedPhotoId, byPermanentId.get().getId());

        // Verify total photo count - should still be 1
        long totalPhotos = photoRepository.count();
        assertEquals(1, totalPhotos, "Should only have one photo, not a duplicate");
    }

    @Test
    void testUserScenario_TwoPhotos_SecondPhotoNeedsPermanentId() {
        // User's exact scenario:
        // Book has 2 photos
        // Photo 1 (order 0): has permanentId already
        // Photo 2 (order 1): does NOT have permanentId
        // Export from source server includes both photos with permanentIds
        // Import to target server should update Photo 2's permanentId

        // Arrange: Create two photos on the target server
        Photo photo1 = new Photo();
        photo1.setBook(testBook);
        photo1.setCaption("First Photo");
        photo1.setPhotoOrder(0);
        photo1.setPermanentId("AGyMBsO0HwfW3DeusG-H-photo1-existing");
        photo1.setExportStatus(Photo.ExportStatus.COMPLETED);
        photo1 = photoRepository.save(photo1);
        Long photo1Id = photo1.getId();

        Photo photo2 = new Photo();
        photo2.setBook(testBook);
        photo2.setCaption("Second Photo - No Permanent ID");
        photo2.setPhotoOrder(1);
        photo2.setPermanentId(null); // This one doesn't have a permanentId yet
        photo2 = photoRepository.save(photo2);
        Long photo2Id = photo2.getId();

        System.out.println("BEFORE IMPORT:");
        System.out.println("  Photo 1 (ID=" + photo1Id + ", order=0): permanentId=" + photo1.getPermanentId());
        System.out.println("  Photo 2 (ID=" + photo2Id + ", order=1): permanentId=" + photo2.getPermanentId());

        // Act: Import both photos from source server, both have permanentIds
        ImportRequestDto importDto = new ImportRequestDto();

        // Add library
        LibraryDto libraryDto = new LibraryDto();
        libraryDto.setName(testLibrary.getName());
        libraryDto.setHostname(testLibrary.getHostname());
        importDto.setLibraries(List.of(libraryDto));

        // Add author
        ImportAuthorDto authorDto = new ImportAuthorDto();
        authorDto.setName(testAuthor.getName());
        importDto.setAuthors(List.of(authorDto));

        // Add book
        ImportBookDto bookDto = new ImportBookDto();
        bookDto.setTitle(testBook.getTitle());
        bookDto.setLibraryName(testLibrary.getName());
        ImportAuthorDto bookAuthorDto = new ImportAuthorDto();
        bookAuthorDto.setName(testAuthor.getName());
        bookDto.setAuthor(bookAuthorDto);
        bookDto.setStatus(BookStatus.ACTIVE);
        importDto.setBooks(List.of(bookDto));

        // Add photo 1 (already has correct permanentId)
        ImportPhotoDto photoDto1 = new ImportPhotoDto();
        photoDto1.setBookTitle(testBook.getTitle());
        photoDto1.setBookAuthorName(testAuthor.getName());
        photoDto1.setPermanentId("AGyMBsO0HwfW3DeusG-H-photo1-existing");
        photoDto1.setCaption("First Photo Updated");
        photoDto1.setPhotoOrder(0);
        photoDto1.setContentType("image/jpeg");
        photoDto1.setExportStatus(Photo.ExportStatus.COMPLETED);

        // Add photo 2 (needs permanentId to be updated)
        ImportPhotoDto photoDto2 = new ImportPhotoDto();
        photoDto2.setBookTitle(testBook.getTitle());
        photoDto2.setBookAuthorName(testAuthor.getName());
        photoDto2.setPermanentId("AGyMBsO0HwfW3DeusG-H-photo2-NEW"); // This is the new permanentId
        photoDto2.setCaption("Second Photo with Permanent ID");
        photoDto2.setPhotoOrder(1);
        photoDto2.setContentType("image/jpeg");
        photoDto2.setExportStatus(Photo.ExportStatus.COMPLETED);

        importDto.setPhotos(List.of(photoDto1, photoDto2));

        importService.importData(importDto);

        // Assert: Verify total photo count - should still be 2
        long totalPhotos = photoRepository.count();
        assertEquals(2, totalPhotos, "Should still have exactly 2 photos, not create duplicates");

        // Verify Photo 1 unchanged
        Optional<Photo> updatedPhoto1Opt = photoRepository.findById(photo1Id);
        assertTrue(updatedPhoto1Opt.isPresent(), "Photo 1 should still exist");
        Photo updatedPhoto1 = updatedPhoto1Opt.get();
        assertEquals("AGyMBsO0HwfW3DeusG-H-photo1-existing", updatedPhoto1.getPermanentId(), "Photo 1 permanentId should be unchanged");
        assertEquals("First Photo Updated", updatedPhoto1.getCaption(), "Photo 1 caption should be updated");

        // Verify Photo 2 got its permanentId updated
        Optional<Photo> updatedPhoto2Opt = photoRepository.findById(photo2Id);
        assertTrue(updatedPhoto2Opt.isPresent(), "Photo 2 should still exist");
        Photo updatedPhoto2 = updatedPhoto2Opt.get();

        System.out.println("\nAFTER IMPORT:");
        System.out.println("  Photo 1 (ID=" + updatedPhoto1.getId() + ", order=0): permanentId=" + updatedPhoto1.getPermanentId());
        System.out.println("  Photo 2 (ID=" + updatedPhoto2.getId() + ", order=1): permanentId=" + updatedPhoto2.getPermanentId());

        assertEquals("AGyMBsO0HwfW3DeusG-H-photo2-NEW", updatedPhoto2.getPermanentId(),
            "Photo 2 permanentId should be updated to the imported value");
        assertEquals("Second Photo with Permanent ID", updatedPhoto2.getCaption(), "Photo 2 caption should be updated");
        assertEquals(photo2Id, updatedPhoto2.getId(), "Photo 2 ID should not change");

        // Verify we can find both photos by their permanentIds
        Optional<Photo> byPermanentId1 = photoRepository.findByPermanentId("AGyMBsO0HwfW3DeusG-H-photo1-existing");
        assertTrue(byPermanentId1.isPresent());
        assertEquals(photo1Id, byPermanentId1.get().getId());

        Optional<Photo> byPermanentId2 = photoRepository.findByPermanentId("AGyMBsO0HwfW3DeusG-H-photo2-NEW");
        assertTrue(byPermanentId2.isPresent(), "Should be able to find Photo 2 by its new permanentId");
        assertEquals(photo2Id, byPermanentId2.get().getId(), "Photo 2 found by permanentId should be the same entity");
    }

    @Test
    void testUserScenario_PhotoExportFormat_BothPhotosExported() {
        // This test simulates the EXACT photo export format:
        // When you export photos, the export includes ALL photos for a book
        // Target server might have photos WITHOUT permanentIds
        // Source server exports photos WITH permanentIds
        // We need to match by book+order when the permanentId doesn't exist on target

        // Arrange: Target server has 2 photos, only first one has permanentId
        Photo photo1 = new Photo();
        photo1.setBook(testBook);
        photo1.setCaption("Photo 1");
        photo1.setPhotoOrder(0);
        photo1.setPermanentId("AGyMBsO0HwfW3DeusG-H-FIRST");
        photo1.setExportStatus(Photo.ExportStatus.COMPLETED);
        photo1 = photoRepository.save(photo1);
        Long photo1Id = photo1.getId();

        Photo photo2 = new Photo();
        photo2.setBook(testBook);
        photo2.setCaption("Photo 2 - Missing PermanentId");
        photo2.setPhotoOrder(1);
        // Photo 2 has NO permanentId
        photo2 = photoRepository.save(photo2);
        Long photo2Id = photo2.getId();

        System.out.println("\n=== BEFORE IMPORT ===");
        System.out.println("Photo 1 (ID=" + photo1Id + ", order=0): permanentId=" + photo1.getPermanentId());
        System.out.println("Photo 2 (ID=" + photo2Id + ", order=1): permanentId=" + photo2.getPermanentId());
        System.out.println("Total photos: " + photoRepository.count());

        // Act: Now import from source server where BOTH photos have permanentIds
        ImportRequestDto importDto = new ImportRequestDto();

        LibraryDto libraryDto = new LibraryDto();
        libraryDto.setName(testLibrary.getName());
        libraryDto.setHostname(testLibrary.getHostname());
        importDto.setLibraries(List.of(libraryDto));

        ImportAuthorDto authorDto = new ImportAuthorDto();
        authorDto.setName(testAuthor.getName());
        importDto.setAuthors(List.of(authorDto));

        ImportBookDto bookDto = new ImportBookDto();
        bookDto.setTitle(testBook.getTitle());
        bookDto.setLibraryName(testLibrary.getName());
        ImportAuthorDto bookAuthorDto = new ImportAuthorDto();
        bookAuthorDto.setName(testAuthor.getName());
        bookDto.setAuthor(bookAuthorDto);
        bookDto.setStatus(BookStatus.ACTIVE);
        importDto.setBooks(List.of(bookDto));

        // Import Photo 1 - has permanentId that matches existing
        ImportPhotoDto photoDto1 = new ImportPhotoDto();
        photoDto1.setBookTitle(testBook.getTitle());
        photoDto1.setBookAuthorName(testAuthor.getName());
        photoDto1.setPermanentId("AGyMBsO0HwfW3DeusG-H-FIRST"); // Matches existing
        photoDto1.setCaption("Photo 1 Updated");
        photoDto1.setPhotoOrder(0);
        photoDto1.setContentType("image/jpeg");

        // Import Photo 2 - has NEW permanentId (from source server export)
        ImportPhotoDto photoDto2 = new ImportPhotoDto();
        photoDto2.setBookTitle(testBook.getTitle());
        photoDto2.setBookAuthorName(testAuthor.getName());
        photoDto2.setPermanentId("AGyMBsO0HwfW3DeusG-H-SECOND"); // NEW permanentId
        photoDto2.setCaption("Photo 2 with new PermanentId");
        photoDto2.setPhotoOrder(1);
        photoDto2.setContentType("image/jpeg");

        importDto.setPhotos(List.of(photoDto1, photoDto2));

        System.out.println("\n=== IMPORTING ===");
        System.out.println("Importing Photo 1: permanentId=" + photoDto1.getPermanentId() + ", order=0");
        System.out.println("Importing Photo 2: permanentId=" + photoDto2.getPermanentId() + ", order=1");

        importService.importData(importDto);

        System.out.println("\n=== AFTER IMPORT ===");

        // Check all photos in database
        List<Photo> allPhotos = photoRepository.findAll();
        System.out.println("Total photos after import: " + allPhotos.size());
        for (Photo p : allPhotos) {
            System.out.println("  Photo ID=" + p.getId() + ", order=" + p.getPhotoOrder() +
                             ", permanentId=" + p.getPermanentId() + ", caption=" + p.getCaption());
        }

        // Assert: Should still have exactly 2 photos
        assertEquals(2, photoRepository.count(), "Should still have exactly 2 photos");

        // Assert: Photo 2 should have its permanentId updated
        Photo updatedPhoto2 = photoRepository.findById(photo2Id).orElseThrow();
        assertEquals("AGyMBsO0HwfW3DeusG-H-SECOND", updatedPhoto2.getPermanentId(),
            "Photo 2 should have the imported permanentId");
        assertEquals(photo2Id, updatedPhoto2.getId(), "Photo 2 should be the same entity (not a new one)");

        // Assert: Photo 1 should be unchanged
        Photo updatedPhoto1 = photoRepository.findById(photo1Id).orElseThrow();
        assertEquals("AGyMBsO0HwfW3DeusG-H-FIRST", updatedPhoto1.getPermanentId(),
            "Photo 1 should keep its permanentId");
    }

    @Test
    void testRealUserScenario_ExportImportWithPermanentIds() {
        // User's EXACT scenario:
        // 1. Source server: Create book with 2 photos, export to Google Photos (both get permanentIds)
        // 2. Source server: Export JSON with photo metadata
        // 3. Target server: Import JSON FIRST TIME - photo 2 has NO permanentId yet (this works)
        // 4. Source server: Photo 2 gets exported to Google Photos, gets a permanentId
        // 5. Source server: Export JSON again (now photo 2 has permanentId)
        // 6. Target server: Import JSON SECOND TIME - photo 2's permanentId should be merged (THIS IS THE BUG)

        System.out.println("\n=== SOURCE SERVER: Create book with 2 photos ===");
        Photo photo1 = new Photo();
        photo1.setBook(testBook);
        photo1.setCaption("Photo 1");
        photo1.setPhotoOrder(0);
        photo1.setPermanentId("PERM-ID-1");
        photo1.setExportStatus(Photo.ExportStatus.COMPLETED);
        photo1 = photoRepository.save(photo1);

        Photo photo2 = new Photo();
        photo2.setBook(testBook);
        photo2.setCaption("Photo 2");
        photo2.setPhotoOrder(1);
        // Photo 2 does NOT have permanentId yet on source server
        photo2 = photoRepository.save(photo2);

        System.out.println("  Photo 1 permanentId=" + photo1.getPermanentId());
        System.out.println("  Photo 2 permanentId=" + photo2.getPermanentId() + " (null - not exported yet)");

        // FIRST EXPORT from source server
        System.out.println("\n=== SOURCE SERVER: First JSON Export (photo 2 has no permanentId) ===");
        entityManager.flush();
        ImportRequestDto firstExport = importService.exportData();

        System.out.println("  Exported photos: " + firstExport.getPhotos().size());
        for (ImportPhotoDto p : firstExport.getPhotos()) {
            System.out.println("    Photo: order=" + p.getPhotoOrder() +
                             ", permanentId=" + p.getPermanentId() +
                             ", caption=" + p.getCaption());
        }

        // Simulate target server: Import data WITHOUT photo 2's permanentId
        System.out.println("\n=== TARGET SERVER: First Import (photo 2 has no permanentId) ===");
        importService.importData(firstExport);

        // Photo 2 on target server should NOT have permanentId yet (because source didn't have it)
        List<Photo> photosAfterFirstImport = photoRepository.findAll();
        System.out.println("  Photos after first import: " + photosAfterFirstImport.size());
        for (Photo p : photosAfterFirstImport) {
            System.out.println("    Photo ID=" + p.getId() + ", order=" + p.getPhotoOrder() +
                             ", permanentId=" + p.getPermanentId());
        }

        // Photo 2 should still be the same entity with same ID
        Photo targetPhoto2 = photoRepository.findById(photo2.getId()).orElseThrow();
        Long targetPhoto2Id = targetPhoto2.getId();

        assertNull(targetPhoto2.getPermanentId(), "Photo 2 should not have permanentId after first import");

        // BACK TO SOURCE SERVER: Photo 2 gets exported to Google Photos
        System.out.println("\n=== SOURCE SERVER: Photo 2 exported to Google Photos ===");
        photo2.setPermanentId("PERM-ID-2");
        photo2.setExportStatus(Photo.ExportStatus.COMPLETED);
        photo2 = photoRepository.save(photo2);

        System.out.println("  Photo 2 now has permanentId=" + photo2.getPermanentId());

        // SECOND EXPORT from source server
        System.out.println("\n=== SOURCE SERVER: Second JSON Export (photo 2 NOW has permanentId) ===");
        entityManager.flush();
        ImportRequestDto secondExport = importService.exportData();

        System.out.println("  Exported photos: " + secondExport.getPhotos().size());
        for (ImportPhotoDto p : secondExport.getPhotos()) {
            System.out.println("    Photo: order=" + p.getPhotoOrder() +
                             ", permanentId=" + p.getPermanentId() +
                             ", caption=" + p.getCaption());
        }

        // TARGET SERVER: Simulate that photo 2 still doesn't have permanentId
        // (On real target server, it wouldn't have received the permanentId yet)
        System.out.println("\n=== TARGET SERVER: Before Second Import ===");
        targetPhoto2 = photoRepository.findById(targetPhoto2Id).orElseThrow();
        targetPhoto2.setPermanentId(null); // Erase permanentId to simulate target server state
        targetPhoto2 = photoRepository.save(targetPhoto2);
        System.out.println("  Photo 2 permanentId=" + targetPhoto2.getPermanentId() + " (erased to simulate target server)");
        assertNull(targetPhoto2.getPermanentId(), "Photo 2 should not have permanentId before second import");
        entityManager.flush();

        // SECOND IMPORT on target server (THE BUG TEST)
        System.out.println("\n=== TARGET SERVER: Second Import (should merge permanentId) ===");
        importService.importData(secondExport);
        System.out.println("\n=== TARGET SERVER: Second Import complete ===");

        Photo updatedPhoto2 = photoRepository.findById(targetPhoto2Id).orElseThrow();
        System.out.println("  After: Photo 2 permanentId=" + updatedPhoto2.getPermanentId());

        // ASSERT: Photo 2's permanentId should be updated
        assertEquals("PERM-ID-2", updatedPhoto2.getPermanentId(),
            "Photo 2's permanentId should be merged from the second import");
        assertEquals(targetPhoto2Id, updatedPhoto2.getId(),
            "Photo 2 should be the same entity (not a new one)");

        // Verify total photo count didn't change
        assertEquals(2, photoRepository.count(), "Should still have exactly 2 photos");
    }
}
