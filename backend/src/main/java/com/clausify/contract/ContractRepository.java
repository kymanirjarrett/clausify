package com.clausify.contract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Every lookup that serves a user request is scoped by owner (CLAUDE.md "Ownership"): a contract that
 * belongs to someone else is indistinguishable from one that does not exist.
 */
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByIdAndUserId(Long id, Long userId);

    Page<Contract> findAllByUserId(Long userId, Pageable pageable);
}
