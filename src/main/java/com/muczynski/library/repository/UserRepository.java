/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /** @deprecated Use findAllByUsernameOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<User> findByUsername(String username);
    /** @deprecated Use findAllByUsernameIgnoreCaseOrderByIdAsc() instead to handle duplicates safely. */
    @Deprecated
    Optional<User> findByUsernameIgnoreCase(String username);
    List<User> findAllByUsernameOrderByIdAsc(String username);
    List<User> findAllByUsernameIgnoreCaseOrderByIdAsc(String username);
    List<User> findAllBySsoProviderAndSsoSubjectIdOrderByIdAsc(String ssoProvider, String ssoSubjectId);

    // Find user by username with a local password (for form login)
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.password IS NOT NULL ORDER BY u.id ASC")
    List<User> findAllByUsernameWithPasswordOrderByIdAsc(@Param("username") String username);
}
