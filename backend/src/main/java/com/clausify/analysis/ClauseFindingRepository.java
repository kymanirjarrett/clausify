package com.clausify.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClauseFindingRepository extends JpaRepository<ClauseFinding, Long> {

    List<ClauseFinding> findByAnalysisId(Long analysisId);
}
