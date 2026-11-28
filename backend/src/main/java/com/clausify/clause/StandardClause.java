package com.clausify.clause;

import com.clausify.analysis.RiskCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** A reference clause in the FR-9 library: fair or unfair wording for one category, with its embedding. */
@Entity
@Table(name = "standard_clauses")
@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StandardClause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable id from standard-clauses.json, so edits update the row instead of adding a new one. */
    @Column(name = "clause_key", nullable = false, unique = true, length = 100)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RiskCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean favorable;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** JSON array of 384 floats, or null until StandardClauseLoader embeds it. */
    @Column(columnDefinition = "JSON")
    @ToString.Exclude
    private String embedding;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    StandardClause(String key) {
        this.key = key;
    }
}
