package com.newtonite.operations.activity;

import com.newtonite.operations.user.User;
import com.newtonite.operations.workitem.WorkItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

/**
 * Append-only JPA entity capturing audit trails, workflow transitions, and collaboration notes for a work item.
 */
@Entity
@Table(
        name = "activity_logs",
        indexes = {
                @Index(name = "idx_activity_work_item_created", columnList = "work_item_id, created_at")
        }
)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Work item is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "work_item_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_activities_work_item")
    )
    private WorkItem workItem;

    @NotNull(message = "Actor is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "actor_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_activities_actor")
    )
    private User actor;

    @NotNull(message = "Activity type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, updatable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "details", columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Default constructor required by the JPA specification.
     */
    public ActivityLog() {
    }

    /**
     * Constructor for creating an append-only historical log entry.
     *
     * @param workItem     Work item the activity applies to.
     * @param actor        User performing the action.
     * @param activityType Nature of the event.
     * @param details      Descriptive summary or note.
     */
    public ActivityLog(WorkItem workItem, User actor, ActivityType activityType, String details) {
        this.workItem = workItem;
        this.actor = actor;
        this.activityType = activityType;
        this.details = details;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Read-Only Accessors (Append-Only Invariant) ---

    public Long getId() {
        return id;
    }

    public WorkItem getWorkItem() {
        return workItem;
    }

    public User getActor() {
        return actor;
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

    // --- Object Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityLog that = (ActivityLog) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ActivityLog{" +
                "id=" + id +
                ", workItemId=" + (workItem != null ? workItem.getId() : null) +
                ", actorId=" + (actor != null ? actor.getId() : null) +
                ", activityType=" + activityType +
                ", createdAt=" + createdAt +
                '}';
    }
}
