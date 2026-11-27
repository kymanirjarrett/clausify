package com.clausify.contract;

import com.clausify.common.ConflictException;
import com.clausify.common.NotFoundException;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** FR-7: the retry rules in {@link ContractService#retry}, without a database or web layer. */
@ExtendWith(MockitoExtension.class)
class ContractServiceRetryTest {

    private static final Long CONTRACT_ID = 7L;
    private static final Long USER_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
    private static final Instant ANALYZED_AT = NOW.plusSeconds(40);
    private static final String STORED_TEXT = "The Contractor shall indemnify the Client without limitation.";

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private ApplicationEventPublisher events;

    private ContractService service;

    private final User owner = User.builder().id(USER_ID).name("Maria Lopez").email("maria@example.com").password("hash").build();

    @BeforeEach
    void createService() {
        service = new ContractService(contractRepository, userRepository, pdfTextExtractor, events,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** FR-7 example 1: the stored text is analyzed again, with no re-upload. */
    @Test
    void failedContractIsResetAndQueuedAgainFromItsStoredText() {
        Contract contract = contractIn(ContractStatus.FAILED);
        when(contractRepository.findByIdAndUserId(CONTRACT_ID, USER_ID)).thenReturn(Optional.of(contract));

        service.retry(CONTRACT_ID, USER_ID);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.UPLOADED);
        assertThat(contract.getFailureReason()).isNull();
        assertThat(contract.getExtractedText()).isEqualTo(STORED_TEXT);
        verify(events).publishEvent(new ContractUploadedEvent(CONTRACT_ID));
        verifyNoInteractions(pdfTextExtractor);
    }

    /** FR-7 example 2: 409, and the existing analysis is left exactly as it was. */
    @Test
    void analyzedContractIsAConflictAndStaysAnalyzed() {
        Contract contract = contractIn(ContractStatus.ANALYZED);
        when(contractRepository.findByIdAndUserId(CONTRACT_ID, USER_ID)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> service.retry(CONTRACT_ID, USER_ID)).isInstanceOf(ConflictException.class);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ANALYZED);
        assertThat(contract.getAnalyzedAt()).isEqualTo(ANALYZED_AT);
        assertThat(contract.getFailureReason()).isNull();
        verifyNoInteractions(events);
    }

    /** A double click on Retry must not queue a second run next to one that is already waiting or running. */
    @ParameterizedTest
    @EnumSource(value = ContractStatus.class, names = {"UPLOADED", "ANALYZING"})
    void contractThatIsQueuedOrRunningIsAConflict(ContractStatus status) {
        Contract contract = contractIn(status);
        when(contractRepository.findByIdAndUserId(CONTRACT_ID, USER_ID)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> service.retry(CONTRACT_ID, USER_ID)).isInstanceOf(ConflictException.class);

        assertThat(contract.getStatus()).isEqualTo(status);
        verifyNoInteractions(events);
    }

    /** CLAUDE.md "Ownership": a missing contract and someone else's look the same, and nothing is queued. */
    @Test
    void missingOrForeignContractIsNotFoundAndNothingIsQueued() {
        when(contractRepository.findByIdAndUserId(CONTRACT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retry(CONTRACT_ID, USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ContractService.NOT_FOUND);

        verifyNoInteractions(events);
    }

    private Contract contractIn(ContractStatus status) {
        Contract contract = Contract.uploaded(owner, "Retainer_Agreement_Draft.pdf", 2048, 4, STORED_TEXT, NOW);
        ReflectionTestUtils.setField(contract, "id", CONTRACT_ID);
        switch (status) {
            case UPLOADED -> { }
            case ANALYZING -> contract.markAnalyzing();
            case ANALYZED -> {
                contract.markAnalyzing();
                contract.markAnalyzed(ANALYZED_AT);
            }
            case FAILED -> {
                contract.markAnalyzing();
                contract.markFailed("The AI service did not respond.");
            }
        }
        return contract;
    }
}
