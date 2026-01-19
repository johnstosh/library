/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.mapper;

import com.muczynski.library.domain.Loan;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.LoanDto;
import com.muczynski.library.repository.PhotoRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class LoanMapper {

    private static final Logger logger = LoggerFactory.getLogger(LoanMapper.class);

    @Autowired
    protected PhotoRepository photoRepository;

    @Mapping(target = "bookId", expression = "java(getBookId(loan))")
    @Mapping(target = "bookTitle", expression = "java(getBookTitle(loan))")
    @Mapping(target = "userId", expression = "java(getUserId(loan))")
    @Mapping(target = "userName", expression = "java(getUserName(loan))")
    @Mapping(target = "photoId", expression = "java(getPhotoId(loan))")
    @Mapping(target = "photoChecksum", expression = "java(getPhotoChecksum(loan))")
    public abstract LoanDto toDto(Loan loan);

    protected Long getBookId(Loan loan) {
        if (loan == null || loan.getBook() == null) {
            logger.warn("Loan or book is null when mapping to DTO - loan ID: {}", loan != null ? loan.getId() : "null");
            return null;
        }
        return loan.getBook().getId();
    }

    protected String getBookTitle(Loan loan) {
        if (loan == null || loan.getBook() == null) {
            return "Unknown Book";
        }
        return loan.getBook().getTitle();
    }

    protected Long getUserId(Loan loan) {
        if (loan == null || loan.getUser() == null) {
            logger.warn("Loan or user is null when mapping to DTO - loan ID: {}", loan != null ? loan.getId() : "null");
            return null;
        }
        return loan.getUser().getId();
    }

    protected String getUserName(Loan loan) {
        if (loan == null || loan.getUser() == null) {
            return "Unknown User";
        }
        return loan.getUser().getUsername();
    }

    protected Long getPhotoId(Loan loan) {
        if (loan == null || loan.getId() == null) {
            return null;
        }
        return photoRepository.findByLoanId(loan.getId())
                .map(Photo::getId)
                .orElse(null);
    }

    protected String getPhotoChecksum(Loan loan) {
        if (loan == null || loan.getId() == null) {
            return null;
        }
        return photoRepository.findByLoanId(loan.getId())
                .map(Photo::getImageChecksum)
                .orElse(null);
    }
}
