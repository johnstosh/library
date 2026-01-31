/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.*;
import com.muczynski.library.dto.DatabaseStatsDto;
import com.muczynski.library.dto.BranchDto;
import com.muczynski.library.dto.importdtos.*;
import com.muczynski.library.mapper.BranchMapper;
import com.muczynski.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

    public static final String DEFAULT_PASSWORD = "divinemercy";

    private final BranchRepository branchRepository;
    private final AuthorRepository authorRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final AuthorityRepository authorityRepository;
    private final PhotoRepository photoRepository;
    private final BranchMapper branchMapper;
    private final PasswordEncoder passwordEncoder;

    public ImportResponseDto.ImportCounts importData(ImportRequestDto dto) {
        logger.info("Starting import. Libraries: {}, Authors: {}, Users: {}, Books: {}, Loans: {}, Photos: {}",
            dto.getBranches() != null ? dto.getBranches().size() : 0,
            dto.getAuthors() != null ? dto.getAuthors().size() : 0,
            dto.getUsers() != null ? dto.getUsers().size() : 0,
            dto.getBooks() != null ? dto.getBooks().size() : 0,
            dto.getLoans() != null ? dto.getLoans().size() : 0,
            dto.getPhotos() != null ? dto.getPhotos().size() : 0);

        int libraryCount = 0;
        int authorCount = 0;
        int userCount = 0;
        int bookCount = 0;
        int loanCount = 0;
        int photoCount = 0;

        Map<String, Library> libMap = new HashMap<>();
        if (dto.getBranches() != null) {
            for (BranchDto lDto : dto.getBranches()) {
                // Check if library with same branch name already exists (select first by ID if duplicates)
                List<Library> existingLibraries = branchRepository.findAllByBranchNameOrderByIdAsc(lDto.getBranchName());
                Library lib = existingLibraries.isEmpty() ? null : existingLibraries.get(0);
                if (lib == null) {
                    // Create new library without copying ID from import
                    lib = new Library();
                    lib.setBranchName(lDto.getBranchName());
                    lib.setLibrarySystemName(lDto.getLibrarySystemName());
                } else {
                    // Update existing library
                    lib.setLibrarySystemName(lDto.getLibrarySystemName());

                    // Merge duplicates: reassign books from duplicate branches to primary and delete duplicates
                    if (existingLibraries.size() > 1) {
                        logger.info("Merging {} duplicate libraries with branch name '{}' into library ID: {}",
                                   existingLibraries.size(), lDto.getBranchName(), lib.getId());

                        for (int i = 1; i < existingLibraries.size(); i++) {
                            Library duplicate = existingLibraries.get(i);
                            // Reassign all books from duplicate to primary library
                            List<Book> booksToReassign = bookRepository.findAllByLibraryId(duplicate.getId());
                            for (Book book : booksToReassign) {
                                book.setLibrary(lib);
                                bookRepository.save(book);
                                logger.debug("Reassigned book '{}' (ID: {}) from library {} to library {}",
                                           book.getTitle(), book.getId(), duplicate.getId(), lib.getId());
                            }
                            logger.info("Reassigned {} books from duplicate library ID {} to primary library ID {}",
                                       booksToReassign.size(), duplicate.getId(), lib.getId());

                            // Delete the duplicate library
                            branchRepository.delete(duplicate);
                            logger.info("Deleted duplicate library ID {} (branch name: '{}')",
                                       duplicate.getId(), duplicate.getBranchName());
                        }
                    }
                }
                lib = branchRepository.save(lib);
                libMap.put(lDto.getBranchName(), lib);
                libraryCount++;
            }
        }
        logger.info("Imported {} libraries", libraryCount);

        Map<String, Author> authMap = new HashMap<>();
        if (dto.getAuthors() != null) {
            for (ImportAuthorDto aDto : dto.getAuthors()) {
                // Check if author with same name already exists (select first by ID if duplicates)
                List<Author> existingAuthors = authorRepository.findAllByNameOrderByIdAsc(aDto.getName());
                Author auth = existingAuthors.isEmpty() ? null : existingAuthors.get(0);
                if (auth == null) {
                    auth = new Author();
                    auth.setName(aDto.getName());
                }
                // Update fields (merge)
                auth.setDateOfBirth(aDto.getDateOfBirth());
                auth.setDateOfDeath(aDto.getDateOfDeath());
                auth.setReligiousAffiliation(aDto.getReligiousAffiliation());
                auth.setBirthCountry(aDto.getBirthCountry());
                auth.setNationality(aDto.getNationality());
                auth.setBriefBiography(aDto.getBriefBiography());
                auth.setGrokipediaUrl(aDto.getGrokipediaUrl());
                auth = authorRepository.save(auth);
                authMap.put(aDto.getName(), auth);
                authorCount++;
            }
        }
        logger.info("Imported {} authors", authorCount);

        Map<String, User> userMap = new HashMap<>();
        if (dto.getUsers() != null) {
            for (ImportUserDto uDto : dto.getUsers()) {
                // Check if user with same username already exists (case-insensitive)
                List<User> existingUsers = userRepository.findAllByUsernameIgnoreCaseOrderByIdAsc(uDto.getUsername());
                User user;
                if (!existingUsers.isEmpty()) {
                    user = existingUsers.get(0); // Use existing user with lowest ID
                } else {
                    user = new User();
                    user.setUserIdentifier(UUID.randomUUID().toString()); // Generate unique identifier
                    user.setUsername(uDto.getUsername());
                }

                // Update userIdentifier if provided (but don't overwrite existing)
                if (uDto.getUserIdentifier() != null && !uDto.getUserIdentifier().isEmpty() && user.getUserIdentifier() == null) {
                    user.setUserIdentifier(uDto.getUserIdentifier());
                }

                // Update password if provided
                String password = uDto.getPassword();
                if (password != null && !password.isEmpty()) {
                    if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
                        // Already a BCrypt hash (60 chars) - use directly
                        user.setPassword(password);
                    } else {
                        // Plaintext password - encode it
                        user.setPassword(passwordEncoder.encode(password));
                    }
                } else if (user.getPassword() == null || user.getPassword().isEmpty()) {
                    // No password and user is new - use default
                    user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                }
                // Update other fields (merge) - convert null to empty string for string fields
                // Note: only update if DTO has a non-null value (null in DTO means "not provided")
                if (uDto.getXaiApiKey() != null) {
                    user.setXaiApiKey(uDto.getXaiApiKey());
                }
                if (uDto.getGooglePhotosApiKey() != null) {
                    user.setGooglePhotosApiKey(uDto.getGooglePhotosApiKey());
                }
                if (uDto.getGooglePhotosRefreshToken() != null) {
                    user.setGooglePhotosRefreshToken(uDto.getGooglePhotosRefreshToken());
                }
                if (uDto.getGooglePhotosTokenExpiry() != null) {
                    user.setGooglePhotosTokenExpiry(uDto.getGooglePhotosTokenExpiry());
                }
                if (uDto.getGoogleClientSecret() != null) {
                    user.setGoogleClientSecret(uDto.getGoogleClientSecret());
                }
                if (uDto.getGooglePhotosAlbumId() != null) {
                    user.setGooglePhotosAlbumId(uDto.getGooglePhotosAlbumId());
                }
                if (uDto.getLastPhotoTimestamp() != null) {
                    user.setLastPhotoTimestamp(uDto.getLastPhotoTimestamp());
                }
                if (uDto.getSsoProvider() != null) {
                    user.setSsoProvider(uDto.getSsoProvider());
                }
                if (uDto.getSsoSubjectId() != null) {
                    user.setSsoSubjectId(uDto.getSsoSubjectId());
                }
                if (uDto.getEmail() != null) {
                    user.setEmail(uDto.getEmail());
                }
                if (uDto.getLibraryCardDesign() != null) {
                    user.setLibraryCardDesign(uDto.getLibraryCardDesign());
                }
                // Ensure empty fields are initialized properly for new users
                if (user.getXaiApiKey() == null) user.setXaiApiKey("");
                if (user.getGooglePhotosApiKey() == null) user.setGooglePhotosApiKey("");
                if (user.getGooglePhotosRefreshToken() == null) user.setGooglePhotosRefreshToken("");
                if (user.getGooglePhotosTokenExpiry() == null) user.setGooglePhotosTokenExpiry("");
                if (user.getGoogleClientSecret() == null) user.setGoogleClientSecret("");
                if (user.getGooglePhotosAlbumId() == null) user.setGooglePhotosAlbumId("");
                if (user.getLastPhotoTimestamp() == null) user.setLastPhotoTimestamp("");
                Set<Authority> authorities = new HashSet<>();
                // Merge both 'authorities' and 'roles' fields for backwards compatibility
                List<String> authorityNames = new ArrayList<>();
                if (uDto.getAuthorities() != null) {
                    authorityNames.addAll(uDto.getAuthorities());
                }
                if (uDto.getRoles() != null) {
                    authorityNames.addAll(uDto.getRoles());
                }

                if (!authorityNames.isEmpty()) {
                    for (String rName : authorityNames) {
                        // Use list-based query to handle potential duplicates gracefully
                        List<Authority> existingAuthorities = authorityRepository.findAllByNameOrderByIdAsc(rName);
                        Authority authority;
                        if (existingAuthorities.isEmpty()) {
                            Authority r = new Authority();
                            r.setName(rName);
                            authority = authorityRepository.save(r);
                        } else {
                            authority = existingAuthorities.get(0); // Select the one with the lowest ID
                            if (existingAuthorities.size() > 1) {
                                logger.warn("Found {} duplicate authorities with name '{}'. Using authority with lowest ID: {}. " +
                                           "Consider cleaning up duplicate entries in the database.",
                                           existingAuthorities.size(), rName, authority.getId());
                            }
                        }
                        authorities.add(authority);
                    }
                    user.setAuthorities(authorities);
                }
                user = userRepository.save(user);
                userMap.put(uDto.getUsername(), user);
                userCount++;
            }
        }
        logger.info("Imported {} users", userCount);

        Map<String, Book> bookMap = new HashMap<>();
        if (dto.getBooks() != null) {
            for (ImportBookDto bDto : dto.getBooks()) {
                // Support both new format (authorName) and old format (embedded author object)
                String authorNameToLookup = null;
                if (bDto.getAuthorName() != null && !bDto.getAuthorName().isEmpty()) {
                    // New format: direct author name reference
                    authorNameToLookup = bDto.getAuthorName();
                } else if (bDto.getAuthor() != null && bDto.getAuthor().getName() != null) {
                    // Old format: extract name from embedded author object
                    authorNameToLookup = bDto.getAuthor().getName();
                }

                Author author = null;
                if (authorNameToLookup != null) {
                    author = authMap.get(authorNameToLookup);
                    if (author == null) {
                        throw new LibraryException("Author not found for book: " + bDto.getTitle() + " - " + authorNameToLookup);
                    }
                }
                Library library = libMap.get(bDto.getLibraryName());
                if (library == null) {
                    throw new LibraryException("Library not found for book: " + bDto.getTitle() + " - " + bDto.getLibraryName());
                }

                // Check if book with same title and author already exists
                Book book;
                if (author != null) {
                    List<Book> existingBooks = bookRepository.findAllByTitleAndAuthor_NameOrderByIdAsc(bDto.getTitle(), author.getName());
                    if (existingBooks.isEmpty()) {
                        book = null;
                    } else {
                        book = existingBooks.get(0);
                        if (existingBooks.size() > 1) {
                            logger.warn("Found {} duplicate books with title '{}' and author '{}'. Using first one with ID: {}",
                                    existingBooks.size(), bDto.getTitle(), author.getName(), book.getId());
                        }
                    }
                } else {
                    List<Book> existingBooks = bookRepository.findAllByTitleAndAuthorIsNullOrderByIdAsc(bDto.getTitle());
                    if (existingBooks.isEmpty()) {
                        book = null;
                    } else {
                        book = existingBooks.get(0);
                        if (existingBooks.size() > 1) {
                            logger.warn("Found {} duplicate books with title '{}' and no author. Using first one with ID: {}",
                                    existingBooks.size(), bDto.getTitle(), book.getId());
                        }
                    }
                }
                if (book == null) {
                    book = new Book();
                    book.setTitle(bDto.getTitle());
                }

                // Update fields (merge)
                book.setPublicationYear(bDto.getPublicationYear());
                book.setPublisher(bDto.getPublisher());
                book.setPlotSummary(bDto.getPlotSummary());
                book.setRelatedWorks(bDto.getRelatedWorks());
                book.setDetailedDescription(bDto.getDetailedDescription());
                book.setGrokipediaUrl(bDto.getGrokipediaUrl());
                book.setFreeTextUrl(bDto.getFreeTextUrl());
                if (bDto.getDateAddedToLibrary() != null) {
                    book.setDateAddedToLibrary(bDto.getDateAddedToLibrary());
                } else if (book.getDateAddedToLibrary() == null) {
                    book.setDateAddedToLibrary(LocalDateTime.now());
                }
                if (bDto.getLastModified() != null) {
                    book.setLastModified(bDto.getLastModified());
                }
                book.setStatus(bDto.getStatus() != null ? bDto.getStatus() : BookStatus.ACTIVE);
                book.setLocNumber(bDto.getLocNumber());
                book.setStatusReason(bDto.getStatusReason());
                book.setAuthor(author);
                book.setLibrary(library);
                book = bookRepository.save(book);

                String key = bDto.getTitle() + "|" + (authorNameToLookup != null ? authorNameToLookup : "");
                bookMap.put(key, book);
                bookCount++;
            }
        }
        logger.info("Imported {} books", bookCount);

        if (dto.getLoans() != null) {
            for (ImportLoanDto lDto : dto.getLoans()) {
                // Support both new format (reference fields) and old format (embedded objects)
                String bookTitle = null;
                String bookAuthorName = null;
                String username = null;

                // Try new format first
                if (lDto.getBookTitle() != null && !lDto.getBookTitle().isEmpty()) {
                    bookTitle = lDto.getBookTitle();
                    bookAuthorName = lDto.getBookAuthorName() != null ? lDto.getBookAuthorName() : "";
                } else if (lDto.getBook() != null) {
                    // Fall back to old format: extract from embedded book object
                    bookTitle = lDto.getBook().getTitle();
                    // Check both new authorName and old embedded author
                    if (lDto.getBook().getAuthorName() != null && !lDto.getBook().getAuthorName().isEmpty()) {
                        bookAuthorName = lDto.getBook().getAuthorName();
                    } else if (lDto.getBook().getAuthor() != null && lDto.getBook().getAuthor().getName() != null) {
                        bookAuthorName = lDto.getBook().getAuthor().getName();
                    } else {
                        bookAuthorName = "";
                    }
                }

                // Try new format for user first
                if (lDto.getUsername() != null && !lDto.getUsername().isEmpty()) {
                    username = lDto.getUsername();
                } else if (lDto.getUser() != null) {
                    // Fall back to old format: extract from embedded user object
                    username = lDto.getUser().getUsername();
                }

                Book book = null;
                if (bookTitle != null) {
                    String key = bookTitle + "|" + bookAuthorName;
                    book = bookMap.get(key);
                    if (book == null) {
                        throw new LibraryException("Book not found for loan: " + bookTitle + " by " + bookAuthorName);
                    }
                }
                User user = null;
                if (username != null) {
                    user = userMap.get(username);
                    if (user == null) {
                        throw new LibraryException("User not found for loan: " + username);
                    }
                }

                LocalDate loanDate = lDto.getLoanDate() != null ? lDto.getLoanDate() : LocalDate.now();

                // Check if loan already exists (same book, user, and loan date)
                Loan loan = null;
                if (book != null && user != null) {
                    List<Loan> existingLoans = loanRepository.findAllByBookIdAndUserIdAndLoanDateOrderByIdAsc(book.getId(), user.getId(), loanDate);
                    if (!existingLoans.isEmpty()) {
                        loan = existingLoans.get(0);
                        if (existingLoans.size() > 1) {
                            logger.warn("Found {} duplicate loans for book ID {}, user ID {}, date {}. Using loan with lowest ID: {}. " +
                                       "Consider cleaning up duplicate entries in the database.",
                                       existingLoans.size(), book.getId(), user.getId(), loanDate, loan.getId());
                        }
                    }
                }
                if (loan == null) {
                    loan = new Loan();
                    loan.setBook(book);
                    loan.setUser(user);
                    loan.setLoanDate(loanDate);
                }

                // Update fields (merge)
                loan.setDueDate(lDto.getDueDate() != null ? lDto.getDueDate() : loanDate.plusWeeks(2));
                loan.setReturnDate(lDto.getReturnDate());
                loanRepository.save(loan);
                loanCount++;
            }
        }
        logger.info("Imported {} loans", loanCount);

        // Import photos
        logger.info("Ready to import photos.");
        if (dto.getPhotos() != null) {
            logger.info("Importing {} photos", dto.getPhotos().size());
            for (ImportPhotoDto pDto : dto.getPhotos()) {
                // First resolve book and author references
                Book book = null;
                if (pDto.getBookTitle() != null) {
                    // Handle books with or without authors (null author means empty string key)
                    String authorKey = pDto.getBookAuthorName() != null ? pDto.getBookAuthorName() : "";
                    String key = pDto.getBookTitle() + "|" + authorKey;
                    book = bookMap.get(key);
                    if (book == null) {
                        throw new LibraryException("Book not found for photo: " + pDto.getBookTitle() + " by " + (pDto.getBookAuthorName() != null ? pDto.getBookAuthorName() : "(no author)"));
                    }
                }

                Author author = null;
                if (pDto.getAuthorName() != null) {
                    author = authMap.get(pDto.getAuthorName());
                    if (author == null) {
                        throw new LibraryException("Author not found for photo: " + pDto.getAuthorName());
                    }
                }

                // Try to find existing photo by imageChecksum, permanentId, or book/author + photoOrder
                Photo photo = null;

                // 1. If photo has imageChecksum (SHA-256), try to find existing photo with same checksum
                if (pDto.getImageChecksum() != null && !pDto.getImageChecksum().trim().isEmpty()) {
                    photo = photoRepository.findByImageChecksum(pDto.getImageChecksum()).orElse(null);
                    if (photo != null) {
                        logger.info("Found existing photo with imageChecksum: {} (Photo ID: {})", pDto.getImageChecksum(), photo.getId());
                    }
                }

                // 2. If photo has a permanentId and not found by checksum, try permanentId
                if (photo == null && pDto.getPermanentId() != null && !pDto.getPermanentId().trim().isEmpty()) {
                    photo = photoRepository.findByPermanentId(pDto.getPermanentId()).orElse(null);
                    if (photo != null) {
                        logger.info("Found existing photo with permanentId: {} (Photo ID: {})", pDto.getPermanentId(), photo.getId());
                    }
                }

                // 3. If it's a book photo and not found by checksum/permanentId, match by book + photoOrder
                if (photo == null && book != null && pDto.getPhotoOrder() != null) {
                    List<Photo> photos = photoRepository.findByBookIdAndPhotoOrderOrderByIdAsc(book.getId(), pDto.getPhotoOrder());
                    if (!photos.isEmpty()) {
                        photo = photos.get(0); // Use the one with lowest ID
                        logger.info("Found existing photo with bookId: {} photoOrder: {} (old Perm ID: '{}' -> New Perm ID: '{}' Photo ID: {})",
                            book.getId(), pDto.getPhotoOrder(), photo.getPermanentId(), pDto.getPermanentId(), photo.getId());
                        if (photos.size() > 1) {
                            logger.warn("WARNING: Found {} photos with same bookId {} and photoOrder {}. Using photo ID {}. " +
                                "This may cause permanentId mismatches! Other photo IDs: {}",
                                photos.size(), book.getId(), pDto.getPhotoOrder(), photo.getId(),
                                photos.stream().skip(1).map(p -> p.getId().toString()).collect(java.util.stream.Collectors.joining(", ")));
                        }
                    }
                }

                // 4. If it's an author-only photo, match by author + photoOrder
                if (photo == null && author != null && book == null && pDto.getPhotoOrder() != null) {
                    List<Photo> photos = photoRepository.findByAuthorIdAndBookIsNullAndPhotoOrderOrderByIdAsc(author.getId(), pDto.getPhotoOrder());
                    if (!photos.isEmpty()) {
                        photo = photos.get(0); // Use the one with lowest ID
                        logger.info("Found existing photo with authorId: {} photoOrder: {} (old Perm ID: '{}' -> New Perm ID: '{}' Photo ID: {})",
                            author.getId(), pDto.getPhotoOrder(), photo.getPermanentId(), pDto.getPermanentId(), photo.getId());
                        if (photos.size() > 1) {
                            logger.warn("WARNING: Found {} photos with same authorId {} and photoOrder {}. Using photo ID {}. " +
                                "This may cause permanentId mismatches! Other photo IDs: {}",
                                photos.size(), author.getId(), pDto.getPhotoOrder(), photo.getId(),
                                photos.stream().skip(1).map(p -> p.getId().toString()).collect(java.util.stream.Collectors.joining(", ")));
                        }
                    }
                }

                // 5. Create new photo if not found
                if (photo == null) {
                    photo = new Photo();
                    logger.info("Existing photo not found for bookId: {}. Starting new photo. Perm ID: {} Checksum: {}",
                        book != null ? book.getId() : "null", pDto.getPermanentId(), pDto.getImageChecksum());
                }

                // Update fields (merge)
                photo.setContentType(pDto.getContentType());
                photo.setCaption(pDto.getCaption());
                photo.setPhotoOrder(pDto.getPhotoOrder());
                photo.setPermanentId(pDto.getPermanentId());
                photo.setExportedAt(pDto.getExportedAt());
                photo.setExportStatus(pDto.getExportStatus());
                photo.setExportErrorMessage(pDto.getExportErrorMessage());
                // Note: imageChecksum is not updated here - it's a reference field, not a value to import
                // The actual imageChecksum is computed from the photo binary during upload
                photo.setBook(book);
                photo.setAuthor(author);

                photoRepository.save(photo);
                photoCount++;
            }
        }
        logger.info("Imported {} photos", photoCount);

        logger.info("Import completed successfully. Total: {} libraries, {} authors, {} users, {} books, {} loans, {} photos",
            libraryCount, authorCount, userCount, bookCount, loanCount, photoCount);

        return new ImportResponseDto.ImportCounts(libraryCount, authorCount, userCount, bookCount, loanCount, photoCount);
    }

    public ImportRequestDto exportData() {
        ImportRequestDto dto = new ImportRequestDto();

        // Export branches
        List<BranchDto> libDtos = branchRepository.findAll().stream()
                .map(branchMapper::toDto)
                .collect(Collectors.toList());
        dto.setBranches(libDtos);

        // Export authors
        // Note: Empty strings are converted to null so they're excluded from JSON export
        List<ImportAuthorDto> authDtos = new ArrayList<>();
        for (Author author : authorRepository.findAll()) {
            ImportAuthorDto aDto = new ImportAuthorDto();
            aDto.setName(author.getName());
            aDto.setDateOfBirth(author.getDateOfBirth());
            aDto.setDateOfDeath(author.getDateOfDeath());
            aDto.setReligiousAffiliation(emptyToNull(author.getReligiousAffiliation()));
            aDto.setBirthCountry(emptyToNull(author.getBirthCountry()));
            aDto.setNationality(emptyToNull(author.getNationality()));
            aDto.setBriefBiography(emptyToNull(author.getBriefBiography()));
            aDto.setGrokipediaUrl(emptyToNull(author.getGrokipediaUrl()));
            authDtos.add(aDto);
        }
        dto.setAuthors(authDtos);

        // Export users (including hashed passwords)
        // Note: Empty strings are converted to null so they're excluded from JSON export
        List<ImportUserDto> userDtos = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            ImportUserDto uDto = new ImportUserDto();
            uDto.setUsername(user.getUsername());
            uDto.setPassword(user.getPassword()); // Export BCrypt hashed password (60 chars)
            uDto.setXaiApiKey(emptyToNull(user.getXaiApiKey()));
            uDto.setGooglePhotosApiKey(emptyToNull(user.getGooglePhotosApiKey()));
            uDto.setGooglePhotosRefreshToken(emptyToNull(user.getGooglePhotosRefreshToken()));
            uDto.setGooglePhotosTokenExpiry(emptyToNull(user.getGooglePhotosTokenExpiry()));
            uDto.setGoogleClientSecret(emptyToNull(user.getGoogleClientSecret()));
            uDto.setGooglePhotosAlbumId(emptyToNull(user.getGooglePhotosAlbumId()));
            uDto.setLastPhotoTimestamp(emptyToNull(user.getLastPhotoTimestamp()));
            uDto.setSsoProvider(emptyToNull(user.getSsoProvider()));
            uDto.setSsoSubjectId(emptyToNull(user.getSsoSubjectId()));
            uDto.setEmail(emptyToNull(user.getEmail()));
            uDto.setLibraryCardDesign(user.getLibraryCardDesign());
            if (user.getAuthorities() != null) {
                java.util.List<String> authorityNames = user.getAuthorities().stream()
                        .map(authority -> authority.getName())
                        .collect(Collectors.toList());
                uDto.setAuthorities(authorityNames);
            }
            uDto.setUserIdentifier(user.getUserIdentifier());  // Set last for JSON ordering
            userDtos.add(uDto);
        }
        dto.setUsers(userDtos);

        // Export books (new format: authorName reference instead of embedded author object)
        // Note: lastModified is NOT exported because it gets updated during import
        List<ImportBookDto> bookDtos = new ArrayList<>();
        for (Book book : bookRepository.findAllWithAuthorAndLibrary()) {
            ImportBookDto bDto = new ImportBookDto();
            bDto.setTitle(book.getTitle());
            bDto.setPublicationYear(book.getPublicationYear());
            bDto.setPublisher(emptyToNull(book.getPublisher()));
            bDto.setPlotSummary(emptyToNull(book.getPlotSummary()));
            bDto.setRelatedWorks(emptyToNull(book.getRelatedWorks()));
            bDto.setDetailedDescription(emptyToNull(book.getDetailedDescription()));
            bDto.setGrokipediaUrl(emptyToNull(book.getGrokipediaUrl()));
            bDto.setFreeTextUrl(emptyToNull(book.getFreeTextUrl()));
            bDto.setDateAddedToLibrary(book.getDateAddedToLibrary());
            // Note: lastModified is NOT exported - it will be updated during import
            bDto.setStatus(book.getStatus());
            bDto.setLocNumber(emptyToNull(book.getLocNumber()));
            bDto.setStatusReason(emptyToNull(book.getStatusReason()));
            // New format: reference author by name only (not embedded object)
            if (book.getAuthor() != null) {
                bDto.setAuthorName(book.getAuthor().getName());
            }
            // Note: bDto.setAuthor() is NOT set - embedded author is deprecated for export
            if (book.getLibrary() != null) {
                bDto.setLibraryName(book.getLibrary().getBranchName());
            }
            bookDtos.add(bDto);
        }
        dto.setBooks(bookDtos);

        // Export loans (new format: reference fields instead of embedded objects)
        List<ImportLoanDto> loanDtos = new ArrayList<>();
        for (Loan loan : loanRepository.findAllWithBookAndUser()) {
            ImportLoanDto lDto = new ImportLoanDto();
            // New format: reference book and user by natural keys only
            if (loan.getBook() != null) {
                lDto.setBookTitle(loan.getBook().getTitle());
                if (loan.getBook().getAuthor() != null) {
                    lDto.setBookAuthorName(loan.getBook().getAuthor().getName());
                }
            }
            if (loan.getUser() != null) {
                lDto.setUsername(loan.getUser().getUsername());
            }
            // Note: lDto.setBook() and lDto.setUser() are NOT set - embedded objects are deprecated for export
            lDto.setLoanDate(loan.getLoanDate());
            lDto.setDueDate(loan.getDueDate());
            lDto.setReturnDate(loan.getReturnDate());
            loanDtos.add(lDto);
        }
        dto.setLoans(loanDtos);

        // Export photo metadata (excluding binary image data)
        // Photo metadata includes permanent IDs, captions, ordering, and export status
        // This allows photos to be reconnected during import via book/author matching
        List<ImportPhotoDto> photoDtos = new ArrayList<>();
        for (PhotoMetadataProjection photo : photoRepository.findBy()) {
            // Skip soft-deleted photos
            if (photo.getDeletedAt() != null) {
                continue;
            }

            ImportPhotoDto pDto = new ImportPhotoDto();
            pDto.setContentType(emptyToNull(photo.getContentType()));
            pDto.setCaption(emptyToNull(photo.getCaption()));
            pDto.setPhotoOrder(photo.getPhotoOrder());
            pDto.setPermanentId(emptyToNull(photo.getPermanentId()));
            pDto.setExportedAt(photo.getExportedAt());
            pDto.setExportStatus(photo.getExportStatus());
            pDto.setExportErrorMessage(emptyToNull(photo.getExportErrorMessage()));
            pDto.setImageChecksum(emptyToNull(photo.getImageChecksum()));

            // Set book reference if exists
            if (photo.getBook() != null) {
                pDto.setBookTitle(photo.getBook().getTitle());
                if (photo.getBook().getAuthor() != null) {
                    pDto.setBookAuthorName(photo.getBook().getAuthor().getName());
                }
            }

            // Set author reference if exists (for author-only photos)
            if (photo.getAuthor() != null && photo.getBook() == null) {
                pDto.setAuthorName(photo.getAuthor().getName());
            }

            photoDtos.add(pDto);
        }
        dto.setPhotos(photoDtos);

        return dto;
    }

    /**
     * Converts empty strings to null so they're excluded from JSON export.
     * This keeps the exported JSON cleaner and smaller.
     */
    private String emptyToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

    /**
     * Converts null to empty string for fields that require it during import.
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Get database statistics with total counts.
     * Used by the Data Management page to show accurate database statistics
     * rather than cached/paginated data from the frontend.
     */
    @Transactional(readOnly = true)
    public DatabaseStatsDto getDatabaseStats() {
        return new DatabaseStatsDto(
            branchRepository.count(),
            bookRepository.count(),
            authorRepository.count(),
            userRepository.count(),
            loanRepository.count()
        );
    }
}
