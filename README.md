# Newtonite Operations Under Pressure — Service Application

A robust, resilient backend service engineered for high-concurrency operations under pressure. Built with Spring Boot 4.1.1, Java 17, and PostgreSQL (with H2 for isolated integration tests).

---

## Key Capabilities

1. **Optimistic Concurrency Control:**
   - Database records maintain versioning (`@Version`) preventing race conditions, dirty overwrites, or double-claims during high-load incident workflows.
   - Concurrency conflicts return structured HTTP `409 CONFLICT`.

2. **Append-Only Audit History:**
   - Every state change (`CREATED`, `CLAIMED`, `RELEASED`, `REASSIGNED`, `COMPLETED`, `PRIORITY_CHANGED`, `NOTE_ADDED`) is permanently recorded in an append-only audit trail with the actor and timestamp.

3. **Team Membership & Authorization:**
   - Granular access enforcement ensures only team members can claim, view, or comment on work items.
   - Privileged operations (reassignment, lead overrides, membership changes) require `TEAM_LEAD` status.

4. **Hermetic & Clean Architecture:**
   - Internal JPA entities are never exposed across the REST boundary.
   - Comprehensive `@RestControllerAdvice` error handling delivers consistent, sanitized JSON responses.

---

## Build & Test Instructions

### Prerequisites
- Java 17 JDK
- PostgreSQL (for production runtime; tests use in-memory PostgreSQL-mode H2)

### Running Tests
Execute the full test suite with Maven:
```bash
./mvnw clean test
```

### Building the Package
Package the executable JAR:
```bash
./mvnw clean verify
```

### Running the Application
Set environment variables for PostgreSQL (or default to localhost:5432):
```bash
# Windows PowerShell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/operations"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your_password_here"
java -jar target/operations-0.0.1-SNAPSHOT.jar

# Linux/macOS
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/operations
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_password_here
java -jar target/operations-0.0.1-SNAPSHOT.jar
```

---

## Database Configuration & Demo Setup

### Automatic Schema Initialization
The service uses Spring Data JPA with `spring.jpa.hibernate.ddl-auto=update` and PostgreSQL dialect (`org.hibernate.dialect.PostgreSQLDialect`). On first startup against a fresh database (e.g. `operations` or disposable `operations_test`), Hibernate automatically initializes all tables, unique constraints, and foreign keys:
- `users` (unique email constraint `uk_users_email`)
- `teams` (unique name constraint `uk_teams_name`)
- `team_memberships` (composite unique constraint `uk_team_memberships_team_user`, FKs to teams and users)
- `work_items` (optimistic locking `@Version`, FKs to teams and users, indexes on status, team, and assignee)
- `activity_logs` (append-only audit log, composite index on `(work_item_id, created_at)`)

### Fresh / Disposable Database Setup
To test against a completely separate, clean database without touching existing operational data:
```sql
CREATE DATABASE operations_test;
```
Then configure the application to target `operations_test`:
```bash
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/operations_test"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your_password_here"
```

### Initial Demo Data Setup (via REST API)
1. **Create an Operational User:**
   ```bash
   curl -X POST http://localhost:8080/api/users \
     -H "Content-Type: application/json" \
     -d '{"displayName": "Alice Admin", "email": "alice@newtonite.com"}'
   ```
2. **Create a Team:**
   ```bash
   curl -X POST http://localhost:8080/api/teams \
     -H "Content-Type: application/json" \
     -d '{"name": "Site Reliability Engineering", "description": "24/7 Operations"}'
   ```
3. **Bootstrap Initial Team Lead (First Member):**
   ```bash
   curl -X POST "http://localhost:8080/api/teams/1/members" \
     -H "Content-Type: application/json" \
     -d '{"userId": 1, "role": "TEAM_LEAD"}'
   ```
4. **Create a Work Item:**
   ```bash
   curl -X POST http://localhost:8080/api/work-items \
     -H "Content-Type: application/json" \
     -d '{"title": "Investigate CPU spike on prod cluster", "description": "Node pool 3 high usage", "priority": "HIGH", "teamId": 1, "creatorId": 1}'
   ```
5. **Claim the Work Item (Optimistic Concurrency Protected):**
   ```bash
   curl -X POST "http://localhost:8080/api/work-items/1/claim?userId=1"
   ```

---

## REST API Overview

### Users
- `POST /api/users` - Create user
- `GET /api/users/{id}` - Get user details
- `GET /api/users?activeOnly=true` - List users

### Teams
- `POST /api/teams` - Create operational team
- `GET /api/teams` - List all teams
- `GET /api/teams/{id}` - Get team details
- `POST /api/teams/{id}/members?actingUserId={id}` - Add member to team (requires Team Lead if team has existing members)
- `DELETE /api/teams/{id}/members/{userId}?actingUserId={id}` - Remove member (Team Lead or self-removal)
- `GET /api/teams/{id}/members` - List team members

### Work Items
- `POST /api/work-items` - Create work item (requires team membership)
- `GET /api/work-items/{id}` - Get work item details
- `GET /api/work-items?teamId={id}&status={status}` - Filter work items by team
- `GET /api/work-items/assigned/{userId}?status={status}` - Get items assigned to user
- `POST /api/work-items/{id}/claim?userId={id}` - Claim an open item (optimistic locking protected)
- `POST /api/work-items/{id}/release?userId={id}` - Release in-progress item back to team queue (assignee or Team Lead)
- `POST /api/work-items/{id}/complete?userId={id}` - Mark in-progress item as done (assignee or Team Lead)
- `POST /api/work-items/{id}/reassign` - Reassign item to another team member (assignee or Team Lead)
- `PATCH /api/work-items/{id}/priority` - Update item priority (assignee or Team Lead)
- `POST /api/work-items/{id}/notes` - Add collaborative note/comment (any team member)
- `GET /api/work-items/{id}/history` - Retrieve chronological activity and audit history

---

## Frontend React Application

A modern single-page application built with React 19, Vite, TanStack Query, React Router, and Tailwind CSS, located in `frontend/`.

### Frontend Setup & Development
```bash
cd frontend
npm install
npm run dev
```
The development server launches at `http://localhost:5173`. All `/api/*` endpoints are automatically proxied to `http://localhost:8080`.

### Building for Production
```bash
cd frontend
npm run build
```
Generates optimized static assets in `frontend/dist/`.

### Running Frontend Tests
```bash
cd frontend
npm test
```
Executes the Vitest unit and integration test suite (badges, session management, HTTP 409 conflict banners, and dashboard unavailable-state verification).

### Architectural Implementation Details
1. **Demo Identity Selector:** As the backend does not implement Spring Security or JWT, the frontend provides a `"Who are you?"` operator picker backed by `GET /api/users?activeOnly=true`. The active user ID is persisted in `localStorage` and supplied as query/body parameters for authorization checks.
2. **Dashboard Panels:**
   - **Assigned to me:** Work items assigned to the current user via `GET /api/work-items/assigned/{userId}`.
   - **Unassigned urgent:** High/urgent priority items awaiting claim across active teams with direct claim action and conflict handling.
   - **Awaiting my approval:** Polished unavailable-state placeholder confirming that the frozen schema implements direct lifecycle transitions (`OPEN → IN_PROGRESS → DONE`) without multi-tier approval workflows.
3. **Collaboration & Audit History:** Real-time notes posting (`POST /api/work-items/{id}/notes`) and full audit trail timeline (`GET /api/work-items/{id}/history`).
4. **Conflict Handling:** Prominently detects HTTP 409 state conflicts and optimistic locking failures, alerting the operator and offering one-click data reload.

