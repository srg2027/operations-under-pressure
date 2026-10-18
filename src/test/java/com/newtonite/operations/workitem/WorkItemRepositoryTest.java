package com.newtonite.operations.workitem;

import com.newtonite.operations.team.Team;
import com.newtonite.operations.team.TeamRepository;
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

@SpringBootTest
@Transactional
class WorkItemRepositoryTest {

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    private Team testTeam;
    private User testUser;

    @BeforeEach
    void setUp() {
        testTeam = teamRepository.save(new Team("Platform Team", "Core infra"));
        testUser = userRepository.save(new User("Dev One", "dev1@example.com"));
    }

    @Test
    @DisplayName("Should persist work item and populate generated values and timestamps")
    void shouldPersistAndRetrieveWorkItem() {
        WorkItem item = new WorkItem("Fix DB connection pool", "Increase max connections", WorkItemPriority.HIGH, testTeam, testUser);
        WorkItem saved = workItemRepository.saveAndFlush(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull().isEqualTo(0L);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(WorkItemStatus.OPEN);
        assertThat(saved.getPriority()).isEqualTo(WorkItemPriority.HIGH);
        assertThat(saved.getTeam().getId()).isEqualTo(testTeam.getId());
        assertThat(saved.getCreatedBy().getId()).isEqualTo(testUser.getId());
        assertThat(saved.getAssignee()).isNull();
    }

    @Test
    @DisplayName("Should filter work items by status and count by status")
    void shouldFilterAndCountByStatus() {
        WorkItem openItem = new WorkItem("Open Task", null, WorkItemPriority.MEDIUM, testTeam, testUser);
        WorkItem inProgressItem = new WorkItem("In-progress Task", null, WorkItemPriority.HIGH, testTeam, testUser);
        inProgressItem.setStatus(WorkItemStatus.IN_PROGRESS);

        workItemRepository.save(openItem);
        workItemRepository.save(inProgressItem);
        workItemRepository.flush();

        List<WorkItem> openItems = workItemRepository.findByStatus(WorkItemStatus.OPEN);
        assertThat(openItems).extracting(WorkItem::getTitle).contains("Open Task");

        List<WorkItem> inProgressItems = workItemRepository.findByStatus(WorkItemStatus.IN_PROGRESS);
        assertThat(inProgressItems).extracting(WorkItem::getTitle).contains("In-progress Task");

        assertThat(workItemRepository.countByStatus(WorkItemStatus.OPEN)).isGreaterThanOrEqualTo(1);
        assertThat(workItemRepository.countByStatus(WorkItemStatus.DONE)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should filter by priority and combined status/priority")
    void shouldFilterByPriorityAndCombined() {
        WorkItem urgentOpen = new WorkItem("P0 Outage", "Production issue", WorkItemPriority.URGENT, testTeam, testUser);
        WorkItem lowOpen = new WorkItem("Doc typo", "Minor fix", WorkItemPriority.LOW, testTeam, testUser);

        workItemRepository.save(urgentOpen);
        workItemRepository.save(lowOpen);
        workItemRepository.flush();

        List<WorkItem> urgentItems = workItemRepository.findByPriority(WorkItemPriority.URGENT);
        assertThat(urgentItems).extracting(WorkItem::getTitle).contains("P0 Outage").doesNotContain("Doc typo");

        List<WorkItem> filtered = workItemRepository.findByStatusAndPriority(WorkItemStatus.OPEN, WorkItemPriority.URGENT);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getTitle()).isEqualTo("P0 Outage");
    }

    @Test
    @DisplayName("Should filter by multiple statuses")
    void shouldFilterByStatusIn() {
        WorkItem item1 = new WorkItem("Task 1", null, WorkItemPriority.LOW, testTeam, testUser);
        WorkItem item2 = new WorkItem("Task 2", null, WorkItemPriority.LOW, testTeam, testUser);
        item2.setStatus(WorkItemStatus.IN_PROGRESS);
        WorkItem item3 = new WorkItem("Task 3", null, WorkItemPriority.LOW, testTeam, testUser);
        item3.setStatus(WorkItemStatus.DONE);

        workItemRepository.saveAllAndFlush(List.of(item1, item2, item3));

        List<WorkItem> activeWork = workItemRepository.findByStatusIn(List.of(WorkItemStatus.OPEN, WorkItemStatus.IN_PROGRESS));
        assertThat(activeWork).extracting(WorkItem::getTitle)
                .contains("Task 1", "Task 2")
                .doesNotContain("Task 3");
    }

    @Test
    @DisplayName("Should filter by team and assignee")
    void shouldFilterByTeamAndAssignee() {
        User assignee = userRepository.save(new User("Claiming Dev", "claim@example.com"));

        WorkItem claimedItem = new WorkItem("Claimed task", null, WorkItemPriority.HIGH, testTeam, testUser);
        claimedItem.claim(assignee);
        workItemRepository.saveAndFlush(claimedItem);

        List<WorkItem> teamItems = workItemRepository.findByTeamId(testTeam.getId());
        assertThat(teamItems).hasSize(1);

        List<WorkItem> assignedItems = workItemRepository.findByAssigneeId(assignee.getId());
        assertThat(assignedItems).hasSize(1);
        assertThat(assignedItems.get(0).getAssignee().getEmail()).isEqualTo("claim@example.com");
    }

    @Test
    @DisplayName("Should increment version on update via optimistic locking")
    void shouldIncrementVersionOnUpdate() {
        WorkItem item = workItemRepository.saveAndFlush(new WorkItem("Task to update", null, WorkItemPriority.LOW, testTeam, testUser));
        Long initialVersion = item.getVersion();

        item.setStatus(WorkItemStatus.IN_PROGRESS);
        WorkItem updated = workItemRepository.saveAndFlush(item);

        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }
}
