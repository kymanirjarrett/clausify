package com.clausify.contract;

import com.clausify.TestcontainersConfiguration;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Real MySQL with Flyway V1 to V5: proves the mapping matches the migration and the ownership queries. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ContractRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00.123456Z");

    private final ContractRepository contracts;
    private final UserRepository users;
    private final TestEntityManager entityManager;

    @Autowired
    ContractRepositoryTest(ContractRepository contracts, UserRepository users, TestEntityManager entityManager) {
        this.contracts = contracts;
        this.users = users;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndReloadsEveryField() {
        User maria = user("maria@example.com");
        Contract saved = contracts.save(Contract.uploaded(maria, "NDA.pdf", 2048, 3, "Full text.", T0));
        saved.markAnalyzing();
        saved.markFailed("Groq timed out.");
        entityManager.flush();
        entityManager.clear();

        Contract reloaded = contracts.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getOriginalFilename()).isEqualTo("NDA.pdf");
        assertThat(reloaded.getFileSizeBytes()).isEqualTo(2048);
        assertThat(reloaded.getPageCount()).isEqualTo(3);
        assertThat(reloaded.getExtractedText()).isEqualTo("Full text.");
        assertThat(reloaded.getStatus()).isEqualTo(ContractStatus.FAILED);
        assertThat(reloaded.getFailureReason()).isEqualTo("Groq timed out.");
        assertThat(reloaded.getUploadedAt()).isEqualTo(T0.truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void findByIdAndUserIdHidesOtherUsersContracts() {
        User maria = user("maria@example.com");
        User bob = user("bob@example.com");
        Long contractId = contracts.save(Contract.uploaded(maria, "NDA.pdf", 2048, 3, "text", T0)).getId();

        assertThat(contracts.findByIdAndUserId(contractId, maria.getId())).isPresent();
        assertThat(contracts.findByIdAndUserId(contractId, bob.getId())).isEmpty();
    }

    @Test
    void findAllByUserIdPagesNewestFirstAndOnlyOwnContracts() {
        User maria = user("maria@example.com");
        User bob = user("bob@example.com");
        for (int i = 0; i < 3; i++) {
            contracts.save(Contract.uploaded(maria, "maria-" + i + ".pdf", 1, 1, "text", T0.plusSeconds(i)));
        }
        contracts.save(Contract.uploaded(bob, "bob.pdf", 1, 1, "text", T0));

        Page<Contract> page = contracts.findAllByUserId(maria.getId(),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "uploadedAt")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Contract::getOriginalFilename).containsExactly("maria-2.pdf", "maria-1.pdf");
    }

    @Test
    void deletingUserDeletesTheirContracts() {
        User maria = user("maria@example.com");
        Long contractId = contracts.save(Contract.uploaded(maria, "NDA.pdf", 1, 1, "text", T0)).getId();
        entityManager.flush();
        entityManager.clear();

        // ON DELETE CASCADE in V5 removes the contract when the user row goes.
        entityManager.getEntityManager().createNativeQuery("DELETE FROM users WHERE id = :id")
                .setParameter("id", maria.getId()).executeUpdate();

        assertThat(contracts.findById(contractId)).isEmpty();
    }

    private User user(String email) {
        return users.save(User.builder().name("Test").email(email).password("hash").build());
    }
}
