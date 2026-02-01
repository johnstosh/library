// (c) Copyright 2025 by Muczynski
package com.muczynski.library;

import com.muczynski.library.domain.Authority;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Library;
import com.muczynski.library.repository.AuthorityRepository;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BranchRepository;

import java.util.List;

/**
 * Helper methods for creating test entities that respect unique constraints.
 * Use these instead of directly saving new entities to avoid constraint violations.
 */
public class TestEntityHelper {

    /**
     * Find or create an Authority by name. Avoids uk_authority_name violations.
     */
    public static Authority findOrCreateAuthority(AuthorityRepository repo, String name) {
        List<Authority> existing = repo.findAllByNameOrderByIdAsc(name);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        Authority authority = new Authority();
        authority.setName(name);
        return repo.save(authority);
    }

    /**
     * Find or create an Author by name. Avoids uk_author_name violations.
     */
    public static Author findOrCreateAuthor(AuthorRepository repo, String name) {
        List<Author> existing = repo.findAllByNameOrderByIdAsc(name);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        Author author = new Author();
        author.setName(name);
        return repo.save(author);
    }

    /**
     * Find or create a Library by branch name. Avoids uk_library_branch_name violations.
     */
    public static Library findOrCreateLibrary(BranchRepository repo, String branchName, String systemName) {
        List<Library> existing = repo.findAllByBranchNameOrderByIdAsc(branchName);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        Library library = new Library();
        library.setBranchName(branchName);
        library.setLibrarySystemName(systemName);
        return repo.save(library);
    }
}
