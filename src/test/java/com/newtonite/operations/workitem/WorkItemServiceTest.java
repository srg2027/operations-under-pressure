package com.newtonite.operations.workitem;

import com.newtonite.operations.activity.ActivityLog;
import com.newtonite.operations.activity.ActivityType;
import com.newtonite.operations.common.exception.InvalidWorkflowException;
import com.newtonite.operations.common.exception.UnauthorizedOperationException;
import com.newtonite.operations.team.Team;
import com.newtonite.operations.team.TeamMembership;
import com.newtonite.operations.team.TeamMembershipRepository;
import com.newtonite.operations.team.TeamRepository;
import com.newtonite.operations.team.TeamRole;
import com.newtonite.operations.user.User;
import com.newtonite.operations.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class WorkItemServiceTest {

    @Autowired
    private WorkItemService workItemService;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    private Team engineeringTeam;
    private Team opsTeam;
    private User engineer;
    private User teamLead;
    private User externalUser;

    @BeforeEach
    void setUp() {
        engineeringTeam = teamRepository.save(new Team("Engineering", "Software delivery"));
        opsTeam = teamRepository.save(new Team("Operations", "Platform operations"));

        engineer = userRepository.save(new User("Alice Dev", "alice@newtonite.com"));
        teamLead = userRepository.save(new User("Bob Lead", "bob.lead@newtonite.com"));
        externalUser = userRepository.save(new User("Charlie Outsider", "charlie@external.com"));

        teamMembershipRepository.save(new TeamMembership(engineeringTeam, engineer, TeamRole.MEMBER));
        teamMembershipRepository.save(new TeamMembership(engineeringTeam, teamLead, TeamRole.TEAM_LEAD));
    }

    @Test
    @DisplayName("Should create work item and record CREATED activity log")
    void shouldCreateWorkItemSuccessfully() {
        WorkItem created = workItemService.createWorkItem(
                "P0 Incident",
                "Database pool exhaustion",
                WorkItemPriority.URGENT,
                engineeringTeam.getId(),
                engineer.getId()
        );

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(WorkItemStatus.OPEN);
        assertThat(created.getAssignee()).isNull();
        assertThat(created.getTeam().getId()).isEqualTo(engineeringTeam.getId());
        assertThat(created.getCreatedBy().getId()).isEqualTo(engineer.getId());

        List<ActivityLog> history = workItemService.getActivityHistory(created.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getActivityType()).isEqualTo(ActivityType.CREATED);
        assertThat(history.get(0).getActor().getId()).isEqualTo(engineer.getId());
    }

    @Test
    @DisplayName("Should reject work item creation if user is not a member of the team")
    void shouldRejectCreationByNonTeamMember() {
        assertThatThrownBy(() -> workItemService.createWorkItem(
                "Unauthorized Task",
                null,
                WorkItemPriority.LOW,
                engineeringTeam.getId(),
                externalUser.getId()
        )).isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("not a member of team");
    }

    @Test
    @DisplayName("Should claim an open work item and record CLAIMED activity log")
    void shouldClaimWorkItemSuccessfully() {
        WorkItem item = workItemService.createWorkItem(
                "Investigate Memory Spike",
                "High heap usage in service A",
                WorkItemPriority.HIGH,
                engineeringTeam.getId(),
                engineer.getId()
        );

        WorkItem claimed = workItemService.claimWorkItem(item.getId(), engineer.getId());

        assertThat(claimed.getStatus()).isEqualTo(WorkItemStatus.IN_PROGRESS);
        assertThat(claimed.getAssignee()).isNotNull();
        assertThat(claimed.getAssignee().getId()).isEqualTo(engineer.getId());
        assertThat(claimed.getClaimedAt()).isNotNull();

        List<ActivityLog> history = workItemService.getActivityHistory(item.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getActivityType()).isEqualTo(ActivityType.CLAIMED);
        assertThat(history.get(1).getActor().getId()).isEqualTo(engineer.getId());
    }

    @Test
    @DisplayName("Should prevent claiming an already claimed work item")
    void shouldPreventClaimingAlreadyClaimedItem() {
        WorkItem item = workItemService.createWorkItem(
                "Kafka Consumer Lag",
                null,
                WorkItemPriority.HIGH,
                engineeringTeam.getId(),
                engineer.getId()
        );
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        assertThatThrownBy(() -> workItemService.claimWorkItem(item.getId(), teamLead.getId()))
                .isInstanceOf(InvalidWorkflowException.class)
                .hasMessageContaining("Only OPEN work items can be claimed");
    }

    @Test
    @DisplayName("Should prevent non-team member from claiming work item")
    void shouldPreventNonTeamMemberFromClaiming() {
        WorkItem item = workItemService.createWorkItem(
                "Audit Log Ingestion",
                null,
                WorkItemPriority.MEDIUM,
                engineeringTeam.getId(),
                engineer.getId()
        );

        assertThatThrownBy(() -> workItemService.claimWorkItem(item.getId(), externalUser.getId()))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("not a member of team");
    }

    @Test
    @DisplayName("Should allow assignee to release work item and record RELEASED activity log")
    void shouldAllowAssigneeToReleaseWorkItem() {
        WorkItem item = workItemService.createWorkItem("Release test", null, WorkItemPriority.LOW, engineeringTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        WorkItem released = workItemService.releaseWorkItem(item.getId(), engineer.getId());

        assertThat(released.getStatus()).isEqualTo(WorkItemStatus.OPEN);
        assertThat(released.getAssignee()).isNull();
        assertThat(released.getClaimedAt()).isNull();

        List<ActivityLog> history = workItemService.getActivityHistory(item.getId());
        assertThat(history).hasSize(3);
        assertThat(history.get(2).getActivityType()).isEqualTo(ActivityType.RELEASED);
    }

    @Test
    @DisplayName("Should allow team lead to release or reassign work item claimed by another member")
    void shouldAllowTeamLeadToOverrideAndRelease() {
        WorkItem item = workItemService.createWorkItem("Lead override", null, WorkItemPriority.HIGH, engineeringTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        WorkItem releasedByLead = workItemService.releaseWorkItem(item.getId(), teamLead.getId());
        assertThat(releasedByLead.getStatus()).isEqualTo(WorkItemStatus.OPEN);
        assertThat(releasedByLead.getAssignee()).isNull();
    }

    @Test
    @DisplayName("Should complete an in-progress work item and record COMPLETED activity log")
    void shouldCompleteWorkItem() {
        WorkItem item = workItemService.createWorkItem("Task to complete", null, WorkItemPriority.MEDIUM, engineeringTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        WorkItem completed = workItemService.completeWorkItem(item.getId(), engineer.getId());

        assertThat(completed.getStatus()).isEqualTo(WorkItemStatus.DONE);

        List<ActivityLog> history = workItemService.getActivityHistory(item.getId());
        assertThat(history).hasSize(3);
        assertThat(history.get(2).getActivityType()).isEqualTo(ActivityType.COMPLETED);
    }

    @Test
    @DisplayName("Should reject completing an OPEN work item")
    void shouldRejectCompletingUnclaimedItem() {
        WorkItem item = workItemService.createWorkItem("Unclaimed task", null, WorkItemPriority.LOW, engineeringTeam.getId(), engineer.getId());

        assertThatThrownBy(() -> workItemService.completeWorkItem(item.getId(), engineer.getId()))
                .isInstanceOf(InvalidWorkflowException.class)
                .hasMessageContaining("Only IN_PROGRESS work items can be completed");
    }

    @Test
    @DisplayName("Should change priority and record PRIORITY_CHANGED activity log")
    void shouldChangePrioritySuccessfully() {
        WorkItem item = workItemService.createWorkItem("Priority test", null, WorkItemPriority.LOW, engineeringTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item.getId(), engineer.getId());

        WorkItem updated = workItemService.changePriority(item.getId(), WorkItemPriority.URGENT, engineer.getId());
        assertThat(updated.getPriority()).isEqualTo(WorkItemPriority.URGENT);

        List<ActivityLog> history = workItemService.getActivityHistory(item.getId());
        assertThat(history).hasSize(3);
        assertThat(history.get(2).getActivityType()).isEqualTo(ActivityType.PRIORITY_CHANGED);
        assertThat(history.get(2).getDetails()).contains("LOW to URGENT");
    }

    @Test
    @DisplayName("Should allow team member to add collaboration note")
    void shouldAddCollaborationNote() {
        WorkItem item = workItemService.createWorkItem("Discussion item", null, WorkItemPriority.MEDIUM, engineeringTeam.getId(), engineer.getId());

        ActivityLog note = workItemService.addNote(item.getId(), teamLead.getId(), "Blocked by downstream dependency");

        assertThat(note.getId()).isNotNull();
        assertThat(note.getActivityType()).isEqualTo(ActivityType.NOTE_ADDED);
        assertThat(note.getDetails()).isEqualTo("Blocked by downstream dependency");
        assertThat(note.getActor().getId()).isEqualTo(teamLead.getId());

        List<ActivityLog> history = workItemService.getActivityHistory(item.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getActivityType()).isEqualTo(ActivityType.NOTE_ADDED);
    }

    @Test
    @DisplayName("Should reject blank note")
    void shouldRejectBlankNote() {
        WorkItem item = workItemService.createWorkItem("Note test", null, WorkItemPriority.LOW, engineeringTeam.getId(), engineer.getId());

        assertThatThrownBy(() -> workItemService.addNote(item.getId(), engineer.getId(), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Note cannot be blank");
    }

    @Test
    @DisplayName("Should retrieve work items filtered by team and status")
    void shouldRetrieveWorkItemsByTeamAndStatus() {
        WorkItem item1 = workItemService.createWorkItem("Task 1", null, WorkItemPriority.LOW, engineeringTeam.getId(), engineer.getId());
        WorkItem item2 = workItemService.createWorkItem("Task 2", null, WorkItemPriority.HIGH, engineeringTeam.getId(), engineer.getId());
        workItemService.claimWorkItem(item2.getId(), engineer.getId());

        List<WorkItem> openItems = workItemService.getWorkItemsForTeam(engineeringTeam.getId(), WorkItemStatus.OPEN);
        assertThat(openItems).hasSize(1).extracting(WorkItem::getTitle).contains("Task 1");

        List<WorkItem> inProgressItems = workItemService.getWorkItemsForTeam(engineeringTeam.getId(), WorkItemStatus.IN_PROGRESS);
        assertThat(inProgressItems).hasSize(1).extracting(WorkItem::getTitle).contains("Task 2");
    }
}
