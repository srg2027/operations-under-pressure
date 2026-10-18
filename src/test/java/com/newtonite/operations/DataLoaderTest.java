package com.newtonite.operations;

import com.newtonite.operations.activity.ActivityLog;
import com.newtonite.operations.activity.ActivityLogRepository;
import com.newtonite.operations.activity.ActivityType;
import com.newtonite.operations.team.Team;
import com.newtonite.operations.team.TeamMembership;
import com.newtonite.operations.team.TeamMembershipRepository;
import com.newtonite.operations.team.TeamRepository;
import com.newtonite.operations.team.TeamRole;
import com.newtonite.operations.user.User;
import com.newtonite.operations.user.UserRepository;
import com.newtonite.operations.workitem.WorkItem;
import com.newtonite.operations.workitem.WorkItemPriority;
import com.newtonite.operations.workitem.WorkItemRepository;
import com.newtonite.operations.workitem.WorkItemStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DataLoaderTest {

    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Test
    @DisplayName("First-time seeding should create expected users, team, memberships, and work items")
    void shouldSeedDemoDataOnFirstExecution() {
        dataLoader.seedData();

        // 1. Verify Users
        Optional<User> aliceOpt = userRepository.findByEmail(DataLoader.ALICE_EMAIL);
        assertThat(aliceOpt).isPresent();
        User alice = aliceOpt.get();
        assertThat(alice.getDisplayName()).isEqualTo("Alice Chen");
        assertThat(alice.isActive()).isTrue();

        Optional<User> bobOpt = userRepository.findByEmail(DataLoader.BOB_EMAIL);
        assertThat(bobOpt).isPresent();
        User bob = bobOpt.get();
        assertThat(bob.getDisplayName()).isEqualTo("Bob Martinez");
        assertThat(bob.isActive()).isTrue();

        Optional<User> carolOpt = userRepository.findByEmail(DataLoader.CAROL_EMAIL);
        assertThat(carolOpt).isPresent();
        User carol = carolOpt.get();
        assertThat(carol.getDisplayName()).isEqualTo("Carol Danvers");
        assertThat(carol.isActive()).isTrue();

        // 2. Verify Team
        Optional<Team> teamOpt = teamRepository.findByName(DataLoader.DEMO_TEAM_NAME);
        assertThat(teamOpt).isPresent();
        Team team = teamOpt.get();
        assertThat(team.getDescription()).contains("Platform observability");

        // 3. Verify Team Memberships
        Optional<TeamMembership> aliceMem = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), alice.getId());
        assertThat(aliceMem).isPresent();
        assertThat(aliceMem.get().getRole()).isEqualTo(TeamRole.TEAM_LEAD);

        Optional<TeamMembership> bobMem = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), bob.getId());
        assertThat(bobMem).isPresent();
        assertThat(bobMem.get().getRole()).isEqualTo(TeamRole.MEMBER);

        Optional<TeamMembership> carolMem = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), carol.getId());
        assertThat(carolMem).isPresent();
        assertThat(carolMem.get().getRole()).isEqualTo(TeamRole.MEMBER);

        // 4. Verify Work Items
        List<WorkItem> items = workItemRepository.findByTeamId(team.getId());
        assertThat(items).hasSize(4);

        // Item 1: In Progress, High, Assigned to Bob
        WorkItem item1 = items.stream()
                .filter(i -> i.getTitle().equals(DataLoader.ITEM_1_TITLE))
                .findFirst().orElseThrow();
        assertThat(item1.getStatus()).isEqualTo(WorkItemStatus.IN_PROGRESS);
        assertThat(item1.getPriority()).isEqualTo(WorkItemPriority.HIGH);
        assertThat(item1.getAssignee().getId()).isEqualTo(bob.getId());
        assertThat(item1.getCreatedBy().getId()).isEqualTo(alice.getId());

        List<ActivityLog> item1History = activityLogRepository.findByWorkItemIdOrderByCreatedAtAsc(item1.getId());
        assertThat(item1History).hasSize(3);
        assertThat(item1History.get(0).getActivityType()).isEqualTo(ActivityType.CREATED);
        assertThat(item1History.get(1).getActivityType()).isEqualTo(ActivityType.CLAIMED);
        assertThat(item1History.get(2).getActivityType()).isEqualTo(ActivityType.NOTE_ADDED);
        assertThat(item1History.get(2).getDetails()).contains("Tracing indicates connection pool");

        // Item 2: Open, Urgent, Unassigned
        WorkItem item2 = items.stream()
                .filter(i -> i.getTitle().equals(DataLoader.ITEM_2_TITLE))
                .findFirst().orElseThrow();
        assertThat(item2.getStatus()).isEqualTo(WorkItemStatus.OPEN);
        assertThat(item2.getPriority()).isEqualTo(WorkItemPriority.URGENT);
        assertThat(item2.getAssignee()).isNull();

        // Item 3: Done, Medium, Resolved by Alice
        WorkItem item3 = items.stream()
                .filter(i -> i.getTitle().equals(DataLoader.ITEM_3_TITLE))
                .findFirst().orElseThrow();
        assertThat(item3.getStatus()).isEqualTo(WorkItemStatus.DONE);
        assertThat(item3.getPriority()).isEqualTo(WorkItemPriority.MEDIUM);
        assertThat(item3.getAssignee().getId()).isEqualTo(alice.getId());

        // Item 4: Open, Low, Unassigned
        WorkItem item4 = items.stream()
                .filter(i -> i.getTitle().equals(DataLoader.ITEM_4_TITLE))
                .findFirst().orElseThrow();
        assertThat(item4.getStatus()).isEqualTo(WorkItemStatus.OPEN);
        assertThat(item4.getPriority()).isEqualTo(WorkItemPriority.LOW);
        assertThat(item4.getAssignee()).isNull();
    }

    @Test
    @DisplayName("Repeated executions of seedData should be idempotent and not create duplicate records")
    void shouldBeCompletelyIdempotentOnRepeatedExecution() {
        // First execution
        dataLoader.seedData();

        long userCountAfterFirst = userRepository.count();
        long teamCountAfterFirst = teamRepository.count();
        long membershipCountAfterFirst = teamMembershipRepository.count();
        long workItemCountAfterFirst = workItemRepository.count();
        long activityLogCountAfterFirst = activityLogRepository.count();

        // Second execution (simulates application restart)
        dataLoader.seedData();

        assertThat(userRepository.count()).isEqualTo(userCountAfterFirst);
        assertThat(teamRepository.count()).isEqualTo(teamCountAfterFirst);
        assertThat(teamMembershipRepository.count()).isEqualTo(membershipCountAfterFirst);
        assertThat(workItemRepository.count()).isEqualTo(workItemCountAfterFirst);
        assertThat(activityLogRepository.count()).isEqualTo(activityLogCountAfterFirst);

        // Third execution
        dataLoader.seedData();

        assertThat(userRepository.count()).isEqualTo(userCountAfterFirst);
        assertThat(teamRepository.count()).isEqualTo(teamCountAfterFirst);
        assertThat(teamMembershipRepository.count()).isEqualTo(membershipCountAfterFirst);
        assertThat(workItemRepository.count()).isEqualTo(workItemCountAfterFirst);
        assertThat(activityLogRepository.count()).isEqualTo(activityLogCountAfterFirst);
    }

    @Test
    @DisplayName("Seeding should be compatible with existing data and never delete or overwrite existing records")
    void shouldBeCompatibleWithExistingDatabaseAndNotOverwritePreexistingData() {
        // Pre-existing independent user and team
        Team existingTeam = teamRepository.save(new Team("Security Operations", "SecOps triage"));
        User existingUser = userRepository.save(new User("Zack Defender", "zack@security.org"));
        teamMembershipRepository.save(new TeamMembership(existingTeam, existingUser, TeamRole.TEAM_LEAD));

        // Pre-populate Alice Chen with a custom display name before loader runs
        User customAlice = userRepository.save(new User("Alice Chen (Customized)", DataLoader.ALICE_EMAIL));

        // Run data loader
        dataLoader.seedData();

        // 1. Existing independent team and user must remain intact
        assertThat(teamRepository.findById(existingTeam.getId())).isPresent();
        assertThat(userRepository.findById(existingUser.getId())).isPresent();
        assertThat(teamMembershipRepository.existsByTeamIdAndUserId(existingTeam.getId(), existingUser.getId())).isTrue();

        // 2. Pre-existing user with matching email should NOT have had their record duplicated or overwritten
        Optional<User> loadedAlice = userRepository.findByEmail(DataLoader.ALICE_EMAIL);
        assertThat(loadedAlice).isPresent();
        assertThat(loadedAlice.get().getId()).isEqualTo(customAlice.getId());
        assertThat(loadedAlice.get().getDisplayName()).isEqualTo("Alice Chen (Customized)");

        // 3. Demo team should still be created and linked correctly
        Optional<Team> demoTeam = teamRepository.findByName(DataLoader.DEMO_TEAM_NAME);
        assertThat(demoTeam).isPresent();
        assertThat(teamMembershipRepository.existsByTeamIdAndUserId(demoTeam.get().getId(), customAlice.getId())).isTrue();
    }
}
