package com.sme.afs.repository;

import com.sme.afs.model.User;
import com.sme.afs.util.TestDataUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveUser() {
        // Given
        User user = TestDataUtil.createTestUser("testuser");

        // When
        User savedUser = userRepository.save(user);
        User retrievedUser = userRepository.findById(savedUser.getId()).orElse(null);

        // Then
        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getUsername()).isEqualTo("testuser");
        assertThat(retrievedUser.getEmail()).isEqualTo("testuser@test.com");
    }

    @Test
    void shouldFindUserByUsername() {
        // Given
        User user = TestDataUtil.createTestUser("findme");
        userRepository.save(user);

        // When
        User found = userRepository.findByUsername("findme").orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("findme");
    }

    @Test
    void shouldNotAllowDuplicateUsername() {
        // Given
        User user1 = TestDataUtil.createTestUser("duplicate");
        userRepository.save(user1);

        // When/Then
        User user2 = TestDataUtil.createTestUser("duplicate");
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(user2);
            userRepository.flush();
        });
    }
}
