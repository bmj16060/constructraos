# ADR-002: App Server Temporal Execution Model

Status: Accepted

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [Codex App Server Temporal Execution State Machine](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/designs/codex-app-server-temporal-execution-state-machine.md)
- [TASK-003B: App Server Temporal Runtime](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003B-app-server-temporal-runtime.md)

## Context

ConstructraOS currently uses a dedicated `codex-runtime` service, but the runtime still starts a fresh `codex exec` process for each execution turn.

That shape preserves the original structured-result contract, but it underuses the capabilities now available through Codex app-server:

- persistent threads
- streamed execution events
- first-class approval prompts
- lower per-turn startup overhead
- richer recovery options after runtime restart

The earlier design note explored a Temporal-centric state machine for this path. The repo now needs a durable architectural decision for how app-server, Temporal, PostgreSQL, and Valkey interact.

## Decision

ConstructraOS will adopt an app-server-backed execution model with the following boundaries:

1. Temporal workflows remain the orchestration authority.
2. The runtime boundary moves from one-shot `codex exec` execution toward long-lived app-server thread and turn sessions.
3. Temporal async activity completion is the terminal contract for a single external Codex turn.
4. Workflow Signals or Updates carry coarse intermediate execution state such as approvals, plan updates, diff snapshots, and checkpointed progress.
5. PostgreSQL remains the durable source of truth for both:
   - user-facing task projection data, and
   - runtime coordination data needed for recovery.
6. Valkey may be used as an optional acceleration layer for heartbeat freshness, hot execution indexes, and short-lived lease hints, but not as the sole durability contract.
7. `codex-runtime` must not become the owner of task or transcript projection tables.
8. Recovery must assume persistent app-server thread history is recoverable, but exact mid-stream continuation of an in-flight turn is not guaranteed.

## Workflow and Runtime Split

### Temporal workflow responsibilities

- own execution states and transitions
- own timeout, retry, and reconciliation logic
- own approval wait states
- own final success and failure semantics
- decide when to retry, recover, or fail a stale execution

### Runtime responsibilities

- own the live app-server process and transport session
- start or resume provider threads and turns
- collect provider events
- surface normalized execution updates back to orchestration
- accept approval and interrupt commands from orchestration

### Persistence responsibilities

#### Task projection data

Owned by orchestration through shared persistence libraries:

- task status
- task step status
- operator-visible approval state
- transcript projection
- final structured result payload

#### Runtime coordination data

Owned by a separate shared persistence boundary:

- runtime execution identity
- provider thread and turn identity
- owner instance and lease metadata
- last durable checkpoint
- approval wait state
- reconciliation inputs

This keeps recovery metadata available without giving the runtime service ownership of business state.

## Temporal Pattern Decision

The preferred Temporal pattern is hybrid:

- use async activity completion for the terminal lifecycle of one external Codex turn
- use Signals or Updates for coarse intermediate state
- use timers plus health-check and reconcile activities for stale execution detection

Rejected alternatives:

- a single blocking activity for the whole turn
  - too little visibility and poor recovery semantics
- raw provider event streaming into workflow history
  - too much workflow history growth
- runtime-owned task persistence
  - wrong ownership boundary
- Valkey-only execution coordination
  - insufficient durability for recovery-critical state

## Recovery Model

The system must assume runtime instances can die or restart.

Recovery therefore depends on:

- durable runtime coordination records in PostgreSQL
- coarse normalized checkpointing
- explicit health-check and reconcile loops in Temporal
- idempotent or restartable task-step execution where practical

The system should not assume exact reattachment to an in-flight provider stream after runtime process death.

## Consequences

Positive:

- better use of app-server capabilities
- clearer separation between orchestration, runtime, and persistence ownership
- better liveness and recovery semantics
- room for richer operator-visible progress without making Temporal the read model

Negative:

- more moving parts than the current blocking adapter
- new runtime coordination schema and lease logic
- more workflow messages and reconciliation paths to manage

## Implementation Direction

Implementation should proceed through a parent task and child task breakdown covering:

- runtime coordination persistence
- app-server session runtime in `codex-runtime`
- Temporal async execution, signaling, and reconciliation
- task projection and approval surface updates
