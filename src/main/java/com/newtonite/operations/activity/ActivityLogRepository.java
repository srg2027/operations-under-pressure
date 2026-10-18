package com.newtonite.operations.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByWorkItemIdOrderByCreatedAtAsc(Long workItemId);

    List<ActivityLog> findByWorkItemIdOrderByCreatedAtDesc(Long workItemId);

    long countByWorkItemId(Long workItemId);
}
