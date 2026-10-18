package com.newtonite.operations.activity;

import com.newtonite.operations.team.Team;
import com.newtonite.operations.team.TeamRepository;
import com.newtonite.operations.user.User;
import com.newtonite.operations.user.UserRepository;
import com.newtonite.operations.workitem.WorkItem;
import com.newtonite.operations.workitem.WorkItemPriority;
import com.newtonite.operations.workitem.WorkItemRepository;
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
class ActivityLogRepositoryTest {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    private WorkItem workItem;
    private User actor;

    @BeforeEach
    void setUp() {
        Team team = teamRepository.save(new Team("SecOps", "Security"));
        actor = userRepository.save(new User("Sec Analyst", "sec@example.com"));
        workItem = workItemRepository.save(new WorkItem("Audit TLS", "Check v1.3", WorkItemPriority.MEDIUM, team, actor));
    }

    @Test
    @DisplayName("Should persist and retrieve activities in chronological order")
    void shouldPersistAndRetrieveChronologicalActivities() {
        ActivityLog log1 = new ActivityLog(workItem, actor, ActivityType.CREATED, "Created");
        ActivityLog log2 = new ActivityLog(workItem, actor, ActivityType.NOTE_ADDED, "Investigating certificates");

        activityLogRepository.save(log1);
        activityLogRepository.save(log2);
        activityLogRepository.flush();

        List<ActivityLog> history = activityLogRepository.findByWorkItemIdOrderByCreatedAtAsc(workItem.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getActivityType()).isEqualTo(ActivityType.CREATED);
        assertThat(history.get(1).getActivityType()).isEqualTo(ActivityType.NOTE_ADDED);
        assertThat(history.get(1).getDetails()).isEqualTo("Investigating certificates");

        assertThat(activityLogRepository.countByWorkItemId(workItem.getId())).isEqualTo(2);
    }
}
