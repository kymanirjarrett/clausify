package com.clausify.analysis;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

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
    @Column(name = "risk_category", nullable = false, length = 50)
    private RiskCategory riskCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 50)
    private RiskLevel riskLevel;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean favorable;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "suggested_remediation", columnDefinition = "TEXT")
    private String suggestedRemremediation;

    @Column(columnDefinition = "JSON")
    private String embedding;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
