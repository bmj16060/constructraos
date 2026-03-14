# TASK-003A: App Server Temporal Runtime

Status: In Progress

Date: 2026-03-14

Related:

- [ADR-002: App Server Temporal Execution Model](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-002-app-server-temporal-execution-model.md)
- [Codex App Server Temporal Execution State Machine](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/designs/codex-app-server-temporal-execution-state-machine.md)
- [TASK-001A: Containerized Codex Runtime Boundary](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-001A-containerized-codex-runtime-boundary.md)
- [TASK-002: Persist Execution State](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-002-persist-execution-state.md)
- [TASK-003: API and MCP Task Surface](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003-api-and-mcp-task-surface.md)

## Goal

Replace the current one-shot `codex exec` task execution path with an app-server-backed Temporal execution model that supports streaming progress, approval waits, and recovery after runtime restart without moving task ownership into the runtime service.

## Why This Next

The current runtime boundary works, but it still pays per-turn CLI startup cost and collapses execution into a single final result.

The accepted follow-on architecture now uses app-server threads and a Temporal-centric state machine with:

- async activity completion for terminal turn lifecycle
- workflow Signals or Updates for coarse progress
- explicit health-check and reconciliation loops
- a separate runtime coordination persistence boundary

## In Scope

- parent-level execution plan for the app-server temporal runtime
- delivery order for the first implementation slices
- child tasks that break the work into buildable milestones

## Out of Scope

- unrelated UI work
- review provider integrations
- autonomous multi-agent planning beyond the runtime needed for this slice

## Child Tasks

1. [TASK-003A-01: Runtime Coordination Persistence](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003A/TASK-003A-01-runtime-coordination-persistence.md)
2. [TASK-003A-02: App Server Session Runtime](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003A/TASK-003A-02-app-server-session-runtime.md)
3. [TASK-003A-03: Temporal Async Execution and Recovery](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003A/TASK-003A-03-temporal-async-execution-and-recovery.md)
4. [TASK-003A-04: Task Projection and Approval Surface](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003A/TASK-003A-04-task-projection-and-approval-surface.md)

## Work Sequence

1. Add the runtime coordination persistence boundary before changing execution semantics.
2. Add an app-server-backed session runtime in `codex-runtime` while preserving a controlled fallback path.
3. Rework the Temporal execution slice to use async activity completion, workflow messaging, and reconciliation.
4. Expose the resulting progress and approval state through the existing task projection surfaces.

## Verification

This parent task is complete when:

- each child task is complete
- the compose-served stack can run an app-server-backed task turn end to end
- runtime restarts can be reconciled through the documented recovery model
- task progress and approval waits are inspectable outside Temporal history

## Progress

- [TASK-003A-01: Runtime Coordination Persistence](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003A/TASK-003A-01-runtime-coordination-persistence.md) is complete.
- The current blocking execution path remains in place during the transition, including the `CODEX_RUNTIME_MODE=cli` fallback for the older direct approach.
