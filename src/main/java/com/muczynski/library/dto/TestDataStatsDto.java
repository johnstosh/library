/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

/**
 * DTO for test data statistics
 */
public class TestDataStatsDto {
    private long books;
    private long authors;
    private long loans;
    private long users;

    public TestDataStatsDto() {
    }

    public TestDataStatsDto(long books, long authors, long loans, long users) {
        this.books = books;
        this.authors = authors;
        this.loans = loans;
        this.users = users;
    }

    public long getBooks() {
        return books;
    }

    public void setBooks(long books) {
        this.books = books;
    }

    public long getAuthors() {
        return authors;
    }

    public void setAuthors(long authors) {
        this.authors = authors;
    }

    public long getLoans() {
        return loans;
    }

    public void setLoans(long loans) {
        this.loans = loans;
    }

    public long getUsers() {
        return users;
    }

    public void setUsers(long users) {
        this.users = users;
    }
}
