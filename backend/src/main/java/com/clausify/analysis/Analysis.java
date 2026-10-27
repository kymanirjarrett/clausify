package com.clausify.analysis;

import com.clausify.contract.Contract;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** The AI review of one contract (FR-5): its overall score and the clause findings behind it. */
@Entity
@Table(name = "analyses")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Contract contract;

    /** 0 to 100, computed in Java from the findings (DESIGN.md 7.6), not taken from the model. */
    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "contract_type", length = 100)
    private String contractType;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ClauseFinding> findings = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public void addFinding(ClauseFinding finding) {
        findings.add(finding);
        finding.setAnalysis(this);
    }
}
