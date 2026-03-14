# Codex App Server Temporal Execution State Machine

Date: 2026-03-14

Related:

- [ADR-002: App Server Temporal Execution Model](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-002-app-server-temporal-execution-model.md)
- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-001A: Containerized Codex Runtime Boundary](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-001A-containerized-codex-runtime-boundary.md)
- [TASK-002: Persist Execution State](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-002-persist-execution-state.md)
- [TASK-007: Implementation Execution Loop](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-007-implementation-execution-loop.md)
- [Codex App Server](https://developers.openai.com/codex/app-server/)
- [`codex-rs/app-server` README](https://github.com/openai/codex/tree/main/codex-rs/app-server)

## Why This Note Exists

ConstructraOS currently invokes Codex through a dedicated runtime service that still starts a fresh `codex exec` process for each task turn.

That shape preserves the existing structured-result contract, but it leaves useful app-server capabilities on the table:

- persistent threads instead of one-shot turns
- streamed execution events
- first-class approval flows
- lower per-turn startup overhead
- richer recovery options after runtime restarts

This note proposes a Temporal-centric execution model that uses app-server more fully without moving task ownership into the runtime service.

## Problem

The current execution path is shaped like a single blocking activity:

1. workflow invokes one Codex execution activity
2. activity calls the runtime adapter
3. adapter returns one final structured result
4. persistence records the result and transcript after completion

That model is simple, but it assumes:

- no meaningful intermediate execution state
- no need to surface approvals while work is in flight
- no need to recover a partially observed execution stream
- no need to distinguish runtime coordination data from user-facing task projection data

Moving to app-server only at the transport layer would reduce startup cost, but it would not change those limitations.

## Goals

- use app-server persistent threads and per-turn `outputSchema`
- keep Temporal as the orchestration authority
- keep user-facing task state queryable in PostgreSQL
- allow streamed progress and approval events without waiting for final completion
- recover cleanly from `codex-runtime` crashes or restarts
- avoid putting task/business persistence ownership into the runtime layer

## Non-Goals

- model every raw app-server event in Temporal workflow history
- make `codex-runtime` the source of truth for tasks, steps, or transcripts
- guarantee exact mid-token or mid-delta replay after runtime process death
- replace the current API and MCP task surfaces during the first pass

## Design Summary

The recommended model is a hybrid:

1. Temporal workflow owns the task state machine.
2. A start activity launches or resumes an app-server thread/turn and returns using Temporal async activity completion.
3. `codex-runtime` keeps the live app-server session and streams coarse normalized events back into the workflow via Signals or Updates.
4. Orchestration-owned activities persist the user-facing task projection in PostgreSQL.
5. A separate coordination persistence boundary stores runtime lease and recovery metadata.
6. A reconcile loop detects stale executions and decides whether to recover, complete idempotently, or fail/retry.

This keeps Temporal in control while using app-server as a long-lived execution substrate instead of a one-shot CLI wrapper.

## Layer Boundaries

### Workflow Boundary

Temporal workflows own:

- execution state transitions
- timeout and retry policy
- approval wait states
- recovery and reconciliation decisions
- final success or failure semantics

Temporal should not own:

- raw JSON event logs
- token-level deltas
- live transport session management

### Runtime Boundary

`codex-runtime` owns:

- app-server process lifecycle
- live thread and turn sessions
- app-server event subscription
- approval request transport
- lease heartbeat updates
- delivery of normalized events back to orchestration

`codex-runtime` should not own:

- `tasks`
- `task_steps`
- `task_step_results`
- transcript projection rows used by API and UI

### Persistence Boundary

PostgreSQL use is split into two kinds of data.

#### Task projection data

Owned by orchestration through shared persistence libraries:

- task status
- task step status
- approval state visible to operators
- transcript projection
- plan snapshots and diff snapshots if exposed to API/UI
- final structured result payload

This is the data read by the API, MCP tools, and UI.

#### Runtime coordination data

Owned by a separate coordination boundary:

- `runtime_execution_id`
- workflow id
- provider thread id
- provider turn id
- owner instance id
- lease expiration / heartbeat timestamps
- execution checkpoint metadata
- coarse runtime state

This data exists for crash recovery and liveness, not as the operator-facing task model.

The important rule is that both kinds of data may live in the same PostgreSQL server, but they must not share ownership semantics.

### Valkey Role

Valkey can still help, but it should be treated as an optimization layer rather than the recovery contract.

Good Valkey candidates:

- short-lived heartbeat freshness markers
- expiring lease hints
- hot indexes of currently running executions
- lightweight fan-out or notification coordination
- debounce state for health-check activity polling

Data that must survive runtime restarts or cache loss should stay in PostgreSQL:

- durable execution ownership
- provider thread and turn identity
- last durable checkpoint
- approval wait state
- reconciliation inputs
- terminal completion state when known

Recommended split:

- PostgreSQL is the durable source of truth for both task projection data and runtime coordination data.
- Valkey is an optional acceleration layer used to make liveness checks and active-execution lookups cheaper.
- Temporal remains the authority for orchestration decisions such as retry, timeout, escalation, and reconciliation.

This matches the existing repo pattern where Valkey is helpful but optional rather than a required durability layer.

## Proposed Runtime Coordination Model

Add a dedicated shared persistence boundary for runtime coordination, for example:

- `libraries/persistence/runtimecoordination`

Possible normalized tables:

- `runtime_executions`
- `runtime_execution_leases`
- `runtime_execution_checkpoints`

Example `runtime_executions` fields:

- `id`
- `workflow_id`
- `task_id`
- `task_step_id`
- `provider_thread_id`
- `provider_turn_id`
- `owner_instance_id`
- `state`
- `awaiting_approval`
- `current_request_id`
- `last_event_at`
- `last_heartbeat_at`
- `lease_expires_at`
- `started_at`
- `completed_at`
- `failure_reason`

This model gives recovery logic a durable place to look without forcing runtime-private concerns into the task projection tables.

## Temporal Model

### Why Temporal Async Activity Completion Helps

There are three Temporal patterns that matter here:

1. normal async workflow calls with `Promise`
2. workflow Signals and Updates
3. async activity completion using a task token

For this design, the key pattern is async activity completion.

The start activity should:

1. create or update a runtime coordination record
2. store the Temporal task token
3. tell `codex-runtime` to start or resume the app-server thread/turn
4. call `doNotCompleteOnReturn()`
5. return immediately

Later, when the runtime reaches a terminal state, it completes the activity asynchronously with the final structured result or terminal failure.

This gives Temporal one durable pending external operation for the turn lifecycle.

### Why Signals and Updates Still Matter

Async activity completion only finishes once. It is not a streaming mechanism.

Use Signals or Updates for coarse intermediate state such as:

- turn started
- plan updated
- approval requested
- approval resolved
- diff snapshot updated
- item completed
- stale execution detected

Use Updates when the caller needs a synchronous acknowledgement from the workflow, for example:

- operator approval or rejection
- runtime claim of a stale execution
- explicit interrupt or cancel

### Workflow Wait Loop

The workflow should wait on:

- final async activity completion
- incoming Signals or Updates
- a periodic timer used for health checks and reconciliation

This yields a durable state machine instead of a single blocking activity.

## Proposed Workflow States

- `STARTING`
- `RUNNING`
- `WAITING_FOR_APPROVAL`
- `RECONCILING`
- `COMPLETED`
- `FAILED`
- `CANCELLED`

Expected transitions:

1. `STARTING -> RUNNING`
2. `RUNNING -> WAITING_FOR_APPROVAL`
3. `WAITING_FOR_APPROVAL -> RUNNING`
4. `RUNNING -> RECONCILING`
5. `WAITING_FOR_APPROVAL -> RECONCILING`
6. `RUNNING -> COMPLETED`
7. `WAITING_FOR_APPROVAL -> FAILED`
8. any active state -> `CANCELLED`

## Health Check and Reconcile Loop

The workflow should not wait forever for runtime callbacks.

Instead, it should periodically wake and run a health-check activity against the runtime coordination store.

Example loop:

1. start or resume external turn
2. wait for event or timer
3. if timer fires, run `CheckExecutionHealthActivity`
4. health check may read Valkey first for fast liveness and then fall back to PostgreSQL for durable recovery state
5. if state is stale or unknown, run `ReconcileExecutionActivity`
6. if reconciliation finds a terminal provider result, complete idempotently
7. if reconciliation cannot recover safely, fail or retry according to workflow policy

This gives Temporal an explicit reconciliation path after runtime restarts.

## Recovery Expectations

The design should assume:

- persistent app-server thread history can be read after restart
- exact live-stream continuation of an in-flight turn is not guaranteed
- terminal completion may be recoverable even if the original runtime instance dies
- some in-flight turns may need to be retried from a checkpoint or failed explicitly

Implications:

- do not rely on ephemeral app-server threads when recovery matters
- keep prompts and task-step execution idempotent where practical
- persist approval wait state outside the live runtime process
- treat runtime heartbeats and lease ownership as recoverable coordination signals, not as business state

## Event Normalization

Do not push raw app-server deltas into workflow history.

Normalize them into coarse workflow events and persist raw detail outside Temporal when needed.

Recommended signal payload classes:

- `ExecutionStarted`
- `PlanUpdated`
- `ApprovalRequested`
- `ApprovalResolved`
- `DiffUpdated`
- `ExecutionCheckpointed`
- `ExecutionFailed`

Raw app-server events and JSONL-like transcripts can still be stored for inspection, but those should land in task transcript storage or a dedicated raw event log, not in workflow history.

## Approval Flow

Approvals should be modeled as workflow state, not only runtime transport.

Recommended shape:

1. runtime receives an app-server approval request
2. runtime stores coordination checkpoint and sends `ApprovalRequested`
3. workflow moves to `WAITING_FOR_APPROVAL`
4. operator response enters through API or MCP
5. workflow accepts the response through an Update or Signal
6. an activity forwards that decision to runtime
7. runtime resumes the app-server turn
8. workflow moves back to `RUNNING`

This keeps approval state durable even if the runtime instance dies after surfacing the prompt.

## Why Not Let `codex-runtime` Write Task Tables Directly

That would blur the ownership boundary between:

- external execution coordination
- user-facing orchestration state

If `codex-runtime` writes task tables directly:

- orchestration and runtime become co-owners of the same state
- recovery semantics become unclear
- API-visible data begins depending on transport-layer behavior
- the repo violates its current persistence pattern, which keeps shared data access in reusable library boundaries

The runtime may write coordination records, but orchestration should remain the writer of task projection data.

## Migration Path

The design can land incrementally.

### Phase 1

- add the design-level boundaries
- add runtime coordination persistence
- add provider thread id / turn id tracking
- keep the current blocking execution contract as fallback

### Phase 2

- add app-server-backed runtime sessions
- add async activity completion for terminal turn results
- add coarse workflow Signals for progress and approvals

### Phase 3

- add health-check and reconciliation activities
- add stale lease takeover rules
- add API and UI surfaces for approval and richer task progress

## Consequences

Positive:

- better use of app-server capabilities
- lower fixed execution overhead
- more visible task progress
- clearer recovery story for runtime restarts
- cleaner separation between coordination storage and task projection storage

Negative:

- more moving parts than the current blocking adapter
- new runtime coordination schema and lease logic
- more workflow message traffic
- additional care needed to avoid Temporal history growth

## Open Questions

- whether approval responses should enter through Signals or workflow Updates by default
- whether raw provider events belong in `transcript_records` or a separate event-log table
- whether `agent_turns` should be added before or alongside this change
- how much plan and diff state should be promoted into first-class query surfaces during the first pass
- whether the first recovery path should support only idempotent restart, or also attempt provider-side terminal-state discovery before retrying

## Recommended Next Step

This note now feeds:

- [ADR-002: App Server Temporal Execution Model](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-002-app-server-temporal-execution-model.md)
- [TASK-003B: App Server Temporal Runtime](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003B-app-server-temporal-runtime.md)
