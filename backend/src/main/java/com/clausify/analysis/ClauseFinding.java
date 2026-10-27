package com.clausify.analysis;

import jakarta.persistence.*;
import lombok.*;

/** One flagged clause in an analysis, with the model's score, explanation, and suggested revision. */
@Entity
@Table(name = "clause_findings")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ClauseFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Analysis analysis;

    @Column(nullable = false)
    private String title;

    @Column(name = "section_ref", length = 100)
    private String sectionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RiskCategory category;

    /** The model's 0 to 100 score for this clause (DESIGN.md 7.7). */
    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean favorable;

    /** The clause exactly as it appears in the contract; FR-9 embeds this text. */
    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "suggested_revision", columnDefinition = "TEXT")
    private String suggestedRevision;

    /** all-MiniLM-L6-v2 vector as a JSON array, filled in for FR-9 (ADR 0003). */
    @Column(columnDefinition = "JSON")
    private String embedding;
}
