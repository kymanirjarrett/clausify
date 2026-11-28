package com.clausify.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClauseFindingRepository extends JpaRepository<ClauseFinding, Long> {

    List<ClauseFinding> findByAnalysisId(Long analysisId);

    /** FR-9: one finding, only if its contract belongs to the user; anyone else's is indistinguishable from missing. */
    @Query("select f from ClauseFinding f where f.id = :id and f.analysis.contract.user.id = :userId")
    Optional<ClauseFinding> findOwned(@Param("id") Long id, @Param("userId") Long userId);
}
