# TASK-003A-01: Runtime Coordination Persistence

Status: Planned

Date: 2026-03-14

Related:

- [ADR-002: App Server Temporal Execution Model](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-002-app-server-temporal-execution-model.md)
- [TASK-003A: App Server Temporal Runtime](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003A-app-server-temporal-runtime.md)
- [TASK-002: Persist Execution State](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-002-persist-execution-state.md)

## Goal

Add a dedicated persistence boundary for runtime coordination data without making `codex-runtime` a writer of task projection tables.

## Why This Next

Recovery and reconciliation need durable execution ownership, provider thread identity, and lease metadata before the runtime and Temporal layers can safely change behavior.

## In Scope

- shared persistence models and repositories for runtime coordination
- Flyway migrations for runtime coordination tables
- clear separation from task projection tables
- optional Valkey usage guidelines for hot execution indexes and heartbeat freshness

## Out of Scope

- live app-server session management
- workflow signaling changes
- operator-facing approval UI

## Work Sequence

1. Define the runtime coordination schema.
2. Add a shared persistence package and repositories.
3. Define the coordination read and write interfaces consumed by orchestration and runtime.
4. Keep the boundary separate from `libraries/persistence/tasks`.
5. Verify recovery-critical fields are durable without relying on Valkey.

## Verification

This task is complete when:

- runtime coordination records can be created and updated durably
- the runtime coordination boundary is separate from task projection persistence
- recovery-critical state survives process and cache restarts
