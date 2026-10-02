package com.clausify.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Emails are stored lowercased (see AuthService), so callers pass a normalized email. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
