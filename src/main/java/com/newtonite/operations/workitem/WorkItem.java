package com.newtonite.operations.workitem;

import com.newtonite.operations.team.Team;
import com.newtonite.operations.user.User;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity representing a work item in the Operations Under Pressure application.
 */
@Entity
@Table(
        name = "work_items",
        indexes = {
                @Index(name = "idx_work_items_team_id", columnList = "team_id"),
                @Index(name = "idx_work_items_status", columnList = "status"),
                @Index(name = "idx_work_items_assignee_id", columnList = "assignee_id"),
                @Index(name = "idx_work_items_team_status", columnList = "team_id, status")
        }
)
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WorkItemStatus status = WorkItemStatus.OPEN;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private WorkItemPriority priority = WorkItemPriority.MEDIUM;

    @NotNull(message = "Team is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "team_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_work_items_team")
    )
    private Team team;

    @NotNull(message = "Creator is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "created_by_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_work_items_creator")
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "assignee_id",
            foreignKey = @ForeignKey(name = "fk_work_items_assignee")
    )
    private User assignee;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor required by the JPA specification.
     */
    public WorkItem() {
    }

    /**
     * Convenience constructor for initializing a new work item with ownership.
     *
     * @param title       Required work item title.
     * @param description Optional detailed description.
     * @param priority    Initial priority level (defaults to MEDIUM if null).
     * @param team        Owning team.
     * @param createdBy   Authoring user.
     */
    public WorkItem(String title, String description, WorkItemPriority priority, Team team, User createdBy) {
        this.title = title;
        this.description = description;
        this.priority = priority != null ? priority : WorkItemPriority.MEDIUM;
        this.status = WorkItemStatus.OPEN;
        this.team = team;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
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

    public WorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(WorkItemStatus status) {
        this.status = status;
    }

    public WorkItemPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkItemPriority priority) {
        this.priority = priority;
    }

    /**
     * Optimistic locking version field managed by JPA.
     * No setter is provided to prevent manual alteration.
     */
    public Long getVersion() {
        return version;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    /**
     * Domain method to claim this work item by a user.
     */
    public void claim(User claimingUser) {
        this.assignee = claimingUser;
        this.status = WorkItemStatus.IN_PROGRESS;
        this.claimedAt = Instant.now();
    }

    /**
     * Domain method to release/unclaim this work item back to the team queue.
     */
    public void release() {
        this.assignee = null;
        this.status = WorkItemStatus.OPEN;
        this.claimedAt = null;
    }

    /**
     * Domain method to mark this work item as completed.
     */
    public void complete() {
        this.status = WorkItemStatus.DONE;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // --- Object Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkItem workItem = (WorkItem) o;
        return id != null && Objects.equals(id, workItem.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "WorkItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", version=" + version +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
