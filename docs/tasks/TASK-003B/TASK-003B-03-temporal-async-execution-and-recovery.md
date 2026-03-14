# TASK-003B-03: Temporal Async Execution and Recovery

Status: Planned

Date: 2026-03-14

Related:

- [ADR-002: App Server Temporal Execution Model](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-002-app-server-temporal-execution-model.md)
- [TASK-003B: App Server Temporal Runtime](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003B-app-server-temporal-runtime.md)
- [TASK-003B-01: Runtime Coordination Persistence](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003B/TASK-003B-01-runtime-coordination-persistence.md)
- [TASK-003B-02: App Server Session Runtime](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003B/TASK-003B-02-app-server-session-runtime.md)

## Goal

Move the Codex execution workflow from a single blocking activity to a Temporal state machine that uses async activity completion, workflow messaging, and explicit reconciliation.

## Why This Next

The runtime session layer alone does not improve orchestration behavior unless the workflow can observe progress, wait for approvals, and recover stale executions.

## In Scope

- async activity completion for terminal turn lifecycle
- workflow Signals or Updates for coarse progress and approval state
- periodic health-check timers
- reconciliation activities for stale or unknown executions
- idempotent terminal completion handling

## Out of Scope

- raw provider event persistence
- full UI treatment of approvals
- later multi-agent routing behavior

## Work Sequence

1. Introduce the workflow state machine for app-server-backed turns.
2. Rework the start activity to hand off terminal completion asynchronously.
3. Add workflow message handlers for coarse progress and approval events.
4. Add periodic health checks and reconciliation activities.
5. Verify recovery behavior across runtime restarts and delayed completions.

## Verification

This task is complete when:

- the workflow can wait for terminal completion without a single blocking activity
- coarse progress and approval state are durable in workflow state
- stale executions can be reconciled explicitly
- terminal completion remains idempotent after retries or restarts
