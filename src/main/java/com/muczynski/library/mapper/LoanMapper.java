/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Loan;
import com.muczynski.library.dto.LoanDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    LoanDto toDto(Loan loan);
}
