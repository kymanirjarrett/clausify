package com.clausify.analysis;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "clause_findings")
public class ClauseFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private RiskCategory category;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 50)
    private RiskLevel riskLevel;

    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "suggested_revision", columnDefinition = "TEXT")
    private String suggestedRevision;

    @Column(name = "is_favorable", nullable = false)
    private boolean isFavorable = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // --- Constructors ---
    public ClauseFinding() {}

    // --- Getters and Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Analysis getAnalysis() { return analysis; }
    public void setAnalysis(Analysis analysis) { this.analysis = analysis; }

    public RiskCategory getCategory() { return category; }
    public void setCategory(RiskCategory category) { this.category = category; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getSuggestedRevision() { return suggestedRevision; }
    public void setSuggestedRevision(String suggestedRevision) { this.suggestedRevision = suggestedRevision; }

    public boolean isFavorable() { return isFavorable; }
    public void setFavorable(boolean favorable) { isFavorable = favorable; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
