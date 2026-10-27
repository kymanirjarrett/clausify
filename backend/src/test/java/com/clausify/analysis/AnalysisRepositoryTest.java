package com.clausify.analysis;

import com.clausify.TestcontainersConfiguration;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real MySQL with Flyway V1 to V6: the analysis mapping matches the migration and its relationships work. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AnalysisRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");

    private final AnalysisRepository analyses;
    private final ClauseFindingRepository findings;
    private final ContractRepository contracts;
    private final UserRepository users;
    private final TestEntityManager entityManager;

    @Autowired
    AnalysisRepositoryTest(AnalysisRepository analyses, ClauseFindingRepository findings, ContractRepository contracts,
                           UserRepository users, TestEntityManager entityManager) {
        this.analyses = analyses;
        this.findings = findings;
        this.contracts = contracts;
        this.users = users;
        this.entityManager = entityManager;
    }

    @Test
    void savesAnAnalysisWithFindingsAndLoadsItByContract() {
        Contract contract = contract("maria@example.com");
        Analysis analysis = analysis(contract);
        analysis.addFinding(finding(RiskCategory.LIABILITY, 85, false));
        analysis.addFinding(finding(RiskCategory.PAYMENT_TERMS, 20, true));
        analyses.save(analysis);
        entityManager.flush();
        entityManager.clear();

        Analysis reloaded = analyses.findByContractId(contract.getId()).orElseThrow();
        assertThat(reloaded.getOverallScore()).isEqualTo(80);
        assertThat(reloaded.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(reloaded.getContract().getId()).isEqualTo(contract.getId());
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getFindings())
                .extracting(ClauseFinding::getCategory, ClauseFinding::getRiskScore, ClauseFinding::isFavorable)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(RiskCategory.LIABILITY, 85, false),
                        org.assertj.core.groups.Tuple.tuple(RiskCategory.PAYMENT_TERMS, 20, true));
        assertThat(findings.findByAnalysisId(reloaded.getId())).hasSize(2);
    }

    @Test
    void aContractHasAtMostOneAnalysis() {
        Contract contract = contract("bob@example.com");
        analyses.save(analysis(contract));
        entityManager.flush();

        assertThatThrownBy(() -> {
            analyses.save(analysis(contract));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Contract contract(String email) {
        User user = users.save(User.builder().name("Test").email(email).password("hash").build());
        return contracts.save(Contract.uploaded(user, "nda.pdf", 1, 1, "text", T0));
    }

    private static Analysis analysis(Contract contract) {
        return new Analysis(null, contract, 80, RiskLevel.HIGH, "NDA", "Summary.", "test-model", 1200L,
                new java.util.ArrayList<>(), null);
    }

    private static ClauseFinding finding(RiskCategory category, int score, boolean favorable) {
        return new ClauseFinding(null, null, "Title", "4.2", category, score, RiskLevel.fromScore(score), favorable,
                "Original clause.", "Why it matters.", "Fairer wording.", null);
    }
}
