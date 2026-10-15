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
- PostgreSQL exclusion constraints for scheduled shifts and approved leave
- Concurrent PostgreSQL integration test: two overlapping inserts yield exactly one commit and one `23P01` exclusion violation
- Validation and API error handling
- Unit and integration tests
- GitHub Actions CI

## Core scheduling rule

A scheduled shift conflicts with another scheduled shift for the same employee when:

existing.start < requested.end AND existing.end > requested.start

Touching shifts are valid. Overlapping windows are rejected.

## API

Create an employee with POST /api/v1/employees.

Schedule a shift with POST /api/v1/shifts.

A scheduling conflict returns HTTP 409 with code SHIFT_CONFLICT.

## Run locally

Start PostgreSQL:

    docker compose up -d postgres

Run the application:

    gradle bootRun

The API starts on port 8080.

## Testing

    gradle test

The service checks provide a useful conflict response, but they are not the concurrency boundary. PostgreSQL enforces the invariant with GiST exclusion constraints using `tstzrange(..., '[)')`; the half-open range allows adjacent shifts while rejecting overlaps. Migration `V3__schedule_consistency.sql` applies these constraints to scheduled shifts and approved leave. `ConcurrentShiftConflictIntegrationTest` synchronizes two transactions and asserts exactly one insert commits.

## Roadmap

### Milestone 2 — Scheduling Engine
- shift update and cancellation
- employee availability
- leave and time-off
- department constraints
- schedule queries
- map PostgreSQL exclusion violations to HTTP 409 consistently for create and update paths
- extend integration coverage to concurrent updates and leave/shift interactions

### Milestone 3 — Workforce Operations
- shift swaps
- approval workflows
- overtime tracking
- workload calculations
- audit log

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
- authentication and RBAC
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
