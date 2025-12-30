/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthorMapper {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private BookMapper bookMapper;

    public AuthorDto toDto(Author author) {
        return toDto(author, false);
    }

    public AuthorDto toDto(Author author, boolean includeBooks) {
        if (author == null) {
            return null;
        }

        AuthorDto dto = new AuthorDto();
        dto.setId(author.getId());
        dto.setName(author.getName());
        dto.setDateOfBirth(author.getDateOfBirth());
        dto.setDateOfDeath(author.getDateOfDeath());
        dto.setReligiousAffiliation(author.getReligiousAffiliation());
        dto.setBirthCountry(author.getBirthCountry());
        dto.setNationality(author.getNationality());
        dto.setBriefBiography(author.getBriefBiography());
        dto.setLastModified(author.getLastModified());

        // Use efficient queries to get first photo ID and checksum without loading photos collection
        Long firstPhotoId = photoRepository.findFirstPhotoIdByAuthorId(author.getId());
        if (firstPhotoId != null) {
            dto.setFirstPhotoId(firstPhotoId);
            String firstPhotoChecksum = photoRepository.findFirstPhotoChecksumByAuthorId(author.getId());
            if (firstPhotoChecksum != null) {
                dto.setFirstPhotoChecksum(firstPhotoChecksum);
            }
        }

        // Map books if requested and collection is initialized
        if (includeBooks && author.getBooks() != null) {
            dto.setBooks(author.getBooks().stream()
                    .map(bookMapper::toDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public Author toEntity(AuthorDto authorDto) {
        if (authorDto == null) {
            return null;
        }

        Author author = new Author();
        author.setId(authorDto.getId());
        author.setName(authorDto.getName());
        author.setDateOfBirth(authorDto.getDateOfBirth());
        author.setDateOfDeath(authorDto.getDateOfDeath());
        author.setReligiousAffiliation(authorDto.getReligiousAffiliation());
        author.setBirthCountry(authorDto.getBirthCountry());
        author.setNationality(authorDto.getNationality());
        author.setBriefBiography(authorDto.getBriefBiography());

        return author;
    }
}
