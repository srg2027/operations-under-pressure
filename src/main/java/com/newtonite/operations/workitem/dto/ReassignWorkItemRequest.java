package com.newtonite.operations.workitem.dto;

import jakarta.validation.constraints.NotNull;

public class ReassignWorkItemRequest {

    @NotNull(message = "Target user ID is required")
    private Long targetUserId;

    @NotNull(message = "Acting user ID is required")
    private Long actingUserId;

    public ReassignWorkItemRequest() {
    }

    public ReassignWorkItemRequest(Long targetUserId, Long actingUserId) {
        this.targetUserId = targetUserId;
        this.actingUserId = actingUserId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Long getActingUserId() {
        return actingUserId;
    }

    public void setActingUserId(Long actingUserId) {
        this.actingUserId = actingUserId;
    }
}
