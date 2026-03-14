# ADR-001: Codex Orchestration Implementation Plan

Status: Accepted

Date: 2026-03-13

Related:

- [Codex Agent Team Orchestration Architecture](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/designs/codex-agent-team-orchestration-architecture.md)

## Context

ConstructraOS now has a target architecture for project-aware Codex agent orchestration, but it still needs an implementation plan that fits the current platform seams.

The repo already has the right baseline components for this work:

- `api-service` as the external HTTP and MCP boundary
- `orchestration` as the Temporal worker/runtime boundary
- `libraries/persistence` for shared schema and repositories
- PostgreSQL as the durable database

The implementation needs to preserve Temporal determinism, keep Codex CLI execution behind a side-effect boundary, and make projects, tasks, sessions, and transcripts queryable through the normal persistence model.

The exact runtime boundary for Codex execution is still intentionally open. The current preference is to host the Codex CLI behind a sidecar-style boundary, but that boundary may end up being implemented as a small wrapper service with an API rather than a direct process invocation inside the worker.

## Decision

ConstructraOS will implement Codex orchestration with the following boundaries:

1. Temporal workflows own the coordinator logic.
2. Temporal activities own side effects, including calls to a Codex execution adapter.
3. PostgreSQL is the system of record for projects, workspaces, tasks, agent sessions, transcript records, human questions, and change/review records.
4. The persistence model uses normalized relational tables for workflow-driving entities and `jsonb` columns for flexible payloads and transcript detail.
5. The active project is resolved from the interactive caller's working directory and mapped to a durable project record.
6. The first implementation slice is a single-agent path:
   `start_task -> run one Codex agent turn -> persist result and transcript -> route to next task state`

For this ADR, "Codex execution adapter" means the boundary that accepts an execution request and returns:

- structured agent output
- session identity or continuity metadata
- transcript payload or transcript reference
- execution status and timing metadata

The adapter may be backed by:

- a sidecar-local process wrapper around the Codex CLI
- a dedicated Codex wrapper service with an API
- another equivalent boundary that preserves the same contract

## Workflow and Activity Split

The coordinator described in the design doc maps directly to a Temporal workflow.

Workflow responsibilities:

- accept task start input
- resolve the current execution step
- choose the next agent to run
- invoke the Codex execution activity
- evaluate the returned structured result
- update durable task state through activity calls
- wait when human input is required

Activity responsibilities:

- resolve or create the project record for a working directory
- acquire or resolve an execution workspace when the task can write code
- load task, workspace, and session context from persistence
- call the Codex execution adapter
- capture transcript output and structured result
- persist transcript records, session IDs, workspace state, and task-step results
- answer query-oriented reads needed by API or MCP tools

This keeps non-deterministic Codex execution out of workflow code while avoiding an early commitment to a specific hosting model.

## Workspace and Review Model

Concurrent write access to a single mutable checkout is not a safe orchestration primitive.

ConstructraOS will therefore treat execution workspaces as first-class records rather than assuming multiple agents can share one writable worktree.

Rules:

- read-only work may run concurrently against the same project state
- write-capable work requires an isolated workspace lease
- two write-capable agents must not mutate the same workspace concurrently
- review work happens against a durable change boundary, not a live writable checkout

This ADR intentionally separates execution isolation from review transport:

- `workspace`
  - isolated checkout or worktree used by a single write-capable agent execution path
- `change_set`
  - the proposed source change produced from a workspace, usually represented by base/head revisions and branch metadata
- `review_target`
  - the stable artifact another agent reviews
- `review_system_reference`
  - provider-specific linkage to GitHub or another external review system

GitHub is the expected first provider, but the domain model should stay provider-neutral so the workflow is not hard-wired to pull requests as the only review object.

## Persistence Model

The first implementation will normalize the orchestration backbone and use `jsonb` where payload shape is expected to vary.

Normalized tables:

- `projects`
- `workspaces`
- `tasks`
- `task_steps`
- `agent_sessions`
- `human_questions`
- `change_sets`
- `review_targets`
- `review_system_references`

Tables with mixed relational metadata plus `jsonb` payloads:

- `transcript_records`
- `task_step_results`

Expected table roles:

- `projects`
  - durable project identity
  - unique root path
  - human-readable name derived from the directory or explicit input
- `workspaces`
  - project-scoped execution isolation record
  - workspace path, lease status, branch metadata, base revision, cleanup state
- `tasks`
  - project-scoped orchestration unit
  - goal, status, requested agent, requested by, timestamps
- `task_steps`
  - one row per atomic agent turn
  - agent role, turn number, status, linked workspace, linked session, timing metadata
- `agent_sessions`
  - project-scoped Codex session continuity by task and agent role
- `human_questions`
  - blocked-work coordination and operator answers
- `change_sets`
  - stable record of a proposed source change
  - source workspace, branch, base revision, head revision, status
- `review_targets`
  - provider-neutral reviewable unit linked to a change set
  - review status, reviewer assignment, verification summary
- `review_system_references`
  - provider binding such as GitHub pull request metadata
  - external IDs, URLs, provider type, synchronization metadata
- `transcript_records`
  - relational keys for project/task/step/session lookup
  - raw transcript event stream stored in `jsonb`
- `task_step_results`
  - schema-validated structured result payload stored in `jsonb`

Rule of thumb:

- if a field drives orchestration, routing, joins, uniqueness, or operator filtering, it should be relational
- if a field is large, variable, or mainly for inspection/debugging, it should be `jsonb`

## Project Resolution

The MCP-facing entrypoint will treat the caller's current working directory as the active project key.

Project resolution rules:

1. Look up an existing `projects` row by root path.
2. Create one if none exists.
3. Scope all subsequent task, session, transcript, and question records to that project.

This preserves the project-aware behavior from the design doc without requiring project selection in every prompt.

## First Vertical Slice

The first delivered slice is intentionally narrow.

Included:

- start a project-scoped task
- run one planner or implementer turn through a Temporal workflow
- assign an isolated workspace for write-capable work
- execute one agent turn through the Codex execution adapter
- persist the structured result
- persist transcript metadata and transcript payload
- persist or update the agent session ID
- route the task into `completed`, `blocked`, or `ready_for_next_agent`

Deferred until after the first slice works:

- provider-backed review flows such as GitHub pull requests
- multi-agent consultations
- human question answer/resume loop
- transcript summarization
- transcript storage outside PostgreSQL
- UI for orchestration state
- advanced retry and escalation policies

## Service Placement

The initial implementation should reuse existing repo boundaries instead of introducing new deployables.

Initial placement:

- `api-service`
  - start-task and status/query endpoints
  - MCP tools that call those boundaries
- `services/orchestration`
  - Temporal workflow implementations
  - activities that call the Codex execution adapter and persistence operations
- `libraries/persistence`
  - entities, repositories, and Flyway migrations for orchestration records

Preferred near-term direction:

- define a stable adapter contract first
- keep the first implementation compatible with a sidecar-hosted Codex CLI
- allow that adapter to move behind a wrapper service API later without changing workflow semantics

## Consequences

Positive:

- aligns the implementation with existing Temporal and persistence seams
- keeps workflow logic deterministic
- makes projects, tasks, sessions, and transcripts queryable through PostgreSQL
- prevents unsafe concurrent writes by making workspace isolation explicit
- keeps GitHub integration optional at the domain layer even if GitHub is the first provider used
- preserves a clean side-effect boundary around Codex execution without overcommitting on transport or hosting
- delivers a narrow first slice without blocking on every future orchestration feature

Tradeoffs:

- transcript payloads in PostgreSQL may need a later storage split if volume grows
- persistence design must be deliberate up front because project scoping and session continuity are part of the platform contract
- workspace leasing and cleanup introduce additional operational state to manage
- review abstractions add domain complexity before non-GitHub providers exist, but they avoid locking the orchestration model to one vendor
- the execution adapter contract needs to be designed carefully enough that both a sidecar and a wrapper-service implementation remain viable
- the first slice will intentionally leave consultation and resume loops incomplete until the single-agent path is stable

## Implementation Order

1. Add the orchestration persistence schema and repositories in `libraries/persistence`.
2. Define workflow input/output contracts, workspace leasing rules, and structured result models shared between API and orchestration.
3. Implement the first coordinator workflow for a single-agent task.
4. Define and implement the first Codex execution adapter behind an orchestration activity.
5. Expose task start and task status through API and MCP boundaries.
6. Add provider-neutral change/review records before wiring the first GitHub-backed review flow.
7. Verify the end-to-end path against a real local Codex invocation.

## Verification Expectation

The first implementation pass is complete when:

- a task can be started for a project resolved from the working directory
- write-capable work receives an isolated workspace record rather than sharing a mutable checkout
- the workflow runs one agent turn through the Codex execution adapter
- the database contains the project, task, task step, session, transcript, and structured result records
- the API or MCP layer can read back task status without inspecting Temporal history directly
