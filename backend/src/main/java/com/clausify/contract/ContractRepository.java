package com.clausify.contract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
