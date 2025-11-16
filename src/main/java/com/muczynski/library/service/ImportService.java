package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.*;
import com.muczynski.library.dto.LibraryDto;
import com.muczynski.library.dto.importdtos.*;
import com.muczynski.library.mapper.LibraryMapper;
import com.muczynski.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportService {

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
                Library lib = libraryMapper.toEntity(lDto);
                lib = libraryRepository.save(lib);
                libMap.put(lDto.getName(), lib);
            }
        }

        Map<String, Author> authMap = new HashMap<>();
        if (dto.getAuthors() != null) {
            for (ImportAuthorDto aDto : dto.getAuthors()) {
                Author auth = new Author();
                auth.setName(aDto.getName());
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
                User user = new User();
                user.setUsername(uDto.getUsername());
                String password = uDto.getPassword();
                if (password == null || password.isEmpty()) {
                    password = DEFAULT_PASSWORD;
                }
                user.setPassword(passwordEncoder.encode(password));
                user.setXaiApiKey(uDto.getXaiApiKey());
                Set<Role> roles = new HashSet<>();
                if (uDto.getRoles() != null) {
                    for (String rName : uDto.getRoles()) {
                        Role role = roleRepository.findByName(rName).orElseGet(() -> {
                            Role r = new Role();
                            r.setName(rName);
                            return roleRepository.save(r);
                        });
                        roles.add(role);
                    }
                }
                user.setRoles(roles);
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

                Book book = new Book();
                book.setTitle(bDto.getTitle());
                book.setPublicationYear(bDto.getPublicationYear());
                book.setPublisher(bDto.getPublisher());
                book.setPlotSummary(bDto.getPlotSummary());
                book.setRelatedWorks(bDto.getRelatedWorks());
                book.setDetailedDescription(bDto.getDetailedDescription());
                book.setDateAddedToLibrary(bDto.getDateAddedToLibrary() != null ? bDto.getDateAddedToLibrary() : LocalDate.now());
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

                Loan loan = new Loan();
                loan.setBook(book);
                loan.setUser(user);
                loan.setLoanDate(lDto.getLoanDate() != null ? lDto.getLoanDate() : LocalDate.now());
                loan.setDueDate(lDto.getDueDate() != null ? lDto.getDueDate() : LocalDate.now().plusWeeks(2));
                loan.setReturnDate(lDto.getReturnDate());
                loanRepository.save(loan);
            }
        }

        // Import photos
        if (dto.getPhotos() != null) {
            for (ImportPhotoDto pDto : dto.getPhotos()) {
                Photo photo = new Photo();
                photo.setContentType(pDto.getContentType());
                photo.setCaption(pDto.getCaption());
                photo.setPhotoOrder(pDto.getPhotoOrder());
                photo.setPermanentId(pDto.getPermanentId());
                photo.setExportedAt(pDto.getExportedAt());
                photo.setExportStatus(pDto.getExportStatus());
                photo.setExportErrorMessage(pDto.getExportErrorMessage());

                // Link to book if specified
                if (pDto.getBookTitle() != null && pDto.getBookAuthorName() != null) {
                    String key = pDto.getBookTitle() + "|" + pDto.getBookAuthorName();
                    Book book = bookMap.get(key);
                    if (book == null) {
                        throw new LibraryException("Book not found for photo: " + pDto.getBookTitle() + " by " + pDto.getBookAuthorName());
                    }
                    photo.setBook(book);
                }

                // Link to author if specified
                if (pDto.getAuthorName() != null) {
                    Author author = authMap.get(pDto.getAuthorName());
                    if (author == null) {
                        throw new LibraryException("Author not found for photo: " + pDto.getAuthorName());
                    }
                    photo.setAuthor(author);
                }

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

        // Export users (password set to empty for security)
        List<ImportUserDto> userDtos = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            ImportUserDto uDto = new ImportUserDto();
            uDto.setUsername(user.getUsername());
            uDto.setPassword(""); // Do not export actual password
            uDto.setXaiApiKey(user.getXaiApiKey());
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
        for (Book book : bookRepository.findAll()) {
            ImportBookDto bDto = new ImportBookDto();
            bDto.setTitle(book.getTitle());
            bDto.setPublicationYear(book.getPublicationYear());
            bDto.setPublisher(book.getPublisher());
            bDto.setPlotSummary(book.getPlotSummary());
            bDto.setRelatedWorks(book.getRelatedWorks());
            bDto.setDetailedDescription(book.getDetailedDescription());
            bDto.setDateAddedToLibrary(book.getDateAddedToLibrary());
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
        for (Loan loan : loanRepository.findAll()) {
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
                userDto.setUsername(loan.getUser().getUsername());
                userDto.setPassword(""); // Do not export actual password
                userDto.setXaiApiKey(loan.getUser().getXaiApiKey());
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

        // Export photos (metadata only, no image bytes)
        List<ImportPhotoDto> photoDtos = new ArrayList<>();
        for (Photo photo : photoRepository.findAll()) {
            ImportPhotoDto pDto = new ImportPhotoDto();
            pDto.setContentType(photo.getContentType());
            pDto.setCaption(photo.getCaption());
            pDto.setPhotoOrder(photo.getPhotoOrder());
            pDto.setPermanentId(photo.getPermanentId());
            pDto.setExportedAt(photo.getExportedAt());
            pDto.setExportStatus(photo.getExportStatus());
            pDto.setExportErrorMessage(photo.getExportErrorMessage());

            // Reference book by title and author name
            if (photo.getBook() != null) {
                pDto.setBookTitle(photo.getBook().getTitle());
                if (photo.getBook().getAuthor() != null) {
                    pDto.setBookAuthorName(photo.getBook().getAuthor().getName());
                }
            }

            // Reference author by name (for author photos)
            if (photo.getAuthor() != null) {
                pDto.setAuthorName(photo.getAuthor().getName());
            }

            photoDtos.add(pDto);
        }
        dto.setPhotos(photoDtos);

        return dto;
    }
}
