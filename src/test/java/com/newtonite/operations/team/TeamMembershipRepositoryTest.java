package com.newtonite.operations.team;

import com.newtonite.operations.user.User;
import com.newtonite.operations.user.UserRepository;
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
class TeamMembershipRepositoryTest {

    @Autowired
    private TeamMembershipRepository membershipRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and retrieve team membership by team and user ID")
    void shouldSaveAndFindByTeamAndUser() {
        Team team = teamRepository.save(new Team("Platform Team", "Core platform"));
        User user = userRepository.save(new User("Dev One", "dev1@example.com"));

        TeamMembership membership = new TeamMembership(team, user, TeamRole.TEAM_LEAD);
        TeamMembership saved = membershipRepository.saveAndFlush(membership);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(TeamRole.TEAM_LEAD);

        Optional<TeamMembership> found = membershipRepository.findByTeamIdAndUserId(team.getId(), user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(TeamRole.TEAM_LEAD);

        assertThat(membershipRepository.existsByTeamIdAndUserId(team.getId(), user.getId())).isTrue();
        assertThat(membershipRepository.existsByTeamIdAndUserId(team.getId(), 9999L)).isFalse();
    }

    @Test
    @DisplayName("Should query memberships by team, user, and role")
    void shouldQueryMembershipsByTeamUserAndRole() {
        Team team = teamRepository.save(new Team("DevOps", "CI/CD and infra"));
        User lead = userRepository.save(new User("Lead Person", "lead@example.com"));
        User member = userRepository.save(new User("Member Person", "member@example.com"));

        membershipRepository.save(new TeamMembership(team, lead, TeamRole.TEAM_LEAD));
        membershipRepository.save(new TeamMembership(team, member, TeamRole.MEMBER));
        membershipRepository.flush();

        List<TeamMembership> byTeam = membershipRepository.findByTeamId(team.getId());
        assertThat(byTeam).hasSize(2);

        List<TeamMembership> leads = membershipRepository.findByTeamIdAndRole(team.getId(), TeamRole.TEAM_LEAD);
        assertThat(leads).hasSize(1);
        assertThat(leads.get(0).getUser().getEmail()).isEqualTo("lead@example.com");

        List<TeamMembership> userMemberships = membershipRepository.findByUserId(lead.getId());
        assertThat(userMemberships).hasSize(1);
    }

    @Test
    @DisplayName("Should enforce unique constraint preventing same user added to same team twice")
    void shouldEnforceUniqueConstraintForUserAndTeam() {
        Team team = teamRepository.save(new Team("Triage Team", "Incident response"));
        User user = userRepository.save(new User("Triage Agent", "agent@example.com"));

        membershipRepository.saveAndFlush(new TeamMembership(team, user, TeamRole.MEMBER));

        TeamMembership duplicate = new TeamMembership(team, user, TeamRole.TEAM_LEAD);
        assertThatThrownBy(() -> membershipRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should delete membership by team and user ID without deleting team or user")
    void shouldDeleteMembershipByTeamAndUserId() {
        Team team = teamRepository.save(new Team("Support Team", "Customer support"));
        User user = userRepository.save(new User("Support Agent", "support@example.com"));

        membershipRepository.saveAndFlush(new TeamMembership(team, user, TeamRole.MEMBER));
        assertThat(membershipRepository.existsByTeamIdAndUserId(team.getId(), user.getId())).isTrue();

        membershipRepository.deleteByTeamIdAndUserId(team.getId(), user.getId());
        membershipRepository.flush();

        assertThat(membershipRepository.existsByTeamIdAndUserId(team.getId(), user.getId())).isFalse();
        // Team and User entities must remain intact
        assertThat(teamRepository.existsById(team.getId())).isTrue();
        assertThat(userRepository.existsById(user.getId())).isTrue();
    }
}
