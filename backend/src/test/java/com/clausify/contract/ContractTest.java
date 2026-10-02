package com.clausify.contract;

import com.clausify.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ContractTest {

    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");

    @Test
    void newContractIsUploaded() {
        Contract contract = uploaded();

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.UPLOADED);
        assertThat(contract.getUploadedAt()).isEqualTo(NOW);
        assertThat(contract.getAnalyzedAt()).isNull();
        assertThat(contract.isRetryable()).isFalse();
    }

    @Test
    void happyPathUploadedToAnalyzingToAnalyzed() {
        Contract contract = uploaded();

        contract.markAnalyzing();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ANALYZING);

        contract.markAnalyzed(NOW.plusSeconds(40));
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ANALYZED);
        assertThat(contract.getAnalyzedAt()).isEqualTo(NOW.plusSeconds(40));
    }

    @Test
    void failureWhileAnalyzingRecordsReasonAndIsRetryable() {
        Contract contract = inStatus(ContractStatus.ANALYZING);

        contract.markFailed("  The AI service did not respond.  ");

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.FAILED);
        assertThat(contract.getFailureReason()).isEqualTo("The AI service did not respond.");
        assertThat(contract.isRetryable()).isTrue();
    }

    @Test
    void failureBeforeAnalysisStartsIsAllowed() {
        Contract contract = uploaded();

        contract.markFailed(null);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.FAILED);
        assertThat(contract.getFailureReason()).isEqualTo("Analysis failed.");
    }

    @Test
    void longFailureReasonIsTruncatedToFitTheColumn() {
        Contract contract = inStatus(ContractStatus.ANALYZING);

        contract.markFailed("x".repeat(2000));

        assertThat(contract.getFailureReason()).hasSize(Contract.MAX_FAILURE_REASON).endsWith("...");
    }

    @Test
    void retryResetsFailedToUploadedAndClearsReason() {
        Contract contract = inStatus(ContractStatus.FAILED);

        contract.resetForRetry();

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.UPLOADED);
        assertThat(contract.getFailureReason()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = ContractStatus.class, names = "UPLOADED", mode = EnumSource.Mode.EXCLUDE)
    void markAnalyzingOnlyFromUploaded(ContractStatus from) {
        assertIllegal(from, Contract::markAnalyzing);
    }

    @ParameterizedTest
    @EnumSource(value = ContractStatus.class, names = "ANALYZING", mode = EnumSource.Mode.EXCLUDE)
    void markAnalyzedOnlyFromAnalyzing(ContractStatus from) {
        assertIllegal(from, contract -> contract.markAnalyzed(NOW));
    }

    @ParameterizedTest
    @EnumSource(value = ContractStatus.class, names = {"ANALYZED", "FAILED"})
    void markFailedNotFromFinishedStates(ContractStatus from) {
        assertIllegal(from, contract -> contract.markFailed("boom"));
    }

    @ParameterizedTest
    @EnumSource(value = ContractStatus.class, names = "FAILED", mode = EnumSource.Mode.EXCLUDE)
    void retryOnlyFromFailed(ContractStatus from) {
        Contract contract = inStatus(from);

        assertThat(contract.isRetryable()).isFalse();
        assertIllegal(from, Contract::resetForRetry);
    }

    @Test
    void toStringOmitsTextAndOwner() {
        assertThat(uploaded().toString()).doesNotContain("secret clause text").doesNotContain("maria@example.com");
    }

    private static void assertIllegal(ContractStatus from, Consumer<Contract> transition) {
        Contract contract = inStatus(from);
        assertThatIllegalStateException().isThrownBy(() -> transition.accept(contract));
        assertThat(contract.getStatus()).isEqualTo(from);
    }

    private static Contract uploaded() {
        User owner = User.builder().id(1L).name("Maria").email("maria@example.com").password("hash").build();
        return Contract.uploaded(owner, "Consulting_Agreement_2026.pdf", 1_800_000, 6, "secret clause text", NOW);
    }

    /** Walks the real transitions to reach a status, so tests never bypass the rules. */
    private static Contract inStatus(ContractStatus status) {
        Contract contract = uploaded();
        switch (status) {
            case UPLOADED -> { }
            case ANALYZING -> contract.markAnalyzing();
            case ANALYZED -> { contract.markAnalyzing(); contract.markAnalyzed(NOW); }
            case FAILED -> { contract.markAnalyzing(); contract.markFailed("boom"); }
        }
        return contract;
    }
}
