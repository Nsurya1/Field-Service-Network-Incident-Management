# Field Service & Network Incident Management System

[![CI/CD Pipeline](https://github.com/nitesh/fieldservice/actions/workflows/ci.yml/badge.svg)](https://github.com/nitesh/fieldservice)
[![JaCoCo Instruction Coverage](https://img.shields.io/badge/Coverage-99.8%25-brightgreen.svg)](backend/target/site/jacoco/index.html)
[![JaCoCo Branch Coverage](https://img.shields.io/badge/Branches-98.3%25-brightgreen.svg)](backend/target/site/jacoco/index.html)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-4.x-38bdf8.svg)](https://tailwindcss.com)

A robust, enterprise-grade field service and network incident dispatch management system designed to monitor telecommunications/network infrastructure, orchestrate field technicians, enforce service level agreements (SLAs), and maintain an immutable audit trail.

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
                          │   • Proxies /api/* to Spring Boot   │
                          └──────────────────┬──────────────────┘
                                             │ HTTP :8081
                                             ▼
                          ┌─────────────────────────────────────┐
                          │      Spring Boot 4.x REST API       │
                          │   • Spring Security 7.x + JWT       │
                          │   • Role-Based Access Control       │
                          │   • State Machine Validation        │
                          │   • SLA Calculation Engine          │
                          │   • Workload Capacity Guardrails    │
                          └──────────────────┬──────────────────┘
                                             │ JDBC / HikariCP
                                             ▼
                          ┌─────────────────────────────────────┐
                          │         PostgreSQL 17 Database      │
                          │   • users, engineers, incident,     │
                          │     incident_history                │
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

### 4. Immutable Audit Trail & Event Timeline
Every lifecycle event (`CREATED`, `ASSIGNED`, `STATUS_CHANGED`) automatically records a chronological event log in `incident_history`, accessible via `GET /api/v1/incidents/{id}/history`.

### 5. High-Performance Dashboard Analytics
Aggregated metrics (`GET /api/v1/incidents/stats`) calculate total volume, status distribution, active tickets, and breached SLAs using database-level aggregate SQL queries.

### 6. Role-Based Access Control (RBAC) & Ownership Security
- **ROLE_ADMIN:** Full system access (create engineers, reassign incidents, manage users, view stats).
- **ROLE_MANAGER:** Team dispatch (create & assign incidents, view engineers and analytics).
- **ROLE_ENGINEER:** Field technician with ownership-based access control:
  - `GET /api/v1/incidents` returns only tickets assigned to the authenticated engineer.
  - `GET /api/v1/incidents/mine` provides direct access to the technician's active workload.
  - `PATCH /api/v1/incidents/{id}/status` strictly permits status progression on tickets assigned to them (rejects with **HTTP 403 Forbidden** on unassigned or peer tickets).
- **ROLE_VIEWER:** Read-only stakeholder access (view all incidents, event history, and stats; mutations rejected with **HTTP 403 Forbidden**).
- Password security enforced via `BCryptPasswordEncoder`.

---

## 🧪 Test Coverage & Quality Assurance

The codebase is hardened by **185 automated unit and integration tests** verified by JaCoCo:

- **Overall Instruction Coverage:** **>99.3%**
- **Overall Branch Coverage:** **>90.0%**
- **Integration Tests:** Tested against real PostgreSQL 17 test containers using Testcontainers, verifying Flyway schema migrations (V1, V2, V3), relational foreign keys, `@PrePersist` hooks, transactions, and security filter chains.

| Component / Layer | Instruction Coverage | Branch Coverage | Test Class |
| :--- | :---: | :---: | :--- |
| **Incident Service** | 100.00% | 100.00% | `IncidentServiceTest.java` |
| **Incident Entity** | 100.00% | 100.00% | `IncidentTest.java` |
| **Incident Controller** | 100.00% | 100.00% | `IncidentControllerTest.java` |
| **Auth Service & JWT** | 100.00% | 100.00% | `AuthServiceTest.java`, `JwtUtilsTest.java` |
| **Security Filters** | 100.00% | 100.00% | `JwtAuthenticationFilterTest.java` |
| **Engineer Service** | 100.00% | 100.00% | `EngineerServiceTest.java` |
| **Security Integration** | 100.00% | 100.00% | `SecurityIntegrationTest.java` |
| **Database Integration**| 100.00% | 100.00% | `IncidentIntegrationTest.java` |

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
| `GET` | `/api/v1/incidents/stats` | High-level metrics & SLA breaches | ADMIN, MANAGER, ENGINEER, VIEWER |
| `GET` | `/api/v1/incidents` | Filterable & paginated incident list | ADMIN, MANAGER, ENGINEER, VIEWER |
| `GET` | `/api/v1/incidents/mine` | Authenticated engineer's assigned tickets | ENGINEER |
| `GET` | `/api/v1/incidents/{id}` | Retrieve incident details | ADMIN, MANAGER, ENGINEER, VIEWER |
| `GET` | `/api/v1/incidents/{id}/history` | Chronological audit trail | ADMIN, MANAGER, ENGINEER, VIEWER |
| `POST` | `/api/v1/incidents` | Create a new network incident | ADMIN, MANAGER |
| `PATCH`| `/api/v1/incidents/{id}/assign` | Assign incident to field engineer | ADMIN, MANAGER |
| `PATCH`| `/api/v1/incidents/{id}/unassign` | Unassign incident from engineer (resets to OPEN) | ADMIN, MANAGER |
| `PATCH`| `/api/v1/incidents/{id}/status` | Advance status in lifecycle | ADMIN, MANAGER, ENGINEER |

### Engineer Endpoints (Secured)
| Method | Endpoint | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/engineers` | List all field engineers | ADMIN, MANAGER |
| `GET` | `/api/v1/engineers/{id}` | Retrieve engineer details | ADMIN, MANAGER |
| `POST` | `/api/v1/engineers` | Register a new technician | ADMIN |

---

## 🚀 Getting Started

### Prerequisites
- Java 21 JDK
- Node.js 20+ & npm
- PostgreSQL 16+ or Docker Desktop

---

### ⚙️ Environment Configuration & Profiles

All database credentials and cryptographic secrets have been eliminated from source control and are managed strictly through environment variables and Spring profiles.

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
   # The application will refuse to start up if missing or < 32 bytes in prod.
   APP_JWT_SECRET=replace_with_a_secure_random_hex_key_at_least_32_bytes_long_here
   APP_JWT_EXPIRATION_MS=86400000
   ```

3. **Spring Profiles:**
   - **`dev` (`application-dev.properties`):** Enables SQL formatting, development DDL updates, and safe default fallback keys for zero-friction local development.
   - **`prod` (`application-prod.properties`):** Enforces strict validation, turns off SQL echoing, and requires production environment variables. Startup strictly fails if `APP_JWT_SECRET` is missing or shorter than 32 bytes.
   - **`test` (`application-test.properties`):** Used during automated JUnit 5 / JaCoCo test execution against `fieldservice_test`.

---

### Option A: Running with Docker Compose (Recommended)

To start PostgreSQL, the Spring Boot Backend, and the React + Nginx frontend together:

```powershell
docker compose up --build
```

- **Frontend Application:** `http://localhost:3000`
- **Backend API:** `http://localhost:8081/api/v1`
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
.\mvnw.cmd clean verify
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
- **40 Realistic Network Incidents** across all statuses (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) and priorities (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), including active and resolved SLA breaches to populate dashboard analytics immediately.
- **Idempotency Guarantee**: Running the server multiple times detects existing records and safely skips without duplicating rows. Disabled in production and test profiles.

---

## 📂 Project Structure

```
FieldService/
├── .github/workflows/ci.yml       # GitHub Actions CI/CD Pipeline
├── docker-compose.yml             # Orchestration for DB, Backend & Frontend
├── README.md                      # Complete system documentation
├── backend/                       # Spring Boot 4.x REST API
│   ├── Dockerfile                 # Multi-stage Java 21 container
│   ├── pom.xml                    # Maven configuration & dependencies
│   └── src/
│       ├── main/java/com/sochi/fieldservice/
│       │   ├── common/            # Global exception handling & error models
│       │   ├── database/          # Connection health check endpoints
│       │   ├── engineer/          # Engineer entities, DTOs, service & controller
│       │   ├── health/            # Application health endpoints
│       │   ├── incident/          # Incident state machine, SLA engine & metrics
│       │   ├── security/          # Spring Security 7, JWT utils & filters
│       │   └── user/              # User entity & role definitions
│       └── test/java/             # Unit, MVC slice & PostgreSQL integration tests
└── frontend/                      # React 19 + Vite + Tailwind SPA
    ├── Dockerfile                 # Multi-stage Nginx container
    ├── nginx.conf                 # SPA routing & API reverse proxy
    ├── vite.config.js             # Vite configuration & dev proxy
    └── src/
        ├── components/            # Modals, tables, metrics & auth forms
        ├── context/               # Global authentication context
        └── services/              # Axios HTTP client with JWT interceptor
```
