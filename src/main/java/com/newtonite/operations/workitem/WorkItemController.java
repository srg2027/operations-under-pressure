package com.newtonite.operations.workitem;

import com.newtonite.operations.activity.ActivityLog;
import com.newtonite.operations.activity.dto.ActivityLogResponse;
import com.newtonite.operations.workitem.dto.AddNoteRequest;
import com.newtonite.operations.workitem.dto.ChangePriorityRequest;
import com.newtonite.operations.workitem.dto.CreateWorkItemRequest;
import com.newtonite.operations.workitem.dto.ReassignWorkItemRequest;
import com.newtonite.operations.workitem.dto.WorkItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-items")
public class WorkItemController {

    private final WorkItemService workItemService;

    public WorkItemController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @PostMapping
    public ResponseEntity<WorkItemResponse> createWorkItem(@Valid @RequestBody CreateWorkItemRequest request) {
        WorkItem created = workItemService.createWorkItem(
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getTeamId(),
                request.getCreatorId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkItemResponse.fromEntity(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkItemResponse> getWorkItem(@PathVariable Long id) {
        WorkItem workItem = workItemService.getWorkItem(id);
        return ResponseEntity.ok(WorkItemResponse.fromEntity(workItem));
    }

    @GetMapping
    public ResponseEntity<List<WorkItemResponse>> getTeamWorkItems(
            @RequestParam Long teamId,
            @RequestParam(required = false) WorkItemStatus status
    ) {
        List<WorkItem> items = workItemService.getWorkItemsForTeam(teamId, status);
        List<WorkItemResponse> response = items.stream()
                .map(WorkItemResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/assigned/{userId}")
    public ResponseEntity<List<WorkItemResponse>> getAssignedWorkItems(
            @PathVariable Long userId,
            @RequestParam(required = false) WorkItemStatus status
    ) {
        List<WorkItem> items = workItemService.getWorkItemsForAssignee(userId, status);
        List<WorkItemResponse> response = items.stream()
                .map(WorkItemResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<WorkItemResponse> claimWorkItem(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        WorkItem claimed = workItemService.claimWorkItem(id, userId);
        return ResponseEntity.ok(WorkItemResponse.fromEntity(claimed));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<WorkItemResponse> releaseWorkItem(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        WorkItem released = workItemService.releaseWorkItem(id, userId);
        return ResponseEntity.ok(WorkItemResponse.fromEntity(released));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<WorkItemResponse> completeWorkItem(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        WorkItem completed = workItemService.completeWorkItem(id, userId);
        return ResponseEntity.ok(WorkItemResponse.fromEntity(completed));
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<WorkItemResponse> reassignWorkItem(
            @PathVariable Long id,
            @Valid @RequestBody ReassignWorkItemRequest request
    ) {
        WorkItem reassigned = workItemService.reassignWorkItem(id, request.getTargetUserId(), request.getActingUserId());
        return ResponseEntity.ok(WorkItemResponse.fromEntity(reassigned));
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<WorkItemResponse> changePriority(
            @PathVariable Long id,
            @Valid @RequestBody ChangePriorityRequest request
    ) {
        WorkItem updated = workItemService.changePriority(id, request.getPriority(), request.getActorId());
        return ResponseEntity.ok(WorkItemResponse.fromEntity(updated));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ActivityLogResponse> addNote(
            @PathVariable Long id,
            @Valid @RequestBody AddNoteRequest request
    ) {
        ActivityLog noteLog = workItemService.addNote(id, request.getActorId(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED).body(ActivityLogResponse.fromEntity(noteLog));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ActivityLogResponse>> getActivityHistory(@PathVariable Long id) {
        List<ActivityLog> history = workItemService.getActivityHistory(id);
        List<ActivityLogResponse> response = history.stream()
                .map(ActivityLogResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
