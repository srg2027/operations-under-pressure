package com.newtonite.operations.workitem.dto;

import com.newtonite.operations.workitem.WorkItem;
import com.newtonite.operations.workitem.WorkItemPriority;
import com.newtonite.operations.workitem.WorkItemStatus;

import java.time.Instant;

public class WorkItemResponse {

    private Long id;
    private String title;
    private String description;
    private WorkItemStatus status;
    private WorkItemPriority priority;
    private Long teamId;
    private String teamName;
    private Long createdById;
    private String creatorDisplayName;
    private Long assigneeId;
    private String assigneeDisplayName;
    private Instant claimedAt;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public WorkItemResponse() {
    }

    public static WorkItemResponse fromEntity(WorkItem item) {
        WorkItemResponse dto = new WorkItemResponse();
        dto.id = item.getId();
        dto.title = item.getTitle();
        dto.description = item.getDescription();
        dto.status = item.getStatus();
        dto.priority = item.getPriority();
        dto.version = item.getVersion();
        dto.createdAt = item.getCreatedAt();
        dto.updatedAt = item.getUpdatedAt();
        dto.claimedAt = item.getClaimedAt();

        if (item.getTeam() != null) {
            dto.teamId = item.getTeam().getId();
            dto.teamName = item.getTeam().getName();
        }

        if (item.getCreatedBy() != null) {
            dto.createdById = item.getCreatedBy().getId();
            dto.creatorDisplayName = item.getCreatedBy().getDisplayName();
        }

        if (item.getAssignee() != null) {
            dto.assigneeId = item.getAssignee().getId();
            dto.assigneeDisplayName = item.getAssignee().getDisplayName();
        }

        return dto;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public WorkItemStatus getStatus() {
        return status;
    }

    public WorkItemPriority getPriority() {
        return priority;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public String getCreatorDisplayName() {
        return creatorDisplayName;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public String getAssigneeDisplayName() {
        return assigneeDisplayName;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
