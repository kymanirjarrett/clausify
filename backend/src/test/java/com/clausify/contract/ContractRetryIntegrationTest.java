package com.clausify.contract;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.JwtService;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-7 through the full stack: {@code POST /api/contracts/{id}/retry} (examples 1 and 2, plus ownership and auth).
 * <p>
 * Nothing consumes {@link ContractUploadedEvent} yet (FR-5), so a retried contract stays UPLOADED and these tests
 * can assert that. Once the analysis listener exists it will move the contract on in the background, which would
 * make the UPLOADED assertion in the retry-from-FAILED test and the 409 on the second call in the retry-twice test
 * race with it: replace the ContractAnalyzer with a stub in this class at that point.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(TestcontainersConfiguration.class)
class ContractRetryIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");
    private static final Instant ANALYZED_AT = T0.plusSeconds(40);
    private static final String STORED_TEXT = "The Contractor shall indemnify the Client without limitation.";
    private static final String FAILURE_REASON = "The AI service did not respond.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationEvents events;

    private User maria;
    private User bob;

    @BeforeEach
    void createUsers() {
        // Fresh users per test, so other tests' contracts never interfere.
        maria = newUser("maria");
        bob = newUser("bob");
    }

    /** FR-7 example 1: 202, and the stored text is queued for analysis again without a re-upload. */
    @Test
    void failedContractIsAcceptedAndQueuedAgainFromItsStoredText() throws Exception {
        Long id = save(maria, ContractStatus.FAILED);

        retry(maria, id).andExpect(status().isAccepted());

        Contract after = reload(maria, id);
        assertThat(after.getStatus()).isEqualTo(ContractStatus.UPLOADED); // queued; the FR-5 listener moves it to ANALYZING
        assertThat(after.getFailureReason()).isNull();
        assertThat(after.getExtractedText()).isEqualTo(STORED_TEXT);
        assertThat(events.stream(ContractUploadedEvent.class)).containsExactly(new ContractUploadedEvent(id));
    }

    /** FR-7 example 2: 409 Conflict, and the existing analysis is unchanged. */
    @Test
    void analyzedContractIs409AndKeepsItsAnalysis() throws Exception {
        Long id = save(maria, ContractStatus.ANALYZED);

        retry(maria, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").isNotEmpty());

        Contract after = reload(maria, id);
        assertThat(after.getStatus()).isEqualTo(ContractStatus.ANALYZED);
        assertThat(after.getAnalyzedAt()).isEqualTo(ANALYZED_AT);
        assertThat(after.getFailureReason()).isNull();
        // When analyses and clause_findings exist (FR-5), also assert the stored score and findings are untouched.
        assertThat(events.stream(ContractUploadedEvent.class)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ContractStatus.class, names = {"UPLOADED", "ANALYZING"})
    void contractThatIsQueuedOrRunningIs409AndIsNotQueuedAgain(ContractStatus current) throws Exception {
        Long id = save(maria, current);

        retry(maria, id).andExpect(status().isConflict());

        assertThat(reload(maria, id).getStatus()).isEqualTo(current);
        assertThat(events.stream(ContractUploadedEvent.class)).isEmpty();
    }

    @Test
    void retryingTwiceQueuesAnalysisOnlyOnce() throws Exception {
        Long id = save(maria, ContractStatus.FAILED);

        retry(maria, id).andExpect(status().isAccepted());
        retry(maria, id).andExpect(status().isConflict());

        assertThat(events.stream(ContractUploadedEvent.class)).containsExactly(new ContractUploadedEvent(id));
    }

    /** CLAUDE.md "Ownership": another user's contract gets byte-for-byte the same 404 as a missing one, and is untouched. */
    @Test
    void otherUsersFailedContractIsIndistinguishableFromMissingAndStaysFailed() throws Exception {
        Long mariasContract = save(maria, ContractStatus.FAILED);
        long missing = mariasContract + 1_000_000;

        String notYours = retry(bob, mariasContract)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(ContractService.NOT_FOUND))
                .andReturn().getResponse().getContentAsString();

        retry(bob, missing)
                .andExpect(status().isNotFound())
                .andExpect(content().json(notYours.replace("/" + mariasContract, "/" + missing)));

        Contract after = reload(maria, mariasContract);
        assertThat(after.getStatus()).isEqualTo(ContractStatus.FAILED);
        assertThat(after.getFailureReason()).isEqualTo(FAILURE_REASON);
        assertThat(events.stream(ContractUploadedEvent.class)).isEmpty();
    }

    @Test
    void retryRequiresAToken() throws Exception {
        Long id = save(maria, ContractStatus.FAILED);

        mockMvc.perform(post("/api/contracts/{id}/retry", id)).andExpect(status().isUnauthorized());

        assertThat(reload(maria, id).getStatus()).isEqualTo(ContractStatus.FAILED);
        assertThat(events.stream(ContractUploadedEvent.class)).isEmpty();
    }

    private ResultActions retry(User user, long id) throws Exception {
        return mockMvc.perform(post("/api/contracts/{id}/retry", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueToken(user)));
    }

    /** Saves a contract of the given user in the given status, reached through the real lifecycle transitions. */
    private Long save(User owner, ContractStatus status) {
        Contract contract = Contract.uploaded(owner, "Retainer_Agreement_Draft.pdf", 2048, 4, STORED_TEXT, T0);
        switch (status) {
            case UPLOADED -> { }
            case ANALYZING -> contract.markAnalyzing();
            case ANALYZED -> {
                contract.markAnalyzing();
                contract.markAnalyzed(ANALYZED_AT);
            }
            case FAILED -> {
                contract.markAnalyzing();
                contract.markFailed(FAILURE_REASON);
            }
        }
        return contracts.save(contract).getId();
    }

    private Contract reload(User owner, Long id) {
        return contracts.findByIdAndUserId(id, owner.getId()).orElseThrow();
    }

    private User newUser(String name) {
        return users.save(User.builder().name(name).email(name + "." + UUID.randomUUID() + "@example.com").password("hash").build());
    }
}
