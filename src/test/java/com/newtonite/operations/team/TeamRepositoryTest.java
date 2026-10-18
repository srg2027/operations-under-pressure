package com.newtonite.operations.team;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    @Test
    @DisplayName("Should save and find team by unique name")
    void shouldSaveAndFindByName() {
        Team team = new Team("Operations Alpha", "Primary triage team");
        Team saved = teamRepository.saveAndFlush(team);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Team> found = teamRepository.findByName("Operations Alpha");
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Primary triage team");

        assertThat(teamRepository.existsByName("Operations Alpha")).isTrue();
        assertThat(teamRepository.existsByName("Nonexistent Team")).isFalse();
    }

    @Test
    @DisplayName("Should enforce team name uniqueness at database level")
    void shouldEnforceTeamNameUniqueness() {
        teamRepository.saveAndFlush(new Team("Platform Team", "Core platform"));

        Team duplicate = new Team("Platform Team", "Duplicate description");
        assertThatThrownBy(() -> teamRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
