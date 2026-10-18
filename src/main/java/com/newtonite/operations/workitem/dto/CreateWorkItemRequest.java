package com.newtonite.operations.workitem.dto;

import com.newtonite.operations.workitem.WorkItemPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateWorkItemRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private WorkItemPriority priority = WorkItemPriority.MEDIUM;

    @NotNull(message = "Team ID is required")
    private Long teamId;

    @NotNull(message = "Creator user ID is required")
    private Long creatorId;

    public CreateWorkItemRequest() {
    }

    public CreateWorkItemRequest(String title, String description, WorkItemPriority priority, Long teamId, Long creatorId) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.teamId = teamId;
        this.creatorId = creatorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkItemPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkItemPriority priority) {
        this.priority = priority;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }
}
