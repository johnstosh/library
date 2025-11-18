package com.muczynski.library.repository;

import com.muczynski.library.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    List<User> findAllByUsernameOrderByIdAsc(String username);
    List<User> findAllByUsernameIgnoreCaseOrderByIdAsc(String username);
    List<User> findAllBySsoProviderAndSsoSubjectIdOrderByIdAsc(String ssoProvider, String ssoSubjectId);
}
