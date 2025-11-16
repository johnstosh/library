/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final Path rootLocation = Paths.get("uploads");

    public StorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            logger.error("Failed to initialize storage directory at {}: {}", rootLocation, e.getMessage(), e);
            throw new LibraryException("Could not initialize storage", e);
        }
    }

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                logger.debug("Attempted to store empty file: {}", file.getOriginalFilename());
                throw new LibraryException("Failed to store empty file.");
            }
            String filename = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename));
            return filename;
        } catch (IOException e) {
            logger.error("Failed to store file {} due to IO error: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new LibraryException("Failed to store file.", e);
        }
    }

    public void delete(String filename) {
        try {
            Path filePath = rootLocation.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Failed to delete file {} due to IO error: {}", filename, e.getMessage(), e);
            throw new LibraryException("Failed to delete file.", e);
        }
    }
}
