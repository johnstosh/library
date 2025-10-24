package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.RandomAuthor;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LibraryRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private RandomAuthor randomAuthor;

    @Autowired
    private AskGrok askGrok;

    @Autowired
    private ObjectMapper objectMapper;

    public BookDto createBook(BookDto bookDto) {
        Book book = bookMapper.toEntity(bookDto);
        book.setAuthor(authorRepository.findById(bookDto.getAuthorId()).orElseThrow(() -> new RuntimeException("Author not found: " + bookDto.getAuthorId())));
        book.setLibrary(libraryRepository.findById(bookDto.getLibraryId()).orElseThrow(() -> new RuntimeException("Library not found: " + bookDto.getLibraryId())));
        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toDto)
                .sorted(Comparator.comparing(bookDto -> {
                    String title = bookDto.getTitle().toLowerCase();
                    if (title.startsWith("the ")) {
                        return title.substring(4);
                    }
                    return title;
                }))
                .collect(Collectors.toList());
    }

    public BookDto getBookById(Long id) {
        return bookRepository.findById(id)
                .map(bookMapper::toDto)
                .orElse(null);
    }

    public BookDto updateBook(Long id, BookDto bookDto) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found: " + id));
        book.setTitle(bookDto.getTitle());
        book.setPublicationYear(bookDto.getPublicationYear());
        book.setPublisher(bookDto.getPublisher());
        book.setPlotSummary(bookDto.getPlotSummary());
        book.setRelatedWorks(bookDto.getRelatedWorks());
        book.setDetailedDescription(bookDto.getDetailedDescription());
        book.setDateAddedToLibrary(bookDto.getDateAddedToLibrary());
        if (bookDto.getStatus() != null) {
            book.setStatus(bookDto.getStatus());
        }
        if (bookDto.getAuthorId() != null) {
            book.setAuthor(authorRepository.findById(bookDto.getAuthorId()).orElseThrow(() -> new RuntimeException("Author not found: " + bookDto.getAuthorId())));
        }
        if (bookDto.getLibraryId() != null) {
            book.setLibrary(libraryRepository.findById(bookDto.getLibraryId()).orElseThrow(() -> new RuntimeException("Library not found: " + bookDto.getLibraryId())));
        }
        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Book not found: " + id);
        }
        long loanCount = loanRepository.countByBookId(id);
        if (loanCount > 0) {
            throw new RuntimeException("Cannot delete book because it is currently checked out with " + loanCount + " loan(s).");
        }
        bookRepository.deleteById(id);
    }

    private void handleRandomAuthor(BookDto dto) {
        Author randomAuthorEntity = randomAuthor.create();

        Pageable singlePage = PageRequest.of(0, 1);
        Page<Author> existingAuthors = authorRepository.findByNameContainingIgnoreCase(randomAuthorEntity.getName(), singlePage);

        Long selectedAuthorId;
        if (!existingAuthors.isEmpty()) {
            selectedAuthorId = existingAuthors.getContent().get(0).getId();
        } else {
            Author savedAuthor = authorRepository.save(randomAuthorEntity);
            selectedAuthorId = savedAuthor.getId();
        }

        dto.setAuthorId(selectedAuthorId);
        dto.setTitle("temporary title");
    }

    private Map<String, Object> extractJsonFromResponse(String response) {
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            throw new RuntimeException("No valid JSON found in response");
        }

        String jsonSubstring = response.substring(startIndex, endIndex + 1);
        String beforeJson = response.substring(0, startIndex).trim();
        String afterJson = response.substring(endIndex + 1).trim();

        if (!beforeJson.isEmpty() && !isAllWhitespace(beforeJson)) {
            logger.warn("Extraneous text before JSON: '{}'", beforeJson);
        }
        if (!afterJson.isEmpty() && !isAllWhitespace(afterJson)) {
            logger.warn("Extraneous text after JSON: '{}'", afterJson);
        }

        try {
            return objectMapper.readValue(jsonSubstring, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON from response: " + e.getMessage(), e);
        }
    }

    private boolean isAllWhitespace(String str) {
        return str.trim().isEmpty();
    }

    public BookDto generateTempBook(Long id) {
        BookDto dto = getBookById(id);
        if (dto == null) {
            throw new RuntimeException("Book not found: " + id);
        }

        List<Photo> photos = photoRepository.findByBookId(id);
        if (!photos.isEmpty()) {
            Photo photo = photos.get(0);
            String question = """
                Based on this book cover image, suggest a title and author name. 
                Respond only with a JSON object in this exact format: {"title": "[title]", "author": "[author]"}
                Do not include any other text before or after the JSON.""";

            try {
                String response = askGrok.askAboutPhoto(photo.getImage(), photo.getContentType(), question);
                Map<String, Object> jsonData = extractJsonFromResponse(response);

                String title = (String) jsonData.get("title");
                if (title == null || title.trim().isEmpty()) {
                    title = "temporary title";
                }
                dto.setTitle(title.trim());

                String authorName = (String) jsonData.get("author");
                Long authorId;
                if (authorName != null && !authorName.trim().isEmpty()) {
                    authorName = authorName.trim();
                    Author authorEntity = new Author();
                    authorEntity.setName(authorName);
                    authorEntity.setReligiousAffiliation("AI-generated");

                    Pageable singlePage = PageRequest.of(0, 1);
                    Page<Author> existingAuthors = authorRepository.findByNameContainingIgnoreCase(authorName, singlePage);

                    if (!existingAuthors.isEmpty()) {
                        authorId = existingAuthors.getContent().get(0).getId();
                    } else {
                        Author savedAuthor = authorRepository.save(authorEntity);
                        authorId = savedAuthor.getId();
                    }
                } else {
                    handleRandomAuthor(dto);
                    authorId = dto.getAuthorId();
                }
                dto.setAuthorId(authorId);
            } catch (Exception e) {
                logger.warn("Failed to generate book metadata from AI: {}", e.getMessage());
                if (e.getMessage().contains("xAI API key not configured")) {
                    throw new RuntimeException("xAI API key is required to generate book metadata from photo. Please set it in your user settings.");
                } else {
                    throw new RuntimeException("Failed to generate book metadata from AI: " + e.getMessage());
                }
            }
        } else {
            handleRandomAuthor(dto);
        }

        if (dto.getLibraryId() == null) {
            List<Library> libraries = libraryRepository.findAll();
            if (!libraries.isEmpty()) {
                dto.setLibraryId(libraries.get(0).getId());
            }
        }

        return updateBook(id, dto);
    }

}
