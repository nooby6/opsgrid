# OpsGrid

OpsGrid is an enterprise-oriented workforce scheduling backend built with Kotlin and Spring Boot.

The project focuses on operational scheduling problems rather than generic CRUD: temporal conflict detection, transactional consistency, auditability, background work, event-driven integration and eventually workload optimisation.

## Current milestone

Milestone 2 — Scheduling consistency (in progress)

Implemented:
- Kotlin + Spring Boot
- Java 21
- PostgreSQL
- Flyway database migrations
- Docker Compose development database
- Employee and shift domains
- REST API foundation
- Transactional shift scheduling
- Service-layer overlap checks for clear API errors
- PostgreSQL exclusion constraints for overlapping shifts and overlapping approved leave
- Cross-table trigger guards prevent a scheduled shift from overlapping approved leave (and vice versa), using a transaction-scoped per-employee advisory lock
- Concurrent PostgreSQL integration tests prove same-table and cross-table overlapping scheduling writes cannot both commit
- Audit events for shift creation, updates, cancellation, leave approval and leave cancellation
- Before/after state snapshots stored as JSONB, with actor and timestamp
- Audit writes participate in the same transaction as the business mutation
- Stateless JWT resource-server authentication with role-based authorization for employee, shift and leave mutations
- Shift and leave audit actors come from the validated JWT subject rather than a caller-controlled header
- Validation and API error handling, including PostgreSQL exclusion violations mapped to stable HTTP 409 responses
- Unit and integration tests
- GitHub Actions CI

## Core scheduling rule

A scheduled shift conflicts with another scheduled shift for the same employee when:

existing.start < requested.end AND existing.end > requested.start

Touching shifts are valid. Overlapping windows are rejected.

## API

Create an employee with `POST /api/v1/employees`.

Schedule a shift with `POST /api/v1/shifts`. Update and cancel shifts with `PUT /api/v1/shifts/{id}` and `DELETE /api/v1/shifts/{id}`.

Create or cancel leave with `POST /api/v1/leave` and `DELETE /api/v1/leave/{id}`.

All API routes require a valid bearer access token from an OpenID Connect provider. Configure `JWT_ISSUER_URI` and `JWT_JWK_SET_URI`; the defaults point to a local Keycloak realm placeholder and do not provision an identity provider. Shift and leave audit records use the authenticated token's `sub` claim. The API no longer trusts a caller-supplied `X-Actor-Id` header.

Role claims are read from either a top-level `roles` array or Keycloak's `realm_access.roles` array. Required roles:
- `POST /api/v1/employees`: `ADMIN` or `HR`
- Create, update and cancel shifts: `ADMIN` or `SCHEDULER`
- Create and cancel leave: `ADMIN` or `HR`
- Read endpoints: any authenticated user

The API is stateless and validates JWT signatures using the configured JWK set, plus the configured issuer. Role names are mapped to Spring authorities such as `ROLE_SCHEDULER`. This repository configures the resource server; you must still provision an identity provider, users, clients, and role assignments in your deployment.

A scheduling conflict returns HTTP 409 with code `SHIFT_CONFLICT`; invalid time windows return HTTP 400.

## Run locally

Start PostgreSQL:

    docker compose up -d postgres

Run the application:

    gradle bootRun

The API starts on port 8080.

## Testing

    gradle test

The service checks provide a useful conflict response, but they are not the concurrency boundary. PostgreSQL enforces the invariant with GiST exclusion constraints using `tstzrange(..., '[)')`; the half-open range allows adjacent shifts while rejecting overlaps. Migration `V3__schedule_consistency.sql` applies these constraints to scheduled shifts and approved leave. `ConcurrentShiftConflictIntegrationTest` synchronizes two transactions and asserts exactly one insert commits.

Migration `V4__audit_events.sql` adds the audit event table. Migration `V5__prevent_shift_leave_overlap.sql` closes the cross-table race between shift creation/update and leave approval by serializing each employee's scheduling writes inside PostgreSQL and checking the opposite table before commit. Audit writes use the same transaction as each shift or leave mutation, so a failed mutation or failed audit write rolls back the whole operation. Integration tests verify that behaviour.

## Roadmap

### Milestone 2 — Scheduling Engine
- shift update and cancellation
- employee availability
- leave and time-off
- department constraints
- schedule queries
- expand scheduling constraints to cover employee availability and department rules

### Milestone 3 — Workforce Operations
- shift swaps
- approval workflows
- overtime tracking
- workload calculations

### Milestone 4 — Distributed Processing
- Redis
- Kafka
- domain events
- asynchronous notifications
- background jobs
- idempotent consumers

### Milestone 5 — Optimisation
- conflict-free schedule generation
- workload balancing
- overtime minimisation
- availability-aware assignment
- scheduling constraints

### Milestone 6 — Production Engineering
- provision and operate the OIDC identity provider, clients, users, role assignments and key rotation for each deployment
- observability
- metrics
- tracing
- load testing
- Testcontainers integration tests
- container hardening
- Kubernetes deployment

## Engineering goals

OpsGrid is deliberately being built as a backend engineering project. The target is a system where correctness matters: schedules affect people, conflicts have operational consequences, and distributed workflows must remain consistent when requests are retried or services fail.

The project favours explicit domain rules, transactional boundaries, meaningful tests and operational visibility over framework-heavy abstractions. The database is the final authority for overlap prevention; service-layer checks alone are insufficient under concurrent requests.
