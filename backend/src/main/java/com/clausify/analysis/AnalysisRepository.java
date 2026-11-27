package com.clausify.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {


    Optional<Analysis> findByContractId(Long contractId);

    /** FR-12: mean overall score of the user's analyses, or null when there are none. */
    @Query("select avg(a.overallScore) from Analysis a where a.contract.user.id = :userId")
    Double averageOverallScoreByUserId(@Param("userId") Long userId);
}
