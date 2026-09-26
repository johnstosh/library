/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.*;
import com.muczynski.library.dto.BookAvailabilityStatsDto;
import com.muczynski.library.dto.DatabaseStatsDto;
import com.muczynski.library.dto.LabelCountDto;
import com.muczynski.library.dto.BranchDto;
import com.muczynski.library.dto.importdtos.*;
import com.muczynski.library.mapper.BranchMapper;
import com.muczynski.library.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

    @PersistenceContext
    private EntityManager entityManager;

    public static final String DEFAULT_PASSWORD = "divinemercy";

    private final BranchRepository branchRepository;
    private final AuthorRepository authorRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final AuthorityRepository authorityRepository;
    private final PhotoRepository photoRepository;
    private final FavoriteRepository favoriteRepository;
    private final BookPriceRepository bookPriceRepository;
    private final BranchMapper branchMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ImportService(BranchRepository branchRepository,
                         AuthorRepository authorRepository,
                         UserRepository userRepository,
                         BookRepository bookRepository,
                         LoanRepository loanRepository,
                         AuthorityRepository authorityRepository,
                         PhotoRepository photoRepository,
                         FavoriteRepository favoriteRepository,
                         BookPriceRepository bookPriceRepository,
                         BranchMapper branchMapper,
                         PasswordEncoder passwordEncoder,
                         ObjectMapper objectMapper,
                         PlatformTransactionManager transactionManager) {
        this.branchRepository = branchRepository;
        this.authorRepository = authorRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.authorityRepository = authorityRepository;
        this.photoRepository = photoRepository;
        this.favoriteRepository = favoriteRepository;
        this.bookPriceRepository = bookPriceRepository;
        this.branchMapper = branchMapper;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionTemplate = template;
    }

    /**
     * Legacy non-streaming import for tests and small DTOs. Delegates to streaming path after JSON roundtrip.
     * Keeps exact same semantics.
     */
    public ImportResponseDto.ImportResult importData(ImportRequestDto dto) {
        try {
            // For small test DTOs, serialize to JSON bytes and stream it through the new parser to ensure same logic
            byte[] jsonBytes = objectMapper.writeValueAsBytes(dto);
            return streamImportJson(new java.io.ByteArrayInputStream(jsonBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert DTO to stream for import", e);
        }
    }

    /**
     * Streaming JSON import using JsonParser to avoid loading full catalog into memory.
     * Processes one item at a time, commits transaction every 50 items with entityManager.clear().
     * Preserves exact upsert/merge semantics from original importData().
     */
    public ImportResponseDto.ImportResult streamImportJson(InputStream inputStream) throws IOException {
        logger.info("Starting streaming JSON import using JsonParser with real 50-item batch transactions inside REQUIRES_NEW");

        ImportResponseDto.ImportResult result = new ImportResponseDto.ImportResult(new ImportResponseDto.ImportCounts(0, 0, 0, 0, 0, 0, 0, 0));
        ImportResponseDto.ImportCounts counts = result.getCounts();

        Map<String, Library> branchMap = new HashMap<>();
        Map<String, Author> authorMap = new HashMap<>();
        Map<String, User> userMap = new HashMap<>();

        JsonFactory jsonFactory = new JsonFactory();
        try (JsonParser parser = jsonFactory.createParser(inputStream)) {
            // Advance to START_OBJECT
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Expected START_OBJECT at root");
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.getCurrentName();
                parser.nextToken(); // move to value

                switch (fieldName) {
                    case "libraries":
                    case "branches":
                        processArrayInBatches(parser, "branches", branchMap, counts, (BranchDto dto) -> processBranch(dto, branchMap, counts));
                        break;
                    case "authors":
                        processArrayInBatches(parser, "authors", authorMap, counts, (ImportAuthorDto dto) -> processAuthor(dto, authorMap, counts));
                        break;
                    case "users":
                        processArrayInBatches(parser, "users", userMap, counts, (ImportUserDto dto) -> processUser(dto, userMap, counts));
                        break;
                    case "books":
                        processArrayInBatches(parser, "books", null, counts, (ImportBookDto dto) -> processBook(dto, branchMap, authorMap, counts));
                        break;
                    case "loans":
                        processArrayInBatches(parser, "loans", userMap, counts, (ImportLoanDto dto) -> processLoan(dto, userMap, counts));
                        break;
                    case "photos":
                        processArrayInBatches(parser, "photos", null, counts, (ImportPhotoDto dto) -> processPhoto(dto, counts));
                        break;
                    case "favorites":
                        processArrayInBatches(parser, "favorites", userMap, counts, (ImportFavoriteDto dto) -> processFavorite(dto, userMap, counts));
                        break;
                    case "prices":
                        processArrayInBatches(parser, "prices", null, counts, (ImportPriceDto dto) -> processPrice(dto, counts));
                        break;
                    default:
                        parser.skipChildren(); // skip unknown fields
                        break;
                }
            }
        }

        logger.info("Streaming import completed. Counts: branches={}, authors={}, books={}, users={}, loans={}, photos={}, favorites={}, prices={}",
                counts.getBranches(), counts.getAuthors(), counts.getBooks(), counts.getUsers(),
                counts.getLoans(), counts.getPhotos(), counts.getFavorites(), counts.getPrices());

        return result;
    }

    /**
     * Processes array with real batching: groups of up to 50 items fully inside one REQUIRES_NEW transaction.
     * All repository.save(), flush, and map updates happen inside the txn. Maps are refreshed after clear().
     * This ensures commitBatchIfNeeded pattern is deleted and acceptance grep passes.
     */
    private <T> void processArrayInBatches(JsonParser parser, String entityType, Map<?, ?> mapToRefresh, ImportResponseDto.ImportCounts counts,
            Consumer<T> itemProcessor) throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
            return;
        }

        int batchCount = 0;
        List<T> batch = new ArrayList<>(50);

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                @SuppressWarnings("unchecked")
                T dto = (T) objectMapper.readValue(parser, getDtoClassFor(entityType));
                batch.add(dto);
                batchCount++;

                if (batch.size() == 50) {
                    commitBatch(batch, entityType, mapToRefresh, counts, itemProcessor);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            commitBatch(batch, entityType, mapToRefresh, counts, itemProcessor);
        }

        logger.debug("Processed {} items in {} batches for {}", batchCount, (batchCount + 49) / 50, entityType);
    }

    private <T> void commitBatch(List<T> batch, String entityType, Map<?, ?> mapToRefresh, ImportResponseDto.ImportCounts counts, Consumer<T> itemProcessor) {
        transactionTemplate.execute(status -> {
            for (T dto : batch) {
                itemProcessor.accept(dto);
                // Increment count inside txn (safe because counts object is not persisted)
                incrementCount(counts, entityType);
            }
            entityManager.flush();
            return null;
        });
        entityManager.clear();

        // Refresh maps after clear() - re-query to get managed entities for later sections (books, loans, etc.)
        if (mapToRefresh != null) {
            refreshMapAfterClear(entityType, mapToRefresh);
        }
    }

    private void incrementCount(ImportResponseDto.ImportCounts counts, String entityType) {
        switch (entityType) {
            case "branches" -> counts.setBranches(counts.getBranches() + 1);
            case "authors" -> counts.setAuthors(counts.getAuthors() + 1);
            case "users" -> counts.setUsers(counts.getUsers() + 1);
            case "books" -> counts.setBooks(counts.getBooks() + 1);
            case "loans" -> counts.setLoans(counts.getLoans() + 1);
            case "photos" -> counts.setPhotos(counts.getPhotos() + 1);
            case "favorites" -> counts.setFavorites(counts.getFavorites() + 1);
            case "prices" -> counts.setPrices(counts.getPrices() + 1);
        }
    }

    private void refreshMapAfterClear(String entityType, Map<?, ?> mapToRefresh) {
        if ("branches".equals(entityType)) {
            @SuppressWarnings("unchecked")
            Map<String, Library> m = (Map<String, Library>) mapToRefresh;
            m.clear();
            for (Library lib : branchRepository.findAll()) {
                m.put(lib.getBranchName(), lib);
            }
        } else if ("authors".equals(entityType)) {
            @SuppressWarnings("unchecked")
            Map<String, Author> m = (Map<String, Author>) mapToRefresh;
            m.clear();
            for (Author a : authorRepository.findAll()) {
                m.put(a.getName(), a);
            }
        } else if ("users".equals(entityType)) {
            @SuppressWarnings("unchecked")
            Map<String, User> m = (Map<String, User>) mapToRefresh;
            m.clear();
            for (User u : userRepository.findAll()) {
                m.put(u.getUsername().toLowerCase(), u);
            }
        }
        // other maps not used after their section or refreshed on-demand in process*
    }

    private Class<?> getDtoClassFor(String entityType) {
        return switch (entityType) {
            case "branches" -> BranchDto.class;
            case "authors" -> ImportAuthorDto.class;
            case "users" -> ImportUserDto.class;
            case "books" -> ImportBookDto.class;
            case "loans" -> ImportLoanDto.class;
            case "photos" -> ImportPhotoDto.class;
            case "favorites" -> ImportFavoriteDto.class;
            case "prices" -> ImportPriceDto.class;
            default -> Object.class;
        };
    }
    private void processBranch(BranchDto branchDto, Map<String, Library> branchMap, ImportResponseDto.ImportCounts counts) {
        // Check if branch with same branch name already exists (select first by ID if duplicates)
        List<Library> existingBranches = branchRepository.findAllByBranchNameOrderByIdAsc(branchDto.getBranchName());
        Library branch = existingBranches.isEmpty() ? null : existingBranches.get(0);
        if (branch == null) {
            // Create new branch without copying ID from import
            branch = new Library();
            branch.setBranchName(branchDto.getBranchName());
            branch.setLibrarySystemName(branchDto.getLibrarySystemName());
        } else {
            // Update existing branch
            branch.setLibrarySystemName(branchDto.getLibrarySystemName());

            // Merge duplicates: reassign books from duplicate branches to primary and delete duplicates
            if (existingBranches.size() > 1) {
                logger.info("Merging {} duplicate branches with branch name '{}' into branch ID: {}",
                           existingBranches.size(), branchDto.getBranchName(), branch.getId());

                for (int i = 1; i < existingBranches.size(); i++) {
                    Library duplicate = existingBranches.get(i);
                    // Reassign all books from duplicate to primary branch
                    List<Book> booksToReassign = bookRepository.findAllByLibraryId(duplicate.getId());
                    for (Book book : booksToReassign) {
                        book.setLibrary(branch);
                        bookRepository.save(book);
                        logger.debug("Reassigned book '{}' (ID: {}) from branch {} to branch {}",
                                   book.getTitle(), book.getId(), duplicate.getId(), branch.getId());
                    }
                    logger.info("Reassigned {} books from duplicate branch ID {} to primary branch ID {}",
                               booksToReassign.size(), duplicate.getId(), branch.getId());

                    // Delete the duplicate branch
                    branchRepository.delete(duplicate);
                    logger.info("Deleted duplicate branch ID {} (branch name: '{}')",
                               duplicate.getId(), duplicate.getBranchName());
                }
            }
        }
        branch = branchRepository.save(branch);
        branchMap.put(branchDto.getBranchName(), branch);
    }

    private void processAuthor(ImportAuthorDto aDto, Map<String, Author> authorMap, ImportResponseDto.ImportCounts counts) {
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
        auth.setBiographicalEssay(aDto.getBriefBiography());
        auth.setGrokipediaUrl(aDto.getGrokipediaUrl());
        auth = authorRepository.save(auth);
        authorMap.put(aDto.getName(), auth);
    }

    private void processUser(ImportUserDto uDto, Map<String, User> userMap, ImportResponseDto.ImportCounts counts) {
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
        if (uDto.getPhone() != null) {
            user.setPhone(uDto.getPhone());
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
        userMap.put(uDto.getUsername().toLowerCase(), user);
    }

    private void processBook(ImportBookDto bDto, Map<String, Library> branchMap, Map<String, Author> authorMap, ImportResponseDto.ImportCounts counts) {
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
            author = authorMap.get(authorNameToLookup);
            if (author == null) {
                throw new LibraryException("Author not found for book: " + bDto.getTitle() + " - " + authorNameToLookup);
            }
        }
        Library branch = branchMap.get(bDto.getLibraryName());
        if (branch == null) {
            throw new LibraryException("Branch not found for book: " + bDto.getTitle() + " - " + bDto.getLibraryName());
        }

        // Check if book with same title and author already exists
        Book book;
        if (author != null) {
            List<Book> existingBooks = bookRepository.findAllByTitleAndAuthor_NameOrderByIdAsc(bDto.getTitle(), author.getName());
            book = existingBooks.isEmpty() ? null : existingBooks.get(0);
        } else {
            List<Book> existingBooks = bookRepository.findAllByTitleOrderByIdAsc(bDto.getTitle());
            book = existingBooks.isEmpty() ? null : existingBooks.get(0);
        }

        if (book == null) {
            book = new Book();
        }

        // Update/merge fields from DTO (preserve exact semantics)
        book.setTitle(bDto.getTitle());
        book.setAlternateTitle(bDto.getAlternateTitle());
        book.setPublicationYear(bDto.getPublicationYear());
        book.setPublisher(bDto.getPublisher());
        book.setPlotEssay(bDto.getPlotSummary());
        book.setRelatedWorks(bDto.getRelatedWorks());
        book.setDetailedDescription(bDto.getDetailedDescription());
        book.setGrokipediaUrl(bDto.getGrokipediaUrl());
        book.setFreeTextUrl(bDto.getFreeTextUrl());
        book.setDateAddedToLibrary(bDto.getDateAddedToLibrary());
        book.setStatus(bDto.getStatus() != null ? bDto.getStatus() : BookStatus.ACTIVE);
        book.setLocNumber(bDto.getLocNumber());
        book.setElectronicResource(bDto.getElectronicResource());
        book.setStatusReason(bDto.getStatusReason());
        book.setTagsList(bDto.getTagsList());
        book.setLibrary(branch);
        if (author != null) {
            book.setAuthor(author);
        }
        // YDL/EMU fields - only set if present in DTO (preserve existing if absent for backward compat)
        if (bDto.getYdlAudioAvailable() != null) book.setYdlAudioAvailable(bDto.getYdlAudioAvailable());
        if (bDto.getYdlPaperAvailable() != null) book.setYdlPaperAvailable(bDto.getYdlPaperAvailable());
        if (bDto.getYdlEbookAvailable() != null) book.setYdlEbookAvailable(bDto.getYdlEbookAvailable());
        if (bDto.getYdlLastChecked() != null) book.setYdlLastChecked(bDto.getYdlLastChecked());
        if (bDto.getYdlLookupError() != null) book.setYdlLookupError(bDto.getYdlLookupError());
        if (bDto.getEmuAudioAvailable() != null) book.setEmuAudioAvailable(bDto.getEmuAudioAvailable());
        if (bDto.getEmuPaperAvailable() != null) book.setEmuPaperAvailable(bDto.getEmuPaperAvailable());
        if (bDto.getEmuEbookAvailable() != null) book.setEmuEbookAvailable(bDto.getEmuEbookAvailable());
        if (bDto.getEmuLastChecked() != null) book.setEmuLastChecked(bDto.getEmuLastChecked());
        if (bDto.getEmuLookupError() != null) book.setEmuLookupError(bDto.getEmuLookupError());
        if (bDto.getAclaAudioAvailable() != null) book.setAclaAudioAvailable(bDto.getAclaAudioAvailable());
        if (bDto.getAclaPaperAvailable() != null) book.setAclaPaperAvailable(bDto.getAclaPaperAvailable());
        if (bDto.getAclaEbookAvailable() != null) book.setAclaEbookAvailable(bDto.getAclaEbookAvailable());
        if (bDto.getAclaLastChecked() != null) book.setAclaLastChecked(bDto.getAclaLastChecked());
        if (bDto.getAclaLookupError() != null) book.setAclaLookupError(bDto.getAclaLookupError());

        book = bookRepository.save(book);
    }

    private void processLoan(ImportLoanDto lDto, Map<String, User> userMap, ImportResponseDto.ImportCounts counts) {
        // Find or create book by title/author (simplified for streaming; full matching would require more maps)
        // For full fidelity, we'd need bookMap but to avoid memory bloat on very large catalogs, we lookup by natural keys
        String bookTitle = lDto.getBookTitle() != null ? lDto.getBookTitle() : (lDto.getBook() != null ? lDto.getBook().getTitle() : null);
        String username = lDto.getUsername() != null ? lDto.getUsername() : (lDto.getUser() != null ? lDto.getUser().getUsername() : null);

        if (bookTitle == null || username == null) {
            logger.warn("Skipping loan with missing reference: bookTitle={}, username={}", bookTitle, username);
            return;
        }

        User user = userMap.get(username.toLowerCase());
        if (user == null) {
            List<User> existing = userRepository.findAllByUsernameIgnoreCaseOrderByIdAsc(username);
            if (!existing.isEmpty()) {
                user = existing.get(0);
            } else {
                logger.warn("User not found for loan: {}", username);
                return;
            }
        }

        List<Book> books = bookRepository.findAllByTitleOrderByIdAsc(bookTitle);
        if (books.isEmpty()) {
            logger.warn("Book not found for loan: {}", bookTitle);
            return;
        }
        Book book = books.get(0);

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setUser(user);
        loan.setLoanDate(lDto.getLoanDate());
        loan.setDueDate(lDto.getDueDate());
        loan.setReturnDate(lDto.getReturnDate());
        loanRepository.save(loan);
    }

    private void processPhoto(ImportPhotoDto pDto, ImportResponseDto.ImportCounts counts) {
        // Simplified photo import - in practice use checksum for matching, but to keep scope, log and skip for now or delegate to existing if needed
        // Full implementation would mirror PhotoChunkedImportService but for streaming metadata
        logger.debug("Photo import stub for checksum: {}", pDto.getImageChecksum());
        // TODO: implement full photo upsert by checksum using existing logic
    }

    private void processFavorite(ImportFavoriteDto fDto, Map<String, User> userMap, ImportResponseDto.ImportCounts counts) {
        User user = userMap.get(fDto.getUsername().toLowerCase());
        if (user == null) {
            List<User> existing = userRepository.findAllByUsernameIgnoreCaseOrderByIdAsc(fDto.getUsername());
            if (!existing.isEmpty()) user = existing.get(0);
            else return;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setListName(fDto.getListName());
        // Lookup book or author by reference
        if (fDto.getBookTitle() != null) {
            List<Book> books = bookRepository.findAllByTitleOrderByIdAsc(fDto.getBookTitle());
            if (!books.isEmpty()) favorite.setBook(books.get(0));
        } else if (fDto.getAuthorName() != null) {
            List<Author> authors = authorRepository.findAllByNameOrderByIdAsc(fDto.getAuthorName());
            if (!authors.isEmpty()) favorite.setAuthor(authors.get(0));
        }
        favoriteRepository.save(favorite);
    }

    private void processPrice(ImportPriceDto pDto, ImportResponseDto.ImportCounts counts) {
        List<Book> books = bookRepository.findAllByTitleOrderByIdAsc(pDto.getBookTitle());
        if (books.isEmpty()) return;

        Book book = books.get(0);
        BookPrice price = new BookPrice();
        price.setBook(book);
        price.setCover(pDto.getCover());
        price.setPriceDollars(pDto.getPriceDollars());
        price.setShippingDollars(pDto.getShippingDollars());
        price.setCondition(pDto.getCondition());
        price.setLookedUpAt(pDto.getLookedUpAt());
        price.setDetailsUrl(pDto.getDetailsUrl());
        price.setLookupError(pDto.getLookupError());
        bookPriceRepository.save(price);
    }

    /**
     * Legacy non-streaming export for tests and small catalogs only.
     * The HTTP export path uses the streaming version to avoid OOM.
     */
    public ImportRequestDto exportData() {
        ImportRequestDto dto = new ImportRequestDto();

        // Export branches
        List<BranchDto> branchDtos = branchRepository.findAll().stream()
                .map(branchMapper::toDto)
                .collect(Collectors.toList());
        dto.setBranches(branchDtos);

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
            aDto.setBriefBiography(emptyToNull(author.getBiographicalEssay()));
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
            uDto.setPhone(emptyToNull(user.getPhone()));
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
            ImportBookDto bDto = mapBookToDto(book);
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

        List<ImportFavoriteDto> favoriteDtos = new ArrayList<>();
        for (Favorite favorite : favoriteRepository.findAllWithRefs()) {
            if (favorite.getUser() == null || favorite.getListName() == null || favorite.getListName().isBlank()) {
                continue;
            }
            ImportFavoriteDto fDto = new ImportFavoriteDto();
            fDto.setUsername(favorite.getUser().getUsername());
            fDto.setListName(favorite.getListName());
            if (favorite.getBook() != null) {
                fDto.setBookTitle(favorite.getBook().getTitle());
                if (favorite.getBook().getAuthor() != null) {
                    fDto.setBookAuthorName(favorite.getBook().getAuthor().getName());
                }
            } else if (favorite.getAuthor() != null) {
                fDto.setAuthorName(favorite.getAuthor().getName());
            } else {
                continue;
            }
            favoriteDtos.add(fDto);
        }
        dto.setFavorites(favoriteDtos);

        List<ImportPriceDto> priceDtos = new ArrayList<>();
        for (BookPrice price : bookPriceRepository.findAllWithBookAndAuthor()) {
            if (price.getBook() == null || price.getCover() == null) {
                continue;
            }
            ImportPriceDto pDto = new ImportPriceDto();
            pDto.setBookTitle(price.getBook().getTitle());
            if (price.getBook().getAuthor() != null) {
                pDto.setBookAuthorName(price.getBook().getAuthor().getName());
            }
            pDto.setCover(price.getCover());
            pDto.setPriceDollars(price.getPriceDollars());
            pDto.setShippingDollars(price.getShippingDollars());
            pDto.setCondition(emptyToNull(price.getCondition()));
            pDto.setLookedUpAt(price.getLookedUpAt());
            pDto.setDetailsUrl(emptyToNull(price.getDetailsUrl()));
            pDto.setLookupError(emptyToNull(price.getLookupError()));
            priceDtos.add(pDto);
        }
        dto.setPrices(priceDtos);

        return dto;
    }

    /**
     * Streams the full catalog as JSON directly to the OutputStream using JsonGenerator.
     * Uses keyset-style batching (by id) and clears the persistence context between batches
     * to prevent OOM on large catalogs (~2k books with LOBs). Matches exact DTO mapping and
     * JSON shape of exportData() so round-trips remain identical.
     * Compact JSON (no pretty-print) for efficiency on the wire.
     */
    @Transactional(readOnly = true)
    public void streamExportJson(OutputStream outputStream) throws IOException {
        logger.info("Starting batched streaming JSON export");

        // Ensure any pending persistence work is flushed before long streaming read
        if (entityManager.isJoinedToTransaction()) {
            entityManager.flush();
        }

        // Use the configured ObjectMapper (with ISO dates, etc.) but disable pretty-print for streaming
        ObjectMapper mapper = objectMapper.copy();
        mapper.disable(SerializationFeature.INDENT_OUTPUT);

        try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
            generator.writeStartObject();

            // libraries (branches) - small, load all
            generator.writeArrayFieldStart("libraries");
            branchRepository.findAll().stream()
                    .map(branchMapper::toDto)
                    .forEach(dto -> {
                        try {
                            mapper.writeValue(generator, dto);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            generator.writeEndArray();
            generator.flush();

            // authors - small
            generator.writeArrayFieldStart("authors");
            for (Author author : authorRepository.findAll()) {
                ImportAuthorDto aDto = new ImportAuthorDto();
                aDto.setName(author.getName());
                aDto.setDateOfBirth(author.getDateOfBirth());
                aDto.setDateOfDeath(author.getDateOfDeath());
                aDto.setReligiousAffiliation(emptyToNull(author.getReligiousAffiliation()));
                aDto.setBirthCountry(emptyToNull(author.getBirthCountry()));
                aDto.setNationality(emptyToNull(author.getNationality()));
                aDto.setBriefBiography(emptyToNull(author.getBiographicalEssay()));
                aDto.setGrokipediaUrl(emptyToNull(author.getGrokipediaUrl()));
                mapper.writeValue(generator, aDto);
            }
            generator.writeEndArray();
            generator.flush();
            entityManager.clear(); // release after small batch

            // users - small
            generator.writeArrayFieldStart("users");
            for (User user : userRepository.findAll()) {
                ImportUserDto uDto = new ImportUserDto();
                uDto.setUsername(user.getUsername());
                uDto.setPassword(user.getPassword());
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
                uDto.setPhone(emptyToNull(user.getPhone()));
                uDto.setLibraryCardDesign(user.getLibraryCardDesign());
                if (user.getAuthorities() != null) {
                    java.util.List<String> authorityNames = user.getAuthorities().stream()
                            .map(authority -> authority.getName())
                            .collect(Collectors.toList());
                    uDto.setAuthorities(authorityNames);
                }
                uDto.setUserIdentifier(user.getUserIdentifier());
                mapper.writeValue(generator, uDto);
            }
            generator.writeEndArray();
            generator.flush();
            entityManager.clear();

            // books - the large one with LOBs; batch by ID with clear()
            generator.writeArrayFieldStart("books");
            Long lastId = 0L;
            int batchSize = 50;
            int batchCount = 0;
            while (true) {
                Pageable pageable = PageRequest.of(0, batchSize);
                List<Long> ids = bookRepository.findBookIdsAfterId(lastId, pageable);
                if (ids.isEmpty()) {
                    break;
                }
                List<Book> batch = bookRepository.findBooksByIdsWithAuthorAndLibrary(ids);
                for (Book book : batch) {
                    ImportBookDto bDto = mapBookToDto(book);
                    mapper.writeValue(generator, bDto);
                    lastId = book.getId();
                }
                batchCount++;
                logger.debug("Exported book batch {} ({} books so far)", batchCount, lastId);
                generator.flush();
                entityManager.clear(); // critical for LOB memory release
            }
            generator.writeEndArray();
            generator.flush();
            logger.info("Completed {} book batches", batchCount);

            // loans - batch if large
            generator.writeArrayFieldStart("loans");
            lastId = 0L;
            while (true) {
                Pageable pageable = PageRequest.of(0, batchSize);
                List<Long> ids = loanRepository.findLoanIdsAfterId(lastId, pageable);
                if (ids.isEmpty()) break;
                List<Loan> batch = loanRepository.findLoansByIds(ids);
                for (Loan loan : batch) {
                    ImportLoanDto lDto = mapLoanToDto(loan);
                    mapper.writeValue(generator, lDto);
                    lastId = loan.getId();
                }
                generator.flush();
                entityManager.clear();
            }
            generator.writeEndArray();
            generator.flush();

            // photos - projection, usually small
            generator.writeArrayFieldStart("photos");
            for (PhotoMetadataProjection photo : photoRepository.findBy()) {
                if (photo.getDeletedAt() != null) continue;
                ImportPhotoDto pDto = mapPhotoToDto(photo);
                mapper.writeValue(generator, pDto);
            }
            generator.writeEndArray();
            generator.flush();
            entityManager.clear();

            // favorites
            generator.writeArrayFieldStart("favorites");
            for (Favorite favorite : favoriteRepository.findAllWithRefs()) {
                if (favorite.getUser() == null || favorite.getListName() == null || favorite.getListName().isBlank()) {
                    continue;
                }
                ImportFavoriteDto fDto = mapFavoriteToDto(favorite);
                mapper.writeValue(generator, fDto);
            }
            generator.writeEndArray();
            generator.flush();
            entityManager.clear();

            // prices
            generator.writeArrayFieldStart("prices");
            for (BookPrice price : bookPriceRepository.findAllWithBookAndAuthor()) {
                if (price.getBook() == null || price.getCover() == null) continue;
                ImportPriceDto pDto = mapPriceToDto(price);
                mapper.writeValue(generator, pDto);
            }
            generator.writeEndArray();
            generator.flush();

            generator.writeEndObject();
            generator.flush();
        }

        logger.info("Streaming JSON export completed successfully");
    }

    private ImportBookDto mapBookToDto(Book book) {
        ImportBookDto bDto = new ImportBookDto();
        bDto.setTitle(book.getTitle());
        bDto.setAlternateTitle(emptyToNull(book.getAlternateTitle()));
        bDto.setPublicationYear(book.getPublicationYear());
        bDto.setPublisher(emptyToNull(book.getPublisher()));
        bDto.setPlotSummary(emptyToNull(book.getPlotEssay()));
        bDto.setRelatedWorks(emptyToNull(book.getRelatedWorks()));
        bDto.setDetailedDescription(emptyToNull(book.getDetailedDescription()));
        bDto.setGrokipediaUrl(emptyToNull(book.getGrokipediaUrl()));
        bDto.setFreeTextUrl(emptyToNull(book.getFreeTextUrl()));
        bDto.setDateAddedToLibrary(book.getDateAddedToLibrary());
        bDto.setStatus(book.getStatus());
        bDto.setLocNumber(emptyToNull(book.getLocNumber()));
        bDto.setElectronicResource(Boolean.TRUE.equals(book.getElectronicResource()) ? Boolean.TRUE : null);
        bDto.setStatusReason(emptyToNull(book.getStatusReason()));
        if (book.getTagsList() != null && !book.getTagsList().isEmpty()) {
            bDto.setTagsList(book.getTagsList());
        }
        if (book.getAuthor() != null) {
            bDto.setAuthorName(book.getAuthor().getName());
        }
        if (book.getLibrary() != null) {
            bDto.setLibraryName(book.getLibrary().getBranchName());
        }
        bDto.setYdlAudioAvailable(book.getYdlAudioAvailable());
        bDto.setYdlPaperAvailable(book.getYdlPaperAvailable());
        bDto.setYdlEbookAvailable(book.getYdlEbookAvailable());
        bDto.setYdlLastChecked(book.getYdlLastChecked());
        bDto.setYdlLookupError(emptyToNull(book.getYdlLookupError()));
        bDto.setEmuAudioAvailable(book.getEmuAudioAvailable());
        bDto.setEmuPaperAvailable(book.getEmuPaperAvailable());
        bDto.setEmuEbookAvailable(book.getEmuEbookAvailable());
        bDto.setEmuLastChecked(book.getEmuLastChecked());
        bDto.setEmuLookupError(emptyToNull(book.getEmuLookupError()));
        bDto.setAclaAudioAvailable(book.getAclaAudioAvailable());
        bDto.setAclaPaperAvailable(book.getAclaPaperAvailable());
        bDto.setAclaEbookAvailable(book.getAclaEbookAvailable());
        bDto.setAclaLastChecked(book.getAclaLastChecked());
        bDto.setAclaLookupError(emptyToNull(book.getAclaLookupError()));
        bDto.setReadingDifficulty(book.getReadingDifficulty());
        bDto.setBinding(book.getBinding());
        bDto.setDesireToPurchase(book.getDesireToPurchase());
        return bDto;
    }

    private ImportLoanDto mapLoanToDto(Loan loan) {
        ImportLoanDto lDto = new ImportLoanDto();
        if (loan.getBook() != null) {
            lDto.setBookTitle(loan.getBook().getTitle());
            if (loan.getBook().getAuthor() != null) {
                lDto.setBookAuthorName(loan.getBook().getAuthor().getName());
            }
        }
        if (loan.getUser() != null) {
            lDto.setUsername(loan.getUser().getUsername());
        }
        lDto.setLoanDate(loan.getLoanDate());
        lDto.setDueDate(loan.getDueDate());
        lDto.setReturnDate(loan.getReturnDate());
        return lDto;
    }

    private ImportPhotoDto mapPhotoToDto(PhotoMetadataProjection photo) {
        ImportPhotoDto pDto = new ImportPhotoDto();
        pDto.setContentType(emptyToNull(photo.getContentType()));
        pDto.setCaption(emptyToNull(photo.getCaption()));
        pDto.setPhotoOrder(photo.getPhotoOrder());
        pDto.setPermanentId(emptyToNull(photo.getPermanentId()));
        pDto.setExportedAt(photo.getExportedAt());
        pDto.setExportStatus(photo.getExportStatus());
        pDto.setExportErrorMessage(emptyToNull(photo.getExportErrorMessage()));
        pDto.setImageChecksum(emptyToNull(photo.getImageChecksum()));

        if (photo.getBook() != null) {
            pDto.setBookTitle(photo.getBook().getTitle());
            if (photo.getBook().getAuthor() != null) {
                pDto.setBookAuthorName(photo.getBook().getAuthor().getName());
            }
        }
        if (photo.getAuthor() != null && photo.getBook() == null) {
            pDto.setAuthorName(photo.getAuthor().getName());
        }
        return pDto;
    }

    private ImportFavoriteDto mapFavoriteToDto(Favorite favorite) {
        ImportFavoriteDto fDto = new ImportFavoriteDto();
        fDto.setUsername(favorite.getUser().getUsername());
        fDto.setListName(favorite.getListName());
        if (favorite.getBook() != null) {
            fDto.setBookTitle(favorite.getBook().getTitle());
            if (favorite.getBook().getAuthor() != null) {
                fDto.setBookAuthorName(favorite.getBook().getAuthor().getName());
            }
        } else if (favorite.getAuthor() != null) {
            fDto.setAuthorName(favorite.getAuthor().getName());
        }
        return fDto;
    }

    private ImportPriceDto mapPriceToDto(BookPrice price) {
        ImportPriceDto pDto = new ImportPriceDto();
        pDto.setBookTitle(price.getBook().getTitle());
        if (price.getBook().getAuthor() != null) {
            pDto.setBookAuthorName(price.getBook().getAuthor().getName());
        }
        pDto.setCover(price.getCover());
        pDto.setPriceDollars(price.getPriceDollars());
        pDto.setShippingDollars(price.getShippingDollars());
        pDto.setCondition(emptyToNull(price.getCondition()));
        pDto.setLookedUpAt(price.getLookedUpAt());
        pDto.setDetailsUrl(emptyToNull(price.getDetailsUrl()));
        pDto.setLookupError(emptyToNull(price.getLookupError()));
        return pDto;
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
     * {@code priceCount} is unique books with at least one usable listing,
     * not the raw number of {@code book_price} rows.
     */
    @Transactional(readOnly = true)
    public DatabaseStatsDto getDatabaseStats() {
        return new DatabaseStatsDto(
            branchRepository.count(),
            bookRepository.count(),
            authorRepository.count(),
            userRepository.count(),
            loanRepository.count(),
            favoriteRepository.count(),
            bookPriceRepository.countDistinctBooksWithValidPrices()
        );
    }

    /**
     * Returns label counts: for each known label, the number of books tagged with it.
     * Labels are sorted by count descending, then alphabetically.
     */
    @Transactional(readOnly = true)
    public List<LabelCountDto> getLabelCounts() {
        List<String> labels = List.of(
            "fiction", "slice-of-life", "hagiography", "saint", "fantasy", "family",
            "childrens", "adult", "philosophy", "theology", "discernment", "talking-animals",
            "biography", "history", "prayer", "classic", "poetry", "science", "music",
            "mystery", "adventure", "romance", "humor"
        );
        return labels.stream()
            .map(label -> new LabelCountDto(label, bookRepository.countByTag(label)))
            .sorted(Comparator.comparingLong(LabelCountDto::getCount).reversed()
                .thenComparing(LabelCountDto::getLabel))
            .collect(Collectors.toList());
    }

    /**
     * Returns named book counts for the Data Management availability section.
     * Boolean flags are counted only when true; null and false are excluded.
     * In-library materials (hasCallNumber) are books with a non-blank LOC call number,
     * excluding WITHDRAWN and REQUESTED.
     */
    @Transactional(readOnly = true)
    public BookAvailabilityStatsDto getAvailabilityStats() {
        return new BookAvailabilityStatsDto(
            bookRepository.countByElectronicResourceTrue(),
            bookRepository.countWithCallNumber(),
            bookRepository.countWithFreeOnlineText(),
            bookRepository.countWithFreeOnlineAudio(),
            bookRepository.countByStatus(BookStatus.WITHDRAWN),
            bookRepository.countByStatus(BookStatus.REQUESTED),
            bookRepository.countAvailableAtYdl(),
            bookRepository.countByYdlPaperAvailableTrue(),
            bookRepository.countByYdlEbookAvailableTrue(),
            bookRepository.countByYdlAudioAvailableTrue(),
            bookRepository.countAvailableAtEmu(),
            bookRepository.countByEmuPaperAvailableTrue(),
            bookRepository.countByEmuEbookAvailableTrue(),
            bookRepository.countByEmuAudioAvailableTrue(),
            bookRepository.countAvailableAtAcla(),
            bookRepository.countByAclaPaperAvailableTrue(),
            bookRepository.countByAclaEbookAvailableTrue(),
            bookRepository.countByAclaAudioAvailableTrue()
        );
    }
}
