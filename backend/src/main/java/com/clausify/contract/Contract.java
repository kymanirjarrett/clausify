package com.clausify.contract;

import com.clausify.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * An uploaded contract: metadata, the extracted text, and where it is in the analysis lifecycle.
 * <p>
 * There are deliberately no setters for {@code status}: every change goes through a transition method,
 * so the lifecycle rules live in one place and an illegal change (say, ANALYZED back to ANALYZING)
 * fails loudly instead of corrupting data.
 */
@Entity
@Table(name = "contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA only
@ToString
public class Contract {

    static final int MAX_FAILURE_REASON = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "extracted_text", nullable = false, columnDefinition = "MEDIUMTEXT")
    @ToString.Exclude
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ContractStatus status;

    @Column(name = "failure_reason", length = MAX_FAILURE_REASON)
    private String failureReason;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    /** A freshly uploaded contract, waiting for analysis. */
    public static Contract uploaded(User owner, String originalFilename, long fileSizeBytes, int pageCount,
                                    String extractedText, Instant uploadedAt) {
        Contract contract = new Contract();
        contract.user = owner;
        contract.originalFilename = originalFilename;
        contract.fileSizeBytes = fileSizeBytes;
        contract.pageCount = pageCount;
        contract.extractedText = extractedText;
        contract.uploadedAt = uploadedAt;
        contract.status = ContractStatus.UPLOADED;
        return contract;
    }

    /** UPLOADED → ANALYZING, when the background analysis picks it up. */
    public void markAnalyzing() {
        transition(EnumSet.of(ContractStatus.UPLOADED), ContractStatus.ANALYZING);
    }

    /** ANALYZING → ANALYZED. */
    public void markAnalyzed(Instant analyzedAt) {
        transition(EnumSet.of(ContractStatus.ANALYZING), ContractStatus.ANALYZED);
        this.analyzedAt = analyzedAt;
    }

    /** UPLOADED or ANALYZING → FAILED, with a short reason shown to the user. */
    public void markFailed(String reason) {
        transition(EnumSet.of(ContractStatus.UPLOADED, ContractStatus.ANALYZING), ContractStatus.FAILED);
        this.failureReason = truncate(reason == null || reason.isBlank() ? "Analysis failed." : reason.strip());
    }

    /** Only failed analyses can be retried (FR-7). */
    public boolean isRetryable() {
        return status == ContractStatus.FAILED;
    }

    /** FAILED → UPLOADED, so the stored text is analyzed again. */
    public void resetForRetry() {
        transition(EnumSet.of(ContractStatus.FAILED), ContractStatus.UPLOADED);
        this.failureReason = null;
    }

    private void transition(Set<ContractStatus> allowedFrom, ContractStatus to) {
        if (!allowedFrom.contains(status)) {
            throw new IllegalStateException("Contract " + id + " cannot move from " + status + " to " + to);
        }
        this.status = to;
    }

    private static String truncate(String reason) {
        return reason.length() <= MAX_FAILURE_REASON ? reason : reason.substring(0, MAX_FAILURE_REASON - 3) + "...";
    }
}
