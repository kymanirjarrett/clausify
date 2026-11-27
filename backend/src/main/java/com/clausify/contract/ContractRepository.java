package com.clausify.contract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /**
     * One page of the user's contracts as summaries. Selecting only the summary columns keeps Hibernate from
     * loading each contract's extracted text, which the list never shows.
     */
    @Query(value = """
            select new com.clausify.contract.ContractSummaryResponse(
                c.id, c.originalFilename, c.fileSizeBytes, c.pageCount, c.status, c.uploadedAt)
            from Contract c
            where c.user.id = :userId
            """,
            countQuery = "select count(c) from Contract c where c.user.id = :userId")
    Page<ContractSummaryResponse> findSummariesByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * For background analysis only, never for a user request (it is not scoped by owner). Locks the row
     * ({@code SELECT ... FOR UPDATE}) until the transaction ends, so two workers cannot both move the same
     * contract from UPLOADED to ANALYZING, and a status change cannot interleave with a delete.
     * <p>
     * Native SQL on purpose: {@code @Lock(PESSIMISTIC_WRITE)} makes Hibernate's MySQL dialect emit
     * {@code FOR UPDATE OF <alias>}, which TiDB rejects (ADR 0011). Plain {@code FOR UPDATE} works on both.
     */
    @Query(value = "SELECT * FROM contracts WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Contract> findLockedById(@Param("id") Long id);

    /** Ids only, so recovery on startup does not load every contract's full text into memory. */
    @Query("select c.id from Contract c where c.status = :status")
    List<Long> findIdsByStatus(@Param("status") ContractStatus status);
}
