package com.clausify.contract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Every lookup that serves a user request is scoped by owner (CLAUDE.md "Ownership"): a contract that
 * belongs to someone else is indistinguishable from one that does not exist.
 */
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByIdAndUserId(Long id, Long userId);

    Page<Contract> findAllByUserId(Long userId, Pageable pageable);

    /**
     * For background analysis only, never for a user request (it is not scoped by owner). Locks the row
     * ({@code SELECT ... FOR UPDATE}) until the transaction ends, so two workers cannot both move the same
     * contract from UPLOADED to ANALYZING, and a status change cannot interleave with a delete.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Contract> findLockedById(Long id);

    /** Ids only, so recovery on startup does not load every contract's full text into memory. */
    @Query("select c.id from Contract c where c.status = :status")
    List<Long> findIdsByStatus(@Param("status") ContractStatus status);
}
