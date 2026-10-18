package com.newtonite.operations.workitem.dto;

import com.newtonite.operations.workitem.WorkItemPriority;
import jakarta.validation.constraints.NotNull;

public class ChangePriorityRequest {

    @NotNull(message = "Priority is required")
    private WorkItemPriority priority;

    @NotNull(message = "Actor user ID is required")
    private Long actorId;

    public ChangePriorityRequest() {
    }

    public ChangePriorityRequest(WorkItemPriority priority, Long actorId) {
        this.priority = priority;
        this.actorId = actorId;
    }

    public WorkItemPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkItemPriority priority) {
        this.priority = priority;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }
}
