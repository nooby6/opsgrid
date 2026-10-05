# OpsGrid

OpsGrid is an enterprise-oriented workforce scheduling backend built with Kotlin and Spring Boot.

The project focuses on operational scheduling problems rather than generic CRUD: temporal conflict detection, transactional consistency, auditability, background work, event-driven integration and eventually workload optimisation.

## Current milestone

Milestone 1 — Domain & Infrastructure

Implemented:
- Kotlin + Spring Boot
- Java 21
- PostgreSQL
- Flyway database migrations
- Docker Compose development database
- Employee and shift domains
- REST API foundation
- Transactional shift scheduling
- Overlap conflict detection
- Validation and API error handling
- Unit tests
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

The current suite covers the core scheduling rule at the service boundary. CI runs the test suite against PostgreSQL.

## Roadmap

### Milestone 2 — Scheduling Engine
- shift update and cancellation
- employee availability
- leave and time-off
- department constraints
- schedule queries
- stronger database-level consistency

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

The project favours explicit domain rules, transactional boundaries, meaningful tests and operational visibility over framework-heavy abstractions.
