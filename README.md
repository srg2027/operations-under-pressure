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

---

## Database Configuration & Demo Setup

### Automatic Schema & Demo Data Initialization
The service uses Spring Data JPA with `spring.jpa.hibernate.ddl-auto=update` and PostgreSQL dialect (`org.hibernate.dialect.PostgreSQLDialect`). On startup against any clean database (e.g. `operations` or disposable `operations_test`), Hibernate automatically initializes all tables, unique constraints, and foreign keys:
- `users` (unique email constraint `uk_users_email`)
- `teams` (unique name constraint `uk_teams_name`)
- `team_memberships` (composite unique constraint `uk_team_memberships_team_user`, FKs to teams and users)
- `work_items` (optimistic locking `@Version`, FKs to teams and users, indexes on status, team, and assignee)
- `activity_logs` (append-only audit log, composite index on `(work_item_id, created_at)`)

When `app.seed.enabled=true` (enabled by default in `application.properties`), the application automatically and idempotently seeds a core operational team, three demo operators with distinct roles, and four representative work items spanning the full lifecycle.

### Seeded Demo Identities & Team

| Operator | Email | Role in SRE Team | Operational Capabilities |
|---|---|---|---|
| **Alice Chen** | `alice.chen@newtonite.com` | `TEAM_LEAD` | Create, claim, release, complete, override/reassign any team work item, manage team roster |
| **Bob Martinez** | `bob.martinez@newtonite.com` | `MEMBER` | Create, claim open items, manage own assigned items (notes, priority, complete, release) |
| **Carol Danvers** | `carol.danvers@newtonite.com` | `MEMBER` | Create, claim open items, manage own assigned items |

**Operational Team:** `Site Reliability Engineering` (*Platform observability, incident response, and mission-critical production operations*).

### Seeded Representative Work Items

1. **`#1` Investigate latency spike on payment gateway** (Priority: `HIGH`, Status: `IN_PROGRESS`, Assignee: `Bob Martinez`)
   - Pre-loaded with full activity history: `CREATED` by Alice, `CLAIMED` by Bob, and a collaboration note (`NOTE_ADDED`) regarding connection pool starvation.
2. **`#2` Mitigate Kafka consumer lag on order-events topic** (Priority: `URGENT`, Status: `OPEN`, Assignee: `Unassigned`)
   - Ready for immediate claim in the **Unassigned Urgent** dashboard panel.
3. **`#3` Renew TLS wild-card certificates on edge ingress** (Priority: `MEDIUM`, Status: `DONE`, Resolved by: `Alice Chen`)
   - Completed incident demonstrating historical resolution and audit log.
4. **`#4` Optimize database query plans for activity audit log** (Priority: `LOW`, Status: `OPEN`, Assignee: `Unassigned`)
   - General backlog item available for triage.

### Configuring or Disabling Demo Seeding
To disable automatic seeding for staging, production, or benchmarking:
```bash
# Via Environment Variable
export APP_SEED_ENABLED=false

# Or via Spring Boot system property
java -Dapp.seed.enabled=false -jar target/operations-0.0.1-SNAPSHOT.jar
```
Seeding is strictly idempotent: restarting the server with seeding enabled will never duplicate records, reset existing user edits, or delete operational data.

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

---

## 3–5 Minute Evaluator Demo Walkthrough

Follow these steps to experience the complete Newtonite operational workflow:

1. **Launch Services:**
   - Ensure PostgreSQL is running on port 5432 (or run `docker compose up -d`).
   - Run backend: `java -jar target/operations-0.0.1-SNAPSHOT.jar`
   - Run frontend: `cd frontend && npm run dev` (opens at `http://localhost:5173`).

2. **Select Demo Identity:**
   - Navigate to `http://localhost:5173`.
   - The **"Who are you?"** operator picker displays the seeded operators.
   - Click **Select** on **Bob Martinez** (`bob.martinez@newtonite.com`, Member).

3. **Explore Dashboard Panels:**
   - **Assigned to Me:** Shows item `#1` (*Investigate latency spike* - `IN_PROGRESS`).
   - **Unassigned Urgent:** Shows item `#2` (*Mitigate Kafka consumer lag* - `URGENT`, `OPEN`).
   - **Awaiting My Approval:** Displays the honest architectural unavailable-state notice explaining that the backend schema enforces direct lifecycle transitions (`OPEN → IN_PROGRESS → DONE`) without multi-tier approval gates.

4. **One-Click Urgent Claim:**
   - In the **Unassigned Urgent** panel, click **Claim Work Item** on item `#2`.
   - TanStack Query invalidates cache; item `#2` instantly moves to Bob's **Assigned to Me** panel.

5. **Manage Work Item & Audit Timeline:**
   - Click into item `#1` (*Investigate latency spike*).
   - Scroll down to view the append-only **Activity History & Collaboration Notes** timeline (`CREATED`, `CLAIMED`, `NOTE_ADDED`).
   - Add a collaboration note: type *"Scaled up database connection pool to 50"* and submit.
   - Change Priority to `URGENT` via the priority dropdown.
   - Click **Mark as Completed** to transition the item to `DONE`.

6. **Demonstrate Role-Based Lead Privileges:**
   - Click the top-right profile avatar and switch identity to **Alice Chen** (`TEAM_LEAD`).
   - Navigate to **Teams** to view team members.
   - Open any item assigned to another member; as `TEAM_LEAD`, Alice can reassign or release items back to the queue, while regular members can only manage items assigned to themselves.

7. **Verify Optimistic Concurrency Conflict (HTTP 409):**
   - Open two browser tabs on the same open item `#4`.
   - In Tab 1, claim the item as Alice.
   - In Tab 2 (before refreshing), attempt to claim the same item as Bob.
   - Backend detects version mismatch / non-OPEN state and returns HTTP `409 CONFLICT`.
   - Frontend displays the prominent **ConflictBanner** alerting the operator and offering a one-click state reload.

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
- `GET /api/work-items?teamId={id}&status={status}` - Filter work items by team (server-side status filtering; priority and keyword search are evaluated client-side for zero-latency triage)
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

