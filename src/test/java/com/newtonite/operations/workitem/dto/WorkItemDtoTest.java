package com.newtonite.operations.workitem.dto;

import com.newtonite.operations.team.Team;
import com.newtonite.operations.user.User;
import com.newtonite.operations.workitem.WorkItem;
import com.newtonite.operations.workitem.WorkItemPriority;
import com.newtonite.operations.workitem.WorkItemStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WorkItemDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Should map entity fields completely to WorkItemResponse")
    void shouldMapEntityToResponseDto() {
        Team team = new Team("Core Team", "Team description");
        User creator = new User("Creator User", "creator@example.com");
        User assignee = new User("Assignee User", "assignee@example.com");

        WorkItem item = new WorkItem("Fix Outage", "Investigate pod crash", WorkItemPriority.URGENT, team, creator);
        item.claim(assignee);

        WorkItemResponse response = WorkItemResponse.fromEntity(item);

        assertThat(response.getTitle()).isEqualTo("Fix Outage");
        assertThat(response.getDescription()).isEqualTo("Investigate pod crash");
        assertThat(response.getStatus()).isEqualTo(WorkItemStatus.IN_PROGRESS);
        assertThat(response.getPriority()).isEqualTo(WorkItemPriority.URGENT);
        assertThat(response.getTeamName()).isEqualTo("Core Team");
        assertThat(response.getCreatorDisplayName()).isEqualTo("Creator User");
        assertThat(response.getAssigneeDisplayName()).isEqualTo("Assignee User");
        assertThat(response.getClaimedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should validate CreateWorkItemRequest mandatory constraints")
    void shouldValidateCreateRequestConstraints() {
        CreateWorkItemRequest invalid = new CreateWorkItemRequest("", "desc", null, null, null);
        Set<ConstraintViolation<CreateWorkItemRequest>> violations = validator.validate(invalid);

        assertThat(violations).hasSize(4);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactlyInAnyOrder("title", "priority", "teamId", "creatorId");
    }
}
