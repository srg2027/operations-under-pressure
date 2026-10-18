package com.newtonite.operations.workitem;

import com.newtonite.operations.activity.ActivityLog;
import com.newtonite.operations.activity.ActivityLogRepository;
import com.newtonite.operations.activity.ActivityType;
import com.newtonite.operations.common.exception.InvalidWorkflowException;
import com.newtonite.operations.common.exception.ResourceNotFoundException;
import com.newtonite.operations.common.exception.UnauthorizedOperationException;
import com.newtonite.operations.common.exception.WorkItemConflictException;
import com.newtonite.operations.team.Team;
import com.newtonite.operations.team.TeamMembership;
import com.newtonite.operations.team.TeamMembershipRepository;
import com.newtonite.operations.team.TeamRepository;
import com.newtonite.operations.team.TeamRole;
import com.newtonite.operations.user.User;
import com.newtonite.operations.user.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final ActivityLogRepository activityLogRepository;

    public WorkItemService(WorkItemRepository workItemRepository,
                           TeamRepository teamRepository,
                           UserRepository userRepository,
                           TeamMembershipRepository teamMembershipRepository,
                           ActivityLogRepository activityLogRepository) {
        this.workItemRepository = workItemRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.activityLogRepository = activityLogRepository;
    }

    /**
     * Creates a new work item routed to a specific team by an authorized team member.
     */
    public WorkItem createWorkItem(String title, String description, WorkItemPriority priority, Long teamId, Long creatorId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + teamId));

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + creatorId));

        if (!creator.isActive()) {
            throw new UnauthorizedOperationException("Inactive users cannot create work items");
        }

        if (!teamMembershipRepository.existsByTeamIdAndUserId(teamId, creatorId)) {
            throw new UnauthorizedOperationException("User is not a member of team: " + team.getName());
        }

        WorkItem workItem = new WorkItem(title, description, priority, team, creator);
        WorkItem saved = workItemRepository.save(workItem);

        activityLogRepository.save(new ActivityLog(
                saved,
                creator,
                ActivityType.CREATED,
                "Work item created in team " + team.getName()
        ));

        return saved;
    }

    /**
     * Claims an open work item for the requesting user with optimistic concurrency safety.
     */
    public WorkItem claimWorkItem(Long workItemId, Long userId) {
        WorkItem workItem = findWorkItemOrThrow(workItemId);
        User user = findActiveUserOrThrow(userId);

        if (!teamMembershipRepository.existsByTeamIdAndUserId(workItem.getTeam().getId(), userId)) {
            throw new UnauthorizedOperationException("User is not a member of team: " + workItem.getTeam().getName());
        }

        if (workItem.getStatus() != WorkItemStatus.OPEN) {
            throw new InvalidWorkflowException("Only OPEN work items can be claimed. Current status: " + workItem.getStatus());
        }

        try {
            workItem.claim(user);
            WorkItem saved = workItemRepository.saveAndFlush(workItem);

            activityLogRepository.save(new ActivityLog(
                    saved,
                    user,
                    ActivityType.CLAIMED,
                    "Claimed by " + user.getDisplayName()
            ));

            return saved;
        } catch (OptimisticLockingFailureException ex) {
            throw new WorkItemConflictException("Work item was modified or claimed concurrently by another user", ex);
        }
    }

    /**
     * Releases an in-progress work item back to the team queue.
     * Allowed only for the current assignee or a team lead.
     */
    public WorkItem releaseWorkItem(Long workItemId, Long userId) {
        WorkItem workItem = findWorkItemOrThrow(workItemId);
        User user = findActiveUserOrThrow(userId);

        if (workItem.getStatus() != WorkItemStatus.IN_PROGRESS) {
            throw new InvalidWorkflowException("Only IN_PROGRESS work items can be released. Current status: " + workItem.getStatus());
        }

        assertUserCanManageWorkItem(workItem, user);

        try {
            workItem.release();
            WorkItem saved = workItemRepository.saveAndFlush(workItem);

            activityLogRepository.save(new ActivityLog(
                    saved,
                    user,
                    ActivityType.RELEASED,
                    "Released back to team queue by " + user.getDisplayName()
            ));

            return saved;
        } catch (OptimisticLockingFailureException ex) {
            throw new WorkItemConflictException("Work item was modified concurrently", ex);
        }
    }

    /**
     * Marks an in-progress work item as completed.
     * Allowed only for the current assignee or a team lead.
     */
    public WorkItem completeWorkItem(Long workItemId, Long userId) {
        WorkItem workItem = findWorkItemOrThrow(workItemId);
        User user = findActiveUserOrThrow(userId);

        if (workItem.getStatus() != WorkItemStatus.IN_PROGRESS) {
            throw new InvalidWorkflowException("Only IN_PROGRESS work items can be completed. Current status: " + workItem.getStatus());
        }

        assertUserCanManageWorkItem(workItem, user);

        try {
            workItem.complete();
            WorkItem saved = workItemRepository.saveAndFlush(workItem);

            activityLogRepository.save(new ActivityLog(
                    saved,
                    user,
                    ActivityType.COMPLETED,
                    "Marked as completed by " + user.getDisplayName()
            ));

            return saved;
        } catch (OptimisticLockingFailureException ex) {
            throw new WorkItemConflictException("Work item was modified concurrently", ex);
        }
    }

    /**
     * Reassigns an active work item to another member of the same team.
     * Allowed for team leads or the current assignee.
     */
    public WorkItem reassignWorkItem(Long workItemId, Long targetUserId, Long actingUserId) {
        WorkItem workItem = findWorkItemOrThrow(workItemId);
        User actingUser = findActiveUserOrThrow(actingUserId);
        User targetUser = findActiveUserOrThrow(targetUserId);

        assertUserCanManageWorkItem(workItem, actingUser);

        if (!teamMembershipRepository.existsByTeamIdAndUserId(workItem.getTeam().getId(), targetUserId)) {
            throw new UnauthorizedOperationException("Target user is not a member of team: " + workItem.getTeam().getName());
        }

        try {
            workItem.claim(targetUser);
            WorkItem saved = workItemRepository.saveAndFlush(workItem);

            activityLogRepository.save(new ActivityLog(
                    saved,
                    actingUser,
                    ActivityType.REASSIGNED,
                    "Reassigned to " + targetUser.getDisplayName() + " by " + actingUser.getDisplayName()
            ));

            return saved;
        } catch (OptimisticLockingFailureException ex) {
            throw new WorkItemConflictException("Work item was modified concurrently", ex);
        }
    }

    /**
     * Changes priority of a work item.
     * Allowed for the assignee or a team lead.
     */
    public WorkItem changePriority(Long workItemId, WorkItemPriority newPriority, Long actingUserId) {
        WorkItem workItem = findWorkItemOrThrow(workItemId);
        User actingUser = findActiveUserOrThrow(actingUserId);

        assertUserCanManageWorkItem(workItem, actingUser);

        WorkItemPriority oldPriority = workItem.getPriority();
        if (oldPriority == newPriority) {
            return workItem;
        }

        try {
            workItem.setPriority(newPriority);
            WorkItem saved = workItemRepository.saveAndFlush(workItem);

            activityLogRepository.save(new ActivityLog(
                    saved,
                    actingUser,
                    ActivityType.PRIORITY_CHANGED,
                    "Priority changed from " + oldPriority + " to " + newPriority
            ));

            return saved;
        } catch (OptimisticLockingFailureException ex) {
            throw new WorkItemConflictException("Work item was modified concurrently", ex);
        }
    }

    /**
     * Appends a collaborative note/comment to a work item.
     * Allowed for any active team member.
     */
    public ActivityLog addNote(Long workItemId, Long actorId, String note) {
        if (note == null || note.trim().isEmpty()) {
            throw new IllegalArgumentException("Note cannot be blank");
        }

        WorkItem workItem = findWorkItemOrThrow(workItemId);
        User actor = findActiveUserOrThrow(actorId);

        if (!teamMembershipRepository.existsByTeamIdAndUserId(workItem.getTeam().getId(), actorId)) {
            throw new UnauthorizedOperationException("User is not a member of team: " + workItem.getTeam().getName());
        }

        ActivityLog activityLog = new ActivityLog(workItem, actor, ActivityType.NOTE_ADDED, note.trim());
        return activityLogRepository.save(activityLog);
    }

    @Transactional(readOnly = true)
    public WorkItem getWorkItem(Long workItemId) {
        return findWorkItemOrThrow(workItemId);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> getWorkItemsForTeam(Long teamId, WorkItemStatus status) {
        if (status != null) {
            return workItemRepository.findByTeamIdAndStatus(teamId, status);
        }
        return workItemRepository.findByTeamId(teamId);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> getWorkItemsForAssignee(Long userId, WorkItemStatus status) {
        if (status != null) {
            return workItemRepository.findByAssigneeIdAndStatus(userId, status);
        }
        return workItemRepository.findByAssigneeId(userId);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getActivityHistory(Long workItemId) {
        findWorkItemOrThrow(workItemId);
        return activityLogRepository.findByWorkItemIdOrderByCreatedAtAsc(workItemId);
    }

    // --- Helper Validation Methods ---

    private WorkItem findWorkItemOrThrow(Long workItemId) {
        return workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found with ID: " + workItemId));
    }

    private User findActiveUserOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (!user.isActive()) {
            throw new UnauthorizedOperationException("User is inactive: " + user.getEmail());
        }
        return user;
    }

    private void assertUserCanManageWorkItem(WorkItem workItem, User user) {
        boolean isAssignee = workItem.getAssignee() != null && workItem.getAssignee().getId().equals(user.getId());
        if (isAssignee) {
            return;
        }

        Optional<TeamMembership> membership = teamMembershipRepository
                .findByTeamIdAndUserId(workItem.getTeam().getId(), user.getId());

        boolean isLead = membership.isPresent() && membership.get().getRole() == TeamRole.TEAM_LEAD;
        if (!isLead) {
            throw new UnauthorizedOperationException("Only the assigned user or a Team Lead may modify this work item");
        }
    }
}
