/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import com.muczynski.library.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorMapper {

    @Autowired
    private PhotoRepository photoRepository;

    public AuthorDto toDto(Author author) {
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

        // Use efficient query to get first photo ID without loading photos collection
        Long firstPhotoId = photoRepository.findFirstPhotoIdByAuthorId(author.getId());
        if (firstPhotoId != null) {
            dto.setFirstPhotoId(firstPhotoId);
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
