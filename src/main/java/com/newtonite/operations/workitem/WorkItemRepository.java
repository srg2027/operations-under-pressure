package com.newtonite.operations.workitem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {

    List<WorkItem> findByStatus(WorkItemStatus status);

    List<WorkItem> findByPriority(WorkItemPriority priority);

    List<WorkItem> findByStatusAndPriority(WorkItemStatus status, WorkItemPriority priority);

    List<WorkItem> findByStatusIn(Collection<WorkItemStatus> statuses);

    long countByStatus(WorkItemStatus status);

    List<WorkItem> findByTeamId(Long teamId);

    List<WorkItem> findByTeamIdAndStatus(Long teamId, WorkItemStatus status);

    List<WorkItem> findByAssigneeId(Long assigneeId);

    List<WorkItem> findByAssigneeIdAndStatus(Long assigneeId, WorkItemStatus status);

    long countByTeamIdAndStatus(Long teamId, WorkItemStatus status);
}
