package com.muczynski.library.controller;

import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.dto.PhotoDto;
import com.muczynski.library.service.AuthorService;
import com.muczynski.library.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorController.class);

    @Autowired
    private AuthorService authorService;

    @Autowired
    private PhotoService photoService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAllAuthors() {
        try {
            List<AuthorDto> authors = authorService.getAllAuthors();
            return ResponseEntity.ok(authors);
        } catch (Exception e) {
            logger.debug("Failed to retrieve all authors: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/photos")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getPhotosByAuthorId(@PathVariable Long id) {
        try {
            List<PhotoDto> photos = photoService.getPhotosByAuthorId(id);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            logger.debug("Failed to retrieve photos for author ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAuthorById(@PathVariable Long id) {
        try {
            AuthorDto author = authorService.getAuthorById(id);
            return author != null ? ResponseEntity.ok(author) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.debug("Failed to retrieve author by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/photos")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> addPhotoToAuthor(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            PhotoDto newPhoto = photoService.addPhotoToAuthor(id, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(newPhoto);
        } catch (Exception e) {
            logger.debug("Failed to add photo to author ID {} with file {}: {}", id, file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> createAuthor(@RequestBody AuthorDto authorDto) {
        try {
            AuthorDto created = authorService.createAuthor(authorDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.debug("Failed to create author with DTO {}: {}", authorDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> updateAuthor(@PathVariable Long id, @RequestBody AuthorDto authorDto) {
        try {
            AuthorDto updated = authorService.updateAuthor(id, authorDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.debug("Failed to update author ID {} with DTO {}: {}", id, authorDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        try {
            authorService.deleteAuthor(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.debug("Failed to delete author ID {}: {}", id, e.getMessage(), e);
            if (e.getMessage().contains("associated books")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    @DeleteMapping("/{authorId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<Void> deleteAuthorPhoto(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.deleteAuthorPhoto(authorId, photoId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.debug("Failed to delete photo ID {} for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/rotate-cw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> rotatePhotoCW(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.rotateAuthorPhoto(authorId, photoId, true);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.debug("Failed to rotate photo ID {} clockwise for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/rotate-ccw")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> rotatePhotoCCW(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.rotateAuthorPhoto(authorId, photoId, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.debug("Failed to rotate photo ID {} counter-clockwise for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/move-left")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> moveAuthorPhotoLeft(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.moveAuthorPhotoLeft(authorId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.debug("Failed to move photo ID {} left for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{authorId}/photos/{photoId}/move-right")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<?> moveAuthorPhotoRight(@PathVariable Long authorId, @PathVariable Long photoId) {
        try {
            photoService.moveAuthorPhotoRight(authorId, photoId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.debug("Failed to move photo ID {} right for author ID {}: {}", photoId, authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
