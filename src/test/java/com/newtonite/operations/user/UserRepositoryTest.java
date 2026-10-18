package com.newtonite.operations.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save user and find by email with normalization")
    void shouldSaveAndFindByEmail() {
        User user = new User("Alice Walker", "Alice.Walker@example.com");
        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("alice.walker@example.com");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isActive()).isTrue();

        Optional<User> found = userRepository.findByEmail("alice.walker@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Alice Walker");

        assertThat(userRepository.existsByEmail("alice.walker@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should find only active users")
    void shouldFindAllByActiveTrue() {
        User active = userRepository.save(new User("Active User", "active@example.com"));
        User inactive = new User("Inactive User", "inactive@example.com");
        inactive.setActive(false);
        userRepository.save(inactive);
        userRepository.flush();

        List<User> activeUsers = userRepository.findAllByActiveTrue();
        assertThat(activeUsers)
                .extracting(User::getEmail)
                .contains("active@example.com")
                .doesNotContain("inactive@example.com");
    }

    @Test
    @DisplayName("Should enforce email uniqueness at the database level")
    void shouldEnforceEmailUniqueness() {
        userRepository.saveAndFlush(new User("User One", "duplicate@example.com"));

        User duplicate = new User("User Two", "duplicate@example.com");
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
