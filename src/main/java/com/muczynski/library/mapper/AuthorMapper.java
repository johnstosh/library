package com.muczynski.library.mapper;

import com.muczynski.library.domain.Author;
import com.muczynski.library.dto.AuthorDto;
import org.springframework.stereotype.Component;

@Component
public class AuthorMapper {

    public AuthorDto toDto(Author author) {
        AuthorDto authorDto = new AuthorDto();
        authorDto.setId(author.getId());
        authorDto.setName(author.getName());
        authorDto.setDateOfBirth(author.getDateOfBirth());
        authorDto.setDateOfDeath(author.getDateOfDeath());
        authorDto.setReligiousAffiliation(author.getReligiousAffiliation());
        authorDto.setBirthCountry(author.getBirthCountry());
        authorDto.setNationality(author.getNationality());
        authorDto.setBriefBiography(author.getBriefBiography());
        return authorDto;
    }

    public Author toEntity(AuthorDto authorDto) {
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