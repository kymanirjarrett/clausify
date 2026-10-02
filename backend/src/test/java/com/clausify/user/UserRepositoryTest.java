package com.clausify.user;

import com.clausify.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the lab entities against real MySQL with the Flyway schema, so a mapping that
 * disagrees with V1 to V3 fails here instead of at startup.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    private final UserRepository userRepository;
    private final TestEntityManager entityManager;

    @Autowired
    UserRepositoryTest(UserRepository userRepository, TestEntityManager entityManager) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Test
    void savesUserWithAddressesAndProfileThroughCascade() {
        User user = User.builder().name("Maria Lopez").email("maria@example.com").password("hash").build();
        user.addAddress(Address.builder().street("1 Main St").city("Cincinnati").state("OH").zip("45221").build());
        user.setProfile(Profile.builder().user(user).bio("Freelance designer").build());

        Long id = userRepository.save(user).getId();
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(id).orElseThrow();
        assertThat(reloaded.getAddresses()).singleElement()
                .satisfies(address -> assertThat(address.getCity()).isEqualTo("Cincinnati"));
        assertThat(reloaded.getProfile().getId()).isEqualTo(id);
        assertThat(reloaded.getProfile().getLoyalityPoints()).isZero();
    }

    @Test
    void databaseSetsCreatedAtOnInsert() {
        User saved = userRepository.saveAndFlush(
                User.builder().name("Maria Lopez").email("maria@example.com").password("hash").build());

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        userRepository.saveAndFlush(User.builder().name("Maria").email("maria@example.com").password("hash").build());

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                User.builder().name("Other").email("MARIA@example.com").password("hash").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
