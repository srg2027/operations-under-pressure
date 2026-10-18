package com.newtonite.operations.activity.dto;

import com.newtonite.operations.activity.ActivityLog;
import com.newtonite.operations.activity.ActivityType;

import java.time.Instant;

public class ActivityLogResponse {

    private Long id;
    private Long workItemId;
    private Long actorId;
    private String actorDisplayName;
    private ActivityType activityType;
    private String details;
    private Instant createdAt;

    public ActivityLogResponse() {
    }

    public static ActivityLogResponse fromEntity(ActivityLog log) {
        ActivityLogResponse dto = new ActivityLogResponse();
        dto.id = log.getId();
        dto.activityType = log.getActivityType();
        dto.details = log.getDetails();
        dto.createdAt = log.getCreatedAt();

        if (log.getWorkItem() != null) {
            dto.workItemId = log.getWorkItem().getId();
        }
        if (log.getActor() != null) {
            dto.actorId = log.getActor().getId();
            dto.actorDisplayName = log.getActor().getDisplayName();
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkItemId() {
        return workItemId;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
