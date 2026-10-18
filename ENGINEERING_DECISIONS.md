# Engineering Decisions & Architecture Summary
**Project:** Newtonite Software Engineering Challenge — Operations Under Pressure  
**Date:** October 17, 2026  
**Stack:** Java 17, Spring Boot 4.1.1, Spring Data JPA, Hibernate Validator, PostgreSQL (H2 for test isolation), Maven

---

## 1. Domain Model & Optimistic Concurrency

- **WorkItem Concurrency Control:**
  - Implemented `@Version` on the [`WorkItem`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/workitem/WorkItem.java) entity to manage concurrent operational updates and avoid lost updates without pesimistic database locking.
  - Concurrent claims, state transitions, reassignments, or priority modifications trigger `OptimisticLockingFailureException`, which is caught and mapped to HTTP `409 CONFLICT` via [`WorkItemConflictException`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/common/exception/WorkItemConflictException.java).
- **Domain Status Transitions:**
  - Strict lifecycle state transitions (`OPEN` -> `IN_PROGRESS` -> `COMPLETED`, plus `RELEASED` back to `OPEN`).
  - Transitions enforced inside domain entities and validated by [`WorkItemService`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/workitem/WorkItemService.java), raising [`InvalidWorkflowException`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/common/exception/InvalidWorkflowException.java).

---

## 2. Activity History & Append-Only Audit Logging

- **Audit Entity ([`ActivityLog`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/activity/ActivityLog.java)):**
  - Designed as an append-only event stream recording events: `CREATED`, `CLAIMED`, `RELEASED`, `REASSIGNED`, `COMPLETED`, `PRIORITY_CHANGED`, and `NOTE_ADDED`.
  - Linked to the associated work item, acting user, timestamp (`Instant.now()`), event type, and details.
  - Transactions encompass domain changes and audit log entries together, ensuring rollback on failure to maintain audit integrity.

---

## 3. Team Authorization & Membership Rules

- **Membership Verification:**
  - Operations like creating work items, claiming, reassigning, or commenting verify membership via [`TeamMembershipRepository`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/team/TeamMembershipRepository.java).
  - Work item management (release, complete, reassign, change priority) strictly requires the acting user to either be the current assignee or hold the `TEAM_LEAD` role in that team.
  - Membership addition to existing teams requires authorization by an active `TEAM_LEAD`.
  - Membership removal is restricted to `TEAM_LEAD` or self-removal.
  - Inactive users are barred from creating work items, joining teams, or performing actions.

---

## 4. DTO-First API Layer & Clean Architecture

- **Information Protection:**
  - Entities (`User`, `Team`, `TeamMembership`, `WorkItem`, `ActivityLog`) are strictly encapsulated within the domain layer and are never returned or accepted directly in controllers.
  - All endpoints use dedicated validated DTOs:
    - User: [`CreateUserRequest`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/user/dto/CreateUserRequest.java), [`UserResponse`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/user/dto/UserResponse.java).
    - Team: [`CreateTeamRequest`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/team/dto/CreateTeamRequest.java), [`AddMemberRequest`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/team/dto/AddMemberRequest.java), [`TeamResponse`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/team/dto/TeamResponse.java), [`TeamMemberResponse`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/team/dto/TeamMemberResponse.java).
    - WorkItem: [`CreateWorkItemRequest`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/workitem/dto/CreateWorkItemRequest.java), [`ChangePriorityRequest`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/workitem/dto/ChangePriorityRequest.java), [`ReassignWorkItemRequest`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/workitem/dto/ReassignWorkItemRequest.java), [`AddNoteRequest`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/workitem/dto/AddNoteRequest.java), [`WorkItemResponse`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/workitem/dto/WorkItemResponse.java).
    - Activity: [`ActivityLogResponse`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/activity/dto/ActivityLogResponse.java).

---

## 5. Centralized Exception Handling & Uniform Error Responses

- **[`GlobalExceptionHandler`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/common/exception/GlobalExceptionHandler.java):**
  - Catches application exceptions and generates standard [`ErrorResponse`](file:///C:/Users/dixit/Downloads/operations/operations/src/main/java/com/newtonite/operations/common/exception/ErrorResponse.java) structures with timestamp, status code, error message, path, and field validation error maps.
  - HTTP 400 (`MethodArgumentNotValidException`, `IllegalArgumentException`).
  - HTTP 403 (`UnauthorizedOperationException`).
  - HTTP 404 (`ResourceNotFoundException`).
  - HTTP 409 (`WorkItemConflictException`, `InvalidWorkflowException`).
  - HTTP 500 (Sanitized generic exceptions; internal trace details are not leaked).

---

## 6. Testing & Quality Assurance Verification

- **Total Tests Passing:** 67 tests across 12 test suites with 0 failures, 0 errors, and 0 skipped.
- **Suite Breakdown:**
  - `OperationsApplicationTests`: Spring Boot context load test.
  - `TeamRepositoryTest`: Team entity uniqueness and queries.
  - `UserRepositoryTest`: User lookup, normalization, and status filtering.
  - `TeamMembershipRepositoryTest`: Membership uniqueness and role mappings.
  - `WorkItemRepositoryTest`: Filter queries by status, team, and assignee.
  - `ActivityLogRepositoryTest`: Append-only activity query and sorting.
  - `WorkItemServiceTest`: Concurrency, claims, release, notes, priority updates, and auth.
  - `GlobalExceptionHandlerTest`: Error mapping assertions.
  - `WorkItemControllerTest`: MockMvc integration tests for work-item workflow.
  - `UserControllerTest`: MockMvc integration tests for user endpoints.
  - `TeamControllerTest`: MockMvc integration tests for team & membership endpoints.
