package com.muczynski.library.service;

import com.muczynski.library.domain.*;
import com.muczynski.library.dto.*;
import com.muczynski.library.mapper.LibraryMapper;
import com.muczynski.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.Base64;
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
    private final PhotoRepository photoRepository;
    private final RoleRepository roleRepository;
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
            for (AuthorImportDto aDto : dto.getAuthors()) {
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

                List<Photo> photos = new ArrayList<>();
                if (aDto.getPhotos() != null) {
                    for (int i = 0; i < aDto.getPhotos().size(); i++) {
                        PhotoImportDto pDto = aDto.getPhotos().get(i);
                        if (pDto.getImageBase64() != null && !pDto.getImageBase64().isEmpty()) {
                            Photo photo = new Photo();
                            photo.setImage(Base64.getDecoder().decode(pDto.getImageBase64()));
                            photo.setContentType(pDto.getContentType());
                            photo.setCaption(pDto.getCaption());
                            photo.setPhotoOrder(i);
                            photo.setAuthor(auth);
                            photos.add(photo);
                        }
                    }
                    if (!photos.isEmpty()) {
                        photoRepository.saveAll(photos);
                        auth.setPhotos(photos);
                    }
                }
            }
        }

        Map<String, User> userMap = new HashMap<>();
        if (dto.getUsers() != null) {
            for (UserImportDto uDto : dto.getUsers()) {
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
            for (BookImportDto bDto : dto.getBooks()) {
                Author author = authMap.get(bDto.getAuthorName());
                if (author == null) {
                    throw new RuntimeException("Author not found for book: " + bDto.getTitle() + " - " + bDto.getAuthorName());
                }
                Library library = libMap.get(bDto.getLibraryName());
                if (library == null) {
                    throw new RuntimeException("Book not found for book: " + bDto.getTitle() + " - " + bDto.getLibraryName());
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

                String key = bDto.getTitle() + "|" + bDto.getAuthorName();
                bookMap.put(key, book);

                List<Photo> photos = new ArrayList<>();
                if (bDto.getPhotos() != null) {
                    for (int i = 0; i < bDto.getPhotos().size(); i++) {
                        PhotoImportDto pDto = bDto.getPhotos().get(i);
                        if (pDto.getImageBase64() != null && !pDto.getImageBase64().isEmpty()) {
                            Photo photo = new Photo();
                            photo.setImage(Base64.getDecoder().decode(pDto.getImageBase64()));
                            photo.setContentType(pDto.getContentType());
                            photo.setCaption(pDto.getCaption());
                            photo.setPhotoOrder(i);
                            photo.setBook(book);
                            photos.add(photo);
                        }
                    }
                    if (!photos.isEmpty()) {
                        photoRepository.saveAll(photos);
                        book.setPhotos(photos);
                    }
                }
            }
        }

        if (dto.getLoans() != null) {
            for (LoanImportDto lDto : dto.getLoans()) {
                String key = lDto.getBookTitle() + "|" + lDto.getAuthorName();
                Book book = bookMap.get(key);
                if (book == null) {
                    throw new RuntimeException("Book not found for loan: " + lDto.getBookTitle() + " by " + lDto.getAuthorName());
                }
                User user = userMap.get(lDto.getUserUsername());
                if (user == null) {
                    throw new RuntimeException("User not found for loan: " + lDto.getUserUsername());
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
    }

    public ImportRequestDto exportData() {
        ImportRequestDto dto = new ImportRequestDto();

        // Export libraries
        List<LibraryDto> libDtos = libraryRepository.findAll().stream()
                .map(libraryMapper::toDto)
                .collect(Collectors.toList());
        dto.setLibraries(libDtos);

        // Export authors
        List<AuthorImportDto> authDtos = new ArrayList<>();
        for (Author author : authorRepository.findAll()) {
            AuthorImportDto aDto = new AuthorImportDto();
            aDto.setName(author.getName());
            aDto.setDateOfBirth(author.getDateOfBirth());
            aDto.setDateOfDeath(author.getDateOfDeath());
            aDto.setReligiousAffiliation(author.getReligiousAffiliation());
            aDto.setBirthCountry(author.getBirthCountry());
            aDto.setNationality(author.getNationality());
            aDto.setBriefBiography(author.getBriefBiography());

            List<PhotoImportDto> photoDtos = new ArrayList<>();
            if (author.getPhotos() != null) {
                for (int i = 0; i < author.getPhotos().size(); i++) {
                    Photo photo = author.getPhotos().get(i);
                    if (photo.getImage() != null && photo.getImage().length > 0) {
                        PhotoImportDto pDto = new PhotoImportDto();
                        pDto.setContentType(photo.getContentType());
                        pDto.setImageBase64(Base64.getEncoder().encodeToString(photo.getImage()));
                        pDto.setCaption(photo.getCaption());
                        photoDtos.add(pDto);
                    }
                }
            }
            aDto.setPhotos(photoDtos);
            authDtos.add(aDto);
        }
        dto.setAuthors(authDtos);

        // Export users (password set to empty for security)
        List<UserImportDto> userDtos = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            UserImportDto uDto = new UserImportDto();
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
        List<BookImportDto> bookDtos = new ArrayList<>();
        for (Book book : bookRepository.findAll()) {
            BookImportDto bDto = new BookImportDto();
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
                bDto.setAuthorName(book.getAuthor().getName());
            }
            if (book.getLibrary() != null) {
                bDto.setLibraryName(book.getLibrary().getName());
            }

            List<PhotoImportDto> photoDtos = new ArrayList<>();
            if (book.getPhotos() != null) {
                for (int i = 0; i < book.getPhotos().size(); i++) {
                    Photo photo = book.getPhotos().get(i);
                    if (photo.getImage() != null && photo.getImage().length > 0) {
                        PhotoImportDto pDto = new PhotoImportDto();
                        pDto.setContentType(photo.getContentType());
                        pDto.setImageBase64(Base64.getEncoder().encodeToString(photo.getImage()));
                        pDto.setCaption(photo.getCaption());
                        photoDtos.add(pDto);
                    }
                }
            }
            bDto.setPhotos(photoDtos);
            bookDtos.add(bDto);
        }
        dto.setBooks(bookDtos);

        // Export loans
        List<LoanImportDto> loanDtos = new ArrayList<>();
        for (Loan loan : loanRepository.findAll()) {
            LoanImportDto lDto = new LoanImportDto();
            if (loan.getBook() != null) {
                lDto.setBookTitle(loan.getBook().getTitle());
                if (loan.getBook().getAuthor() != null) {
                    lDto.setAuthorName(loan.getBook().getAuthor().getName());
                }
            }
            if (loan.getUser() != null) {
                lDto.setUserUsername(loan.getUser().getUsername());
            }
            lDto.setLoanDate(loan.getLoanDate());
            lDto.setDueDate(loan.getDueDate());
            lDto.setReturnDate(loan.getReturnDate());
            loanDtos.add(lDto);
        }
        dto.setLoans(loanDtos);

        return dto;
    }
}
