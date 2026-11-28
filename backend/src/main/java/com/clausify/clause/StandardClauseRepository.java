package com.clausify.clause;

import com.clausify.analysis.RiskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandardClauseRepository extends JpaRepository<StandardClause, Long> {

    List<StandardClause> findAllByOrderByCategoryAscFavorableDescTitleAsc();

    List<StandardClause> findAllByCategoryOrderByFavorableDescTitleAsc(RiskCategory category);
}
