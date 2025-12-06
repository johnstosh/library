package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.*;
import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.dto.importdtos.*;
import com.muczynski.library.mapper.LibraryMapper;
import com.muczynski.library.repository.*;
import com.muczynski.library.repository.PhotoMetadataProjection;
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

    private final LibraryRepository libraryRepository;
    private final AuthorRepository authorRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final RoleRepository roleRepository;
    private final PhotoRepository photoRepository;
    private final LibraryMapper libraryMapper;
    private final PasswordEncoder passwordEncoder;

    public void importData(ImportRequestDto dto) {
        Map<String, Library> libMap = new HashMap<>();
        if (dto.getLibraries() != null) {
            for (LibraryDto lDto : dto.getLibraries()) {
                // Check if library with same name already exists
                Library lib = libraryRepository.findByName(lDto.getName()).orElse(null);
                if (lib == null) {
                    // Create new library without copying ID from import
                    lib = new Library();
                    lib.setName(lDto.getName());
                    lib.setHostname(lDto.getHostname());
                } else {
                    // Update existing library
                    lib.setHostname(lDto.getHostname());
                }
                lib = libraryRepository.save(lib);
                libMap.put(lDto.getName(), lib);
            }
        }

        Map<String, Author> authMap = new HashMap<>();
        if (dto.getAuthors() != null) {
            for (ImportAuthorDto aDto : dto.getAuthors()) {
                // Check if author with same name already exists
                Author auth = authorRepository.findByName(aDto.getName());
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
                auth = authorRepository.save(auth);
                authMap.put(aDto.getName(), auth);
            }
        }

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
                // Update other fields (merge)
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
                Set<Role> roles = new HashSet<>();
                if (uDto.getRoles() != null) {
                    for (String rName : uDto.getRoles()) {
                        // Use list-based query to handle potential duplicates gracefully
                        List<Role> existingRoles = roleRepository.findAllByNameOrderByIdAsc(rName);
                        Role role;
                        if (existingRoles.isEmpty()) {
                            Role r = new Role();
                            r.setName(rName);
                            role = roleRepository.save(r);
                        } else {
                            role = existingRoles.get(0); // Select the one with the lowest ID
                            if (existingRoles.size() > 1) {
                                logger.warn("Found {} duplicate roles with name '{}'. Using role with lowest ID: {}. " +
                                           "Consider cleaning up duplicate entries in the database.",
                                           existingRoles.size(), rName, role.getId());
                            }
                        }
                        roles.add(role);
                    }
                    user.setRoles(roles);
                }
                user = userRepository.save(user);
                userMap.put(uDto.getUsername(), user);
            }
        }

        Map<String, Book> bookMap = new HashMap<>();
        if (dto.getBooks() != null) {
            for (ImportBookDto bDto : dto.getBooks()) {
                Author author = null;
                if (bDto.getAuthor() != null) {
                    author = authMap.get(bDto.getAuthor().getName());
                    if (author == null) {
                        throw new LibraryException("Author not found for book: " + bDto.getTitle() + " - " + bDto.getAuthor().getName());
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

                String key = bDto.getTitle() + "|" + (bDto.getAuthor() != null ? bDto.getAuthor().getName() : "");
                bookMap.put(key, book);
            }
        }

        if (dto.getLoans() != null) {
            for (ImportLoanDto lDto : dto.getLoans()) {
                Book book = null;
                if (lDto.getBook() != null) {
                    String authorName = lDto.getBook().getAuthor() != null ? lDto.getBook().getAuthor().getName() : "";
                    String key = lDto.getBook().getTitle() + "|" + authorName;
                    book = bookMap.get(key);
                    if (book == null) {
                        throw new LibraryException("Book not found for loan: " + lDto.getBook().getTitle() + " by " + authorName);
                    }
                }
                User user = null;
                if (lDto.getUser() != null) {
                    user = userMap.get(lDto.getUser().getUsername());
                    if (user == null) {
                        throw new LibraryException("User not found for loan: " + lDto.getUser().getUsername());
                    }
                }

                LocalDate loanDate = lDto.getLoanDate() != null ? lDto.getLoanDate() : LocalDate.now();

                // Check if loan already exists (same book, user, and loan date)
                Loan loan = null;
                if (book != null && user != null) {
                    loan = loanRepository.findByBookIdAndUserIdAndLoanDate(book.getId(), user.getId(), loanDate).orElse(null);
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
            }
        }

        // Import photos
        if (dto.getPhotos() != null) {
            for (ImportPhotoDto pDto : dto.getPhotos()) {
                // First resolve book and author references
                Book book = null;
                if (pDto.getBookTitle() != null && pDto.getBookAuthorName() != null) {
                    String key = pDto.getBookTitle() + "|" + pDto.getBookAuthorName();
                    book = bookMap.get(key);
                    if (book == null) {
                        throw new LibraryException("Book not found for photo: " + pDto.getBookTitle() + " by " + pDto.getBookAuthorName());
                    }
                }

                Author author = null;
                if (pDto.getAuthorName() != null) {
                    author = authMap.get(pDto.getAuthorName());
                    if (author == null) {
                        throw new LibraryException("Author not found for photo: " + pDto.getAuthorName());
                    }
                }

                // Try to find existing photo by book/author + photoOrder
                Photo photo = null;

                // 1. If it's a book photo, match by book + photoOrder
                if (book != null && pDto.getPhotoOrder() != null) {
                    List<Photo> photos = photoRepository.findByBookIdAndPhotoOrderOrderByIdAsc(book.getId(), pDto.getPhotoOrder());
                    if (!photos.isEmpty()) {
                        photo = photos.get(0); // Use the one with lowest ID
                    }
                }

                // 2. If it's an author-only photo, match by author + photoOrder
                if (photo == null && author != null && book == null && pDto.getPhotoOrder() != null) {
                    List<Photo> photos = photoRepository.findByAuthorIdAndBookIsNullAndPhotoOrderOrderByIdAsc(author.getId(), pDto.getPhotoOrder());
                    if (!photos.isEmpty()) {
                        photo = photos.get(0); // Use the one with lowest ID
                    }
                }

                // 3. Create new photo if not found
                if (photo == null) {
                    photo = new Photo();
                }

                // Update fields (merge)
                photo.setContentType(pDto.getContentType());
                photo.setCaption(pDto.getCaption());
                photo.setPhotoOrder(pDto.getPhotoOrder());
                photo.setPermanentId(pDto.getPermanentId());
                photo.setExportedAt(pDto.getExportedAt());
                photo.setExportStatus(pDto.getExportStatus());
                photo.setExportErrorMessage(pDto.getExportErrorMessage());
                photo.setBook(book);
                photo.setAuthor(author);

                photoRepository.save(photo);
            }
        }
    }

    public ImportRequestDto exportData() {
        ImportRequestDto dto = new ImportRequestDto();

        // Export libraries
        List<LibraryDto> libDtos = libraryRepository.findAll().stream()
                .map(libraryMapper::toDto)
                .collect(Collectors.toList());
        dto.setLibraries(libDtos);

        // Export authors
        List<ImportAuthorDto> authDtos = new ArrayList<>();
        for (Author author : authorRepository.findAll()) {
            ImportAuthorDto aDto = new ImportAuthorDto();
            aDto.setName(author.getName());
            aDto.setDateOfBirth(author.getDateOfBirth());
            aDto.setDateOfDeath(author.getDateOfDeath());
            aDto.setReligiousAffiliation(author.getReligiousAffiliation());
            aDto.setBirthCountry(author.getBirthCountry());
            aDto.setNationality(author.getNationality());
            aDto.setBriefBiography(author.getBriefBiography());
            authDtos.add(aDto);
        }
        dto.setAuthors(authDtos);

        // Export users (including hashed passwords)
        List<ImportUserDto> userDtos = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            ImportUserDto uDto = new ImportUserDto();
            uDto.setUserIdentifier(user.getUserIdentifier());
            uDto.setUsername(user.getUsername());
            uDto.setPassword(user.getPassword()); // Export BCrypt hashed password (60 chars)
            uDto.setXaiApiKey(user.getXaiApiKey());
            uDto.setGooglePhotosApiKey(user.getGooglePhotosApiKey());
            uDto.setGooglePhotosRefreshToken(user.getGooglePhotosRefreshToken());
            uDto.setGooglePhotosTokenExpiry(user.getGooglePhotosTokenExpiry());
            uDto.setGoogleClientSecret(user.getGoogleClientSecret());
            uDto.setGooglePhotosAlbumId(user.getGooglePhotosAlbumId());
            uDto.setLastPhotoTimestamp(user.getLastPhotoTimestamp());
            uDto.setSsoProvider(user.getSsoProvider());
            uDto.setSsoSubjectId(user.getSsoSubjectId());
            uDto.setEmail(user.getEmail());
            uDto.setLibraryCardDesign(user.getLibraryCardDesign());
            if (user.getRoles() != null) {
                List<String> roles = user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList());
                uDto.setRoles(roles);
            }
            userDtos.add(uDto);
        }
        dto.setUsers(userDtos);

        // Export books
        List<ImportBookDto> bookDtos = new ArrayList<>();
        for (Book book : bookRepository.findAllWithAuthorAndLibrary()) {
            ImportBookDto bDto = new ImportBookDto();
            bDto.setTitle(book.getTitle());
            bDto.setPublicationYear(book.getPublicationYear());
            bDto.setPublisher(book.getPublisher());
            bDto.setPlotSummary(book.getPlotSummary());
            bDto.setRelatedWorks(book.getRelatedWorks());
            bDto.setDetailedDescription(book.getDetailedDescription());
            bDto.setDateAddedToLibrary(book.getDateAddedToLibrary());
            bDto.setLastModified(book.getLastModified());
            bDto.setStatus(book.getStatus());
            bDto.setLocNumber(book.getLocNumber());
            bDto.setStatusReason(book.getStatusReason());
            if (book.getAuthor() != null) {
                ImportAuthorDto authorDto = new ImportAuthorDto();
                authorDto.setName(book.getAuthor().getName());
                authorDto.setDateOfBirth(book.getAuthor().getDateOfBirth());
                authorDto.setDateOfDeath(book.getAuthor().getDateOfDeath());
                authorDto.setReligiousAffiliation(book.getAuthor().getReligiousAffiliation());
                authorDto.setBirthCountry(book.getAuthor().getBirthCountry());
                authorDto.setNationality(book.getAuthor().getNationality());
                authorDto.setBriefBiography(book.getAuthor().getBriefBiography());
                bDto.setAuthor(authorDto);
            }
            if (book.getLibrary() != null) {
                bDto.setLibraryName(book.getLibrary().getName());
            }
            bookDtos.add(bDto);
        }
        dto.setBooks(bookDtos);

        // Export loans
        List<ImportLoanDto> loanDtos = new ArrayList<>();
        for (Loan loan : loanRepository.findAllWithBookAndUser()) {
            ImportLoanDto lDto = new ImportLoanDto();
            if (loan.getBook() != null) {
                ImportBookDto bookDto = new ImportBookDto();
                bookDto.setTitle(loan.getBook().getTitle());
                bookDto.setPublicationYear(loan.getBook().getPublicationYear());
                bookDto.setPublisher(loan.getBook().getPublisher());
                bookDto.setPlotSummary(loan.getBook().getPlotSummary());
                bookDto.setRelatedWorks(loan.getBook().getRelatedWorks());
                bookDto.setDetailedDescription(loan.getBook().getDetailedDescription());
                bookDto.setDateAddedToLibrary(loan.getBook().getDateAddedToLibrary());
                bookDto.setLastModified(loan.getBook().getLastModified());
                bookDto.setStatus(loan.getBook().getStatus());
                bookDto.setLocNumber(loan.getBook().getLocNumber());
                bookDto.setStatusReason(loan.getBook().getStatusReason());
                if (loan.getBook().getAuthor() != null) {
                    ImportAuthorDto authorDto = new ImportAuthorDto();
                    authorDto.setName(loan.getBook().getAuthor().getName());
                    authorDto.setDateOfBirth(loan.getBook().getAuthor().getDateOfBirth());
                    authorDto.setDateOfDeath(loan.getBook().getAuthor().getDateOfDeath());
                    authorDto.setReligiousAffiliation(loan.getBook().getAuthor().getReligiousAffiliation());
                    authorDto.setBirthCountry(loan.getBook().getAuthor().getBirthCountry());
                    authorDto.setNationality(loan.getBook().getAuthor().getNationality());
                    authorDto.setBriefBiography(loan.getBook().getAuthor().getBriefBiography());
                    bookDto.setAuthor(authorDto);
                }
                if (loan.getBook().getLibrary() != null) {
                    bookDto.setLibraryName(loan.getBook().getLibrary().getName());
                }
                lDto.setBook(bookDto);
            }
            if (loan.getUser() != null) {
                ImportUserDto userDto = new ImportUserDto();
                userDto.setUserIdentifier(loan.getUser().getUserIdentifier());
                userDto.setUsername(loan.getUser().getUsername());
                userDto.setPassword(loan.getUser().getPassword()); // Export BCrypt hashed password (60 chars)
                userDto.setXaiApiKey(loan.getUser().getXaiApiKey());
                userDto.setGooglePhotosApiKey(loan.getUser().getGooglePhotosApiKey());
                userDto.setGooglePhotosRefreshToken(loan.getUser().getGooglePhotosRefreshToken());
                userDto.setGooglePhotosTokenExpiry(loan.getUser().getGooglePhotosTokenExpiry());
                userDto.setGoogleClientSecret(loan.getUser().getGoogleClientSecret());
                userDto.setGooglePhotosAlbumId(loan.getUser().getGooglePhotosAlbumId());
                userDto.setLastPhotoTimestamp(loan.getUser().getLastPhotoTimestamp());
                userDto.setSsoProvider(loan.getUser().getSsoProvider());
                userDto.setSsoSubjectId(loan.getUser().getSsoSubjectId());
                userDto.setEmail(loan.getUser().getEmail());
                userDto.setLibraryCardDesign(loan.getUser().getLibraryCardDesign());
                if (loan.getUser().getRoles() != null) {
                    List<String> roles = loan.getUser().getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList());
                    userDto.setRoles(roles);
                }
                lDto.setUser(userDto);
            }
            lDto.setLoanDate(loan.getLoanDate());
            lDto.setDueDate(loan.getDueDate());
            lDto.setReturnDate(loan.getReturnDate());
            loanDtos.add(lDto);
        }
        dto.setLoans(loanDtos);

        // Export photos (metadata only, no image bytes - using projection to avoid OOM)
        List<ImportPhotoDto> photoDtos = new ArrayList<>();
        for (PhotoMetadataProjection photo : photoRepository.findAllProjectedBy()) {
            ImportPhotoDto pDto = new ImportPhotoDto();
            pDto.setContentType(photo.getContentType());
            pDto.setCaption(photo.getCaption());
            pDto.setPhotoOrder(photo.getPhotoOrder());
            pDto.setPermanentId(photo.getPermanentId());
            pDto.setExportedAt(photo.getExportedAt());
            pDto.setExportStatus(photo.getExportStatus());
            pDto.setExportErrorMessage(photo.getExportErrorMessage());

            // Reference book by title and author name
            PhotoMetadataProjection.BookProjection book = photo.getBook();
            if (book != null) {
                pDto.setBookTitle(book.getTitle());
                PhotoMetadataProjection.AuthorProjection bookAuthor = book.getAuthor();
                if (bookAuthor != null) {
                    pDto.setBookAuthorName(bookAuthor.getName());
                }
            }

            // Reference author by name (for author photos)
            PhotoMetadataProjection.AuthorProjection author = photo.getAuthor();
            if (author != null) {
                pDto.setAuthorName(author.getName());
            }

            photoDtos.add(pDto);
        }
        dto.setPhotos(photoDtos);

        return dto;
    }
}
