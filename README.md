# Field Service & Network Incident Management System

[![CI/CD Pipeline](https://github.com/nitesh/fieldservice/actions/workflows/ci.yml/badge.svg)](https://github.com/nitesh/fieldservice)
[![JaCoCo Instruction Coverage](https://img.shields.io/badge/Coverage-98.2%25-brightgreen.svg)](backend/target/site/jacoco/index.html)
[![JaCoCo Branch Coverage](https://img.shields.io/badge/Branches-88.6%25-brightgreen.svg)](backend/target/site/jacoco/index.html)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-4.x-38bdf8.svg)](https://tailwindcss.com)

A robust, enterprise-grade field service and network incident dispatch management system designed to monitor telecommunications/network infrastructure, orchestrate field technicians, enforce service level agreements (SLAs), compute mean time to resolution (MTTR) analytics, and maintain an immutable audit trail.

---

## 🏛️ System Architecture

```
                          ┌─────────────────────────────────────┐
                          │         Web Browser / Client        │
                          └──────────────────┬──────────────────┘
                                             │ HTTP :3000
                                             ▼
                          ┌─────────────────────────────────────┐
                          │            Nginx (Alpine)           │
                          │   • Serves React 19 / Vite SPA      │
                          │   • Proxies /api/* & /actuator/*    │
                          │   • Enforces Security HTTP Headers  │
                          └──────────────────┬──────────────────┘
                                             │ HTTP :8081
                                             ▼
                          ┌─────────────────────────────────────┐
                          │      Spring Boot 4.x REST API       │
                          │   • Spring Security 7.x + JWT       │
                          │   • Role-Based Access Control       │
                          │   • State Machine Validation        │
                          │   • Caffeine High-Performance Cache │
                          │   • SLA Alert & Notification Engine │
                          │   • Micrometer & Prometheus Metrics │
                          └──────────────────┬──────────────────┘
                                             │ JDBC / HikariCP
                                             ▼
                          ┌─────────────────────────────────────┐
                          │         PostgreSQL 17 Database      │
                          │   • users, engineers, incident,     │
                          │     incident_history, visits,       │
                          │     comments, notifications         │
                          └─────────────────────────────────────┘
```

---

## ✨ Key Features & Domain Business Rules

### 1. Incident Lifecycle State Machine & Assignment Hardening
Prevents invalid transitions and unassigned progression through domain-encapsulated logic (`canTransitionTo`) and state guardrails:
- `OPEN` $\rightarrow$ `ASSIGNED`, `IN_PROGRESS` (requires assigned engineer), `CLOSED`
- `ASSIGNED` $\rightarrow$ `IN_PROGRESS`, `OPEN` (via unassign), `CLOSED`
- `IN_PROGRESS` $\rightarrow$ `RESOLVED`, `ASSIGNED`
- `RESOLVED` $\rightarrow$ `CLOSED`, `IN_PROGRESS` (re-open)
- `CLOSED` $\rightarrow$ Terminal state; cannot be modified or assigned.
- **Unassigned Guardrail:** Transitions to `IN_PROGRESS` and `RESOLVED` are strictly blocked (HTTP 400 Bad Request) if no field engineer is assigned to the incident.
- **Assignment Status Preservation:** Assigning an incident only advances status to `ASSIGNED` from `OPEN`; reassigning an `IN_PROGRESS` or `RESOLVED` ticket preserves its operational status rather than regressing.
- **Unassign Support (`PATCH /api/v1/incidents/{id}/unassign`):** Clears the assigned engineer, resets active incidents (`ASSIGNED` or `IN_PROGRESS`) back to `OPEN`, and appends an immutable `UNASSIGNED` audit history event.

### 2. SLA Deadlines & Real-Time Breach Tracking
Each priority level triggers an automatic Service Level Agreement (SLA) target deadline upon creation:
- **CRITICAL:** 4 Hours SLA window
- **HIGH:** 8 Hours SLA window
- **MEDIUM:** 24 Hours SLA window
- **LOW:** 72 Hours SLA window

If an active ticket exceeds its deadline without resolution, `isSlaBreached()` evaluates to `true` and the UI marks the ticket with a real-time breach alert.

### 3. Technician Workload Capacity & Concurrency Protection
Prevents engineer burnout and dispatch bottlenecks with strict concurrency control:
- Each field technician is capped at a maximum of **3 active incidents** (`ASSIGNED` or `IN_PROGRESS`) concurrently.
- **Pessimistic Write Locking (`SELECT ... FOR UPDATE`):** When checking technician capacity during assignment, `EngineerRepository.findByIdForUpdate()` serializes concurrent assignment transactions against the engineer row, eliminating race conditions where simultaneous requests could breach the workload cap.
- **Optimistic Locking (`@Version`):** `Incident` entities use a JPA `@Version` column (`version BIGINT NOT NULL DEFAULT 0`) to detect and reject concurrent lost-update conflicts (`OptimisticLockingFailureException`).
- Attempting to assign an engineer at capacity immediately rejects with **HTTP 409 Conflict**.

### 4. Field Technician Domain Model & Dispatch Guardrails
- **No Entity Leakage:** `EngineerController` returns encapsulated `EngineerResponse` DTOs containing `id`, `name`, `email`, `phone`, `specialization`, `active`, `availabilityStatus` (`AVAILABLE`, `BUSY`, `OFF_DUTY`), `currentWorkload`, and `createdAt`.
- **Batch Workload Computation:** Workload counts are aggregated in a single database grouping query (`countActiveIncidentsByEngineer`), eliminating N+1 query overhead.
- **Availability & Active Guardrails:** Inactive (`active = false`) or off-duty (`availabilityStatus = OFF_DUTY`) technicians are strictly blocked from dispatch assignment with **HTTP 400 Bad Request**.
- **Technician Profile & Status Management:** Authorized dispatchers and admins can modify technician specializations/availability (`PUT /api/v1/engineers/{id}`) and toggle active status (`PATCH /api/v1/engineers/{id}/active`).

### 5. Resolution Notes, Root Cause Analysis & On-Site Service Visits
- **Mandatory Resolution Notes:** Transitioning an incident to `RESOLVED` strictly requires `resolutionNotes` with at least 10 characters (HTTP 400 Bad Request if missing or too short) to prevent closing tickets without documenting root cause or repair actions.
- **Root Cause Categorization:** Incidents capture structured telecommunications root causes (`RootCause` enum: `HARDWARE_FAILURE`, `FIBER_CUT`, `POWER_OUTAGE`, `CONFIGURATION_ERROR`, `FIRMWARE_BUG`, `ENVIRONMENTAL`, `OPERATOR_ERROR`, `OTHER`).
- **On-Site Service Visits (`service_visits`):** Tracks field engineer visits with `incident_id`, `engineer_id`, `visit_date`, `check_in_at`, `check_out_at`, `work_notes`, and `resolution_notes`.
- **Service Visit Endpoints:**
  - `POST /api/v1/incidents/{id}/visits`: Log a visit with check-in/out times and work logs (ADMIN, MANAGER, and assigned ENGINEER).
  - `GET /api/v1/incidents/{id}/visits`: Retrieve chronological list of visits for an incident.

### 6. Comprehensive Audit Trail, Comments & Unified Activity Timeline
- **Actor Attribution (`performedBy`):** Every lifecycle event (`CREATED`, `ASSIGNED`, `STATUS_CHANGED`, `UNASSIGNED`, `COMMENT_ADDED`, `EDITED`) records the authenticated principal's username in `incident_history.performed_by`.
- **Collaborative Incident Comments (`incident_comments`):** Stakeholders can post discussion notes and progress logs (`POST /api/v1/incidents/{id}/comments`), with strict ownership enforcement (engineers can only comment on assigned tickets).
- **Unified Timeline Stream (`GET /api/v1/incidents/{id}/timeline`):** Merges audit history records, technician comments, and on-site service visits into a single, chronologically sorted activity timeline displayed dynamically in the React dashboard modal.

### 7. Operational Intelligence & MTTR Reporting (P13)
- **Database-Level MTTR Aggregations:** Computes Mean Time To Resolution in decimal hours via native PostgreSQL date arithmetic `EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600.0`.
- **SLA Compliance Percentage:** Computes overall compliance percentage directly in SQL without in-memory entity hydration.
- **Engineer Performance Leaderboard:** Ranks technicians by resolved ticket counts and average resolution velocity.
- **RFC-4180 CSV Export:** Direct streaming CSV download endpoint (`GET /api/v1/reports/export/csv`) with quote escaping and proper attachment headers.
- **Recharts Dashboard (`ReportsPage.jsx`):** Interactive dashboard featuring 7d/30d/90d presets, daily creation vs. resolution volume curves, priority distribution pie charts, and KPI summary widgets.

### 8. SLA Alerts & Notification Engine (P14)
- **Database Idempotence:** `notifications` table backed by a unique constraint `uq_notifications_incident_type UNIQUE (incident_id, type)` preventing duplicate alerts.
- **Alert Levels:**
  - `WARNING`: Dispatched when an active incident crosses 80% of its SLA window.
  - `BREACHED`: Dispatched immediately when current time reaches or exceeds the SLA deadline.
- **Background Scheduler:** Periodic `@Scheduled` scanner evaluates unresolved tickets every minute and dispatches notifications automatically.
- **Navbar Notification Center:** Real-time Bell icon with unread count badge, auto-polling every 30s, rich alert dropdown with priority badges, and one-click navigation to ticket details.

### 9. High-Performance Caching & Query Optimization (P15)
- **Caffeine In-Memory Cache:** Configured via `CaffeineCacheManager` with size capping (max 500 items), 5-minute write expiration, and Micrometer statistics recording.
- **Cache Eviction Strategy:**
  - `incidentStats`: Populated on dashboard load; evicted on incident creation, status transition, assignment, or detail edit.
  - `engineerList`: Populated on dispatch lookups; evicted on technician updates, availability changes, or ticket assignments.
  - `reportSummary`: Populated on date-range analytical queries; evicted on incident state mutations.
- **N+1 Prevention:** Batched active incident queries and `@EntityGraph(attributePaths = {"assignedEngineer"})` eager loading for incident search and pagination.

### 10. Observability, Metrics & Actuator (P16)
- **Spring Boot Actuator:** Production endpoints exposed under `/actuator` (`health`, `info`, `metrics`, `prometheus`).
- **Custom Health Indicator (`IncidentSystemHealthIndicator`):** Reports system health status (`OPERATIONAL` or `UNAVAILABLE`), active ticket count, active SLA breaches, and technician headcount.
- **Micrometer & Prometheus Metrics:**
  - `incident.active.gauge`: Real-time gauge of open/active tickets.
  - `incident.breached.gauge`: Real-time gauge of active tickets past SLA deadline.
  - `incident.created.total`: Counter tagged by `priority`.
  - `incident.resolved.total`: Counter tagged by `priority`.
  - `sla.alert.generated.total`: Counter tagged by alert `type` (`WARNING`, `BREACHED`).
- **Granular Security:** Public health check probes (`/actuator/health`, `/actuator/info`) for load balancers; strictly authenticated Admin access for Prometheus scraping (`/actuator/prometheus`) and metrics.

### 11. Role-Based Access Control (RBAC) & Ownership Security
- **ROLE_ADMIN:** Full administrative access (create engineers, reassign incidents, manage users, view all metrics, Prometheus scraping).
- **ROLE_MANAGER:** Team dispatch (create & assign incidents, edit incidents, view engineers, access reports and analytics).
- **ROLE_ENGINEER:** Field technician with ownership-based access control:
  - `GET /api/v1/incidents` returns only tickets assigned to the authenticated engineer.
  - `GET /api/v1/incidents/mine` provides direct access to the technician's active workload.
  - `PATCH /api/v1/incidents/{id}/status` strictly permits status progression on tickets assigned to them (rejects with **HTTP 403 Forbidden** on unassigned or peer tickets).
  - `POST /api/v1/incidents/{id}/visits` permits logging service visits for tickets assigned to them.
- **ROLE_VIEWER:** Read-only stakeholder access (view all incidents, service visits, event history, reports, and stats; mutations rejected with **HTTP 403 Forbidden**).
- Password security enforced via `BCryptPasswordEncoder`.

---

## 🧪 Test Coverage & Quality Assurance

The codebase is hardened by **337 automated unit and integration tests** verified by JaCoCo:

- **Overall Instruction Coverage:** **>98.2%**
- **Overall Branch Coverage:** **>88.6%**
- **Integration Tests:** Tested against real PostgreSQL 17 test containers using Testcontainers, verifying Flyway schema migrations (V1 through V8), relational foreign keys, JPA Specifications, pessimistic/optimistic locks, Caffeine caching, Actuator endpoints, and security filter chains.

| Component / Layer | Instruction Coverage | Branch Coverage | Test Class |
| :--- | :---: | :---: | :--- |
| **Incident Service** | 98.4% | 88.2% | `IncidentServiceTest.java` |
| **Incident Entity** | 100.0% | 100.0% | `IncidentTest.java` |
| **Incident Controller** | 100.0% | 100.0% | `IncidentControllerTest.java` |
| **Report Service** | 100.0% | 90.0% | `ReportServiceTest.java` |
| **Report Controller** | 100.0% | 100.0% | `ReportControllerTest.java` |
| **SLA Alert Engine** | 100.0% | 92.8% | `SlaAlertServiceTest.java`, `SlaAlertSchedulerTest.java` |
| **Notification Service & Controller** | 100.0% | 100.0% | `NotificationServiceTest.java`, `NotificationControllerTest.java` |
| **Observability & Metrics** | 100.0% | 100.0% | `BusinessMetricsServiceTest.java`, `IncidentSystemHealthIndicatorTest.java` |
| **Auth Service & JWT** | 100.0% | 100.0% | `AuthServiceTest.java`, `JwtUtilsTest.java` |
| **Security Filters** | 100.0% | 100.0% | `JwtAuthenticationFilterTest.java` |
| **Engineer Service** | 100.0% | 100.0% | `EngineerServiceTest.java` |
| **Security Integration** | 100.0% | 100.0% | `SecurityIntegrationTest.java` |
| **Database Integration**| 100.0% | 100.0% | `IncidentIntegrationTest.java`, `ReportIntegrationTest.java`, `NotificationIntegrationTest.java` |
| **Caching Integration** | 100.0% | 100.0% | `CachingIntegrationTest.java` |
| **Observability Integration** | 100.0% | 100.0% | `ObservabilityIntegrationTest.java` |

---

## 📡 REST API Reference

### Authentication Endpoints (Public)
| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register new user account | Public |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain JWT token | Public |

### System & Health Endpoints (Public)
| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/health` | Application status & health | Public |
| `GET` | `/api/v1` | System version & information | Public |
| `GET` | `/api/v1/database-check` | Database connectivity probe | Public |

### Incident Endpoints (Secured)
| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/incidents/stats` | High-level metrics & SLA breaches (Cached) | ADMIN, MANAGER, ENGINEER, VIEWER |
| `GET` | `/api/v1/incidents` | Specification search & multi-attribute filter list (`search`, `status`, `priority`, `assignedEngineerId`, `createdFrom`, `createdTo`, `breachedOnly`, `sort`) | ADMIN, MANAGER, ENGINEER, VIEWER |
| `GET` | `/api/v1/incidents/mine` | Authenticated engineer's assigned tickets with specification filters | ENGINEER |
| `GET` | `/api/v1/incidents/{id}` | Retrieve incident details | ADMIN, MANAGER, ENGINEER, VIEWER |
| `PUT` | `/api/v1/incidents/{id}` | Edit title, description, location, priority (priority change recalculates SLA from `createdAt` & records audit history) | ADMIN, MANAGER |
| `GET` | `/api/v1/incidents/{id}/history` | Chronological audit trail | ADMIN, MANAGER, ENGINEER, VIEWER |
| `POST` | `/api/v1/incidents` | Create a new network incident | ADMIN, MANAGER |
| `PATCH`| `/api/v1/incidents/{id}/assign` | Assign incident to field engineer | ADMIN, MANAGER |
| `PATCH`| `/api/v1/incidents/{id}/unassign` | Unassign incident from engineer (resets to OPEN) | ADMIN, MANAGER |
| `PATCH`| `/api/v1/incidents/{id}/status` | Advance status in lifecycle (requires resolution notes if RESOLVED) | ADMIN, MANAGER, ENGINEER |
| `GET` | `/api/v1/incidents/{id}/visits` | Retrieve all on-site service visits for incident | ADMIN, MANAGER, ENGINEER, VIEWER |
| `POST` | `/api/v1/incidents/{id}/visits` | Log an on-site service visit | ADMIN, MANAGER, ENGINEER |
| `GET` | `/api/v1/incidents/{id}/comments` | Retrieve comments for incident | ADMIN, MANAGER, ENGINEER, VIEWER |
| `POST` | `/api/v1/incidents/{id}/comments` | Post a comment on incident | ADMIN, MANAGER, ENGINEER |
| `GET` | `/api/v1/incidents/{id}/timeline` | Retrieve merged chronological activity timeline | ADMIN, MANAGER, ENGINEER, VIEWER |

### Engineer Endpoints (Secured)
| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/engineers` | List all field engineers with batched current workload (Cached) | ADMIN, MANAGER |
| `GET` | `/api/v1/engineers/{id}` | Retrieve engineer details with current workload | ADMIN, MANAGER |
| `POST` | `/api/v1/engineers` | Register a new technician | ADMIN |
| `PUT` | `/api/v1/engineers/{id}` | Update engineer profile & availability status | ADMIN, MANAGER |
| `PATCH`| `/api/v1/engineers/{id}/active` | Toggle engineer active status | ADMIN, MANAGER |

### Reports & Operational Intelligence Endpoints (Secured)
| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/reports/summary` | Retrieve MTTR, SLA compliance %, priority breakdown, daily series, and engineer performance (Cached) | ADMIN, MANAGER, VIEWER |
| `GET` | `/api/v1/reports/export/csv` | Download RFC-4180 compliant CSV export of incidents within date range | ADMIN, MANAGER, VIEWER |

### SLA Notification Endpoints (Secured)
| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/notifications` | List all SLA alerts ordered by creation date | ADMIN, MANAGER, ENGINEER, VIEWER |
| `GET` | `/api/v1/notifications/unread-count` | Retrieve unread notification count | ADMIN, MANAGER, ENGINEER, VIEWER |
| `PATCH`| `/api/v1/notifications/{id}/read` | Mark a specific notification as read | ADMIN, MANAGER, ENGINEER, VIEWER |
| `PATCH`| `/api/v1/notifications/read-all` | Mark all unread notifications as read | ADMIN, MANAGER, ENGINEER, VIEWER |

### Observability & Actuator Endpoints
| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/actuator/health` | Application & database readiness/liveness status | Public |
| `GET` | `/actuator/info` | Application environment and build metadata | Public |
| `GET` | `/actuator/metrics` | Micrometer metrics inventory | ADMIN |
| `GET` | `/actuator/prometheus` | Prometheus exposition format metrics for scraping | ADMIN |

---

## 🚀 Getting Started

### Prerequisites
- Java 21 JDK
- Node.js 20+ & npm
- PostgreSQL 16+ or Docker Desktop

---

### ⚙️ Environment Configuration & Profiles

All database credentials and cryptographic secrets are managed through environment variables and Spring profiles.

1. **Copy the Environment Template:**
   ```powershell
   cp .env.example .env
   ```

2. **Configure your secrets in `.env`:**
   ```env
   # Database Configuration
   POSTGRES_DB=fieldservice
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=your_secure_db_password
   POSTGRES_PORT=5432

   # Spring Datasource Bindings
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fieldservice
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=your_secure_db_password

   # Active Profile: 'dev' (local development) or 'prod' (production deployment)
   SPRING_PROFILES_ACTIVE=dev

   # JWT Secret: In 'prod', this MUST be at least 32 bytes (256 bits).
   APP_JWT_SECRET=replace_with_a_secure_random_hex_key_at_least_32_bytes_long_here
   APP_JWT_EXPIRATION_MS=86400000
   ```

3. **Spring Profiles:**
   - **`dev` (`application-dev.properties`):** Enables SQL formatting, development DDL updates, and automatic sample data seeding for zero-friction local development.
   - **`prod` (`application-prod.properties`):** Enforces strict validation, turns off SQL echoing, requires production environment variables, enables Caffeine caching, and configures production Actuator endpoints.
   - **`test` (`application-test.properties`):** Used during automated JUnit 5 / JaCoCo test execution against isolated PostgreSQL test containers.

---

### Option A: Running with Docker Compose (Production Hardened)

To start PostgreSQL 17, the Spring Boot Backend (JRE 21 non-root container), and the React + Nginx frontend together:

```powershell
docker compose up --build
```

- **Frontend Application:** `http://localhost:3000`
- **Backend API:** `http://localhost:8081/api/v1`
- **Actuator Health Check:** `http://localhost:8081/actuator/health`
- **PostgreSQL Database:** `localhost:5432` (database: `fieldservice`)

---

### Option B: Running Locally

#### 1. Start Backend

Ensure your local PostgreSQL database `fieldservice` is running on port 5432.

```powershell
cd D:\projects\FieldService\backend
.\mvnw.cmd clean spring-boot:run
```

#### 2. Start Frontend

```powershell
cd D:\projects\FieldService\frontend
npm.cmd run dev
```

Visit `http://localhost:3000` in your browser.

---

### 3. Running Automated Tests

To execute the full test suite and regenerate the JaCoCo coverage report:

```powershell
cd D:\projects\FieldService\backend
.\mvnw.cmd test jacoco:report jacoco:check
```

View the generated HTML coverage report at:
`backend/target/site/jacoco/index.html`

---

## 👤 Sample Login Accounts & Demo Seed Data

In the development profile (`dev`), the application includes an idempotent `DataSeeder` (`CommandLineRunner`) that automatically seeds:
- **4 Core User Accounts** (with default password `password123`, overridable via `APP_SEED_PASSWORD`):
  | Username | Default Password | Role | Description |
  | :--- | :--- | :--- | :--- |
  | `admin` | `password123` | `ROLE_ADMIN` | Full administrative capabilities |
  | `dispatcher` | `password123` | `ROLE_MANAGER` | Dispatch and ticket assignment |
  | `technician` | `password123` | `ROLE_ENGINEER` | Field ticket resolution (linked to field engineer profile) |
  | `viewer` | `password123` | `ROLE_VIEWER` | Read-only inspection of incidents and analytics |
- **8 Field Engineers** across telecommunications specializations (`Fibre`, `Router`, `Wireless`, and `Power`).
- **40 Realistic Network Incidents** across all statuses (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) and priorities (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), including active and resolved SLA breaches.
- **Idempotency Guarantee**: Running the server multiple times detects existing records and safely skips without duplicating rows. Disabled in production and test profiles.

---

## 📂 Project Structure

```
FieldService/
├── docker-compose.yml             # Complete container orchestration (Postgres, Backend, Frontend)
├── README.md                      # Comprehensive system documentation
├── backend/                       # Spring Boot 4.x REST API
│   ├── Dockerfile                 # Multi-stage Java 21 JRE non-root container
│   ├── pom.xml                    # Maven configuration, JaCoCo, Caffeine, Actuator
│   └── src/
│       ├── main/java/com/sochi/fieldservice/
│       │   ├── common/            # Global exception handling & error models
│       │   ├── config/            # CacheConfig (Caffeine), SchedulingConfig, DataSeeder, Flyway
│       │   ├── database/          # Connection health check endpoints
│       │   ├── engineer/          # Engineer entities, DTOs, service & controller
│       │   ├── incident/          # Incident state machine, SLA engine, JPA Specifications
│       │   ├── notification/      # SLA Alert Engine, Scheduler, Notification entity & service
│       │   ├── observability/     # Custom HealthIndicator & Micrometer Prometheus metrics
│       │   ├── report/            # MTTR analytics, database aggregations & CSV export
│       │   ├── security/          # Spring Security 7, JWT utils & filters
│       │   ├── user/              # User entity & role definitions
│       │   └── visit/             # On-site service visit logging
│       └── test/java/             # 337 unit, MVC slice & PostgreSQL integration tests
└── frontend/                      # React 19 + Vite + Tailwind SPA
    ├── Dockerfile                 # Multi-stage Nginx container with security headers
    ├── nginx.conf                 # SPA routing & API/Actuator reverse proxy
    ├── vite.config.js             # Vite configuration & dev proxy
    └── src/
        ├── components/            # Modals, tables, metrics, ReportsPage, Navbar alert center
        ├── context/               # Global authentication context
        └── services/              # Axios HTTP clients (incident, report, notification)
```
