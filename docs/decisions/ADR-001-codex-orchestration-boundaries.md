# ADR-001: Codex Orchestration Boundaries

Status: Accepted

Date: 2026-03-13

Related:

- [Codex Agent Team Orchestration Architecture](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/designs/codex-agent-team-orchestration-architecture.md)
- [TASK-001: Codex Invocation Vertical Slice](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-001-codex-invocation-vertical-slice.md)

## Context

ConstructraOS now has a target architecture for project-aware Codex agent orchestration, but it still needs durable boundary decisions that fit the current platform seams.

The repo already has the right baseline components for this work:

- `api-service` as the external HTTP and MCP boundary
- `orchestration` as the Temporal worker/runtime boundary
- `libraries/commons` for common workflow, activity, and model contracts
- `libraries/clients` for typed workflow client adapters used by the API boundary
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
6. Detailed execution sequencing and milestone breakdown live in task documents rather than in the ADR.
7. Planning artifacts such as design notes, ADR proposals, and task breakdowns are intended system outputs and may eventually be produced through a planner-oriented orchestration path.
8. Deterministic business rules and authorization decisions should flow through the policy boundary, not be embedded ad hoc in Java orchestration code.
9. `api-service` remains a thin boundary; business logic and data access should live in workflow, policy, `libraries/commons`, and `libraries/persistence` boundaries rather than in API controllers.

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

In this model, a Codex interaction is an atomic worker turn, but a higher-level workflow step may still span multiple turns when follow-up execution, consultation, or human input is required before that step is complete.

When workflow progression depends on a deterministic rule, that rule should be evaluated through the existing policy boundary instead of being reimplemented as opaque Java branching where a reusable policy can express it.

## Policy Boundary

ConstructraOS already has a policy-service and OPA seam. Codex orchestration should use that seam deliberately.

Expected split:

- Codex execution
  - non-deterministic reasoning
  - implementation proposals
  - review analysis
  - planning artifact generation
- Policy evaluation
  - authorization
  - deterministic business rules
  - reusable gating criteria
  - promotion rules that are policy-shaped rather than procedural

Consequences for implementation:

- workflows may call policy evaluation through dedicated activities
- reusable business rules should be expressed in policy where practical
- Java services should orchestrate policy requests and apply results, not become the canonical home of deterministic rule logic
- Codex output may inform a decision path, but Codex should not be treated as the authority for deterministic business policy

## Planning Artifact Model

The current repo process turns a concept into:

- a design document when the shape is still exploratory
- an ADR when architectural boundaries become durable
- task documents when the work can be sequenced

This should be treated as a product capability, not only as a manual authoring habit.

The system should eventually support a planner-oriented path that can:

- accept a concept or requested change
- synthesize or update a design note
- propose or revise an ADR
- generate or revise task documents

Human approval remains the control point for accepting those artifacts, but the generation and maintenance of planning artifacts is part of the intended orchestration surface.

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
- `agent_turns`
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
  - one row per workflow-managed step in the orchestration process
  - a step may include one or more atomic Codex turns over time
  - agent role, step number, status, linked workspace, linked session, timing metadata
- `agent_turns`
  - optional finer-grained execution record when individual Codex turns need to be modeled separately from higher-level task steps
  - turn number, linked task step, session linkage, execution metadata
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
  - relational keys for project/task/step/turn/session lookup
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

## Service Placement

The initial implementation should reuse existing repo boundaries instead of introducing new deployables.

Initial placement:

- `api-service`
  - start-task and status/query endpoints
  - session bootstrap and policy checks before side effects
  - MCP tools that call those boundaries
  - no long-term business or data access logic beyond boundary coordination
- `libraries/commons`
  - common workflow interfaces
  - activity interfaces
  - common orchestration models
- `libraries/clients`
  - typed workflow client adapters used by `api-service`
- `services/orchestration`
  - Temporal workflow implementations
  - activities that call the Codex execution adapter and persistence operations
- `libraries/persistence`
  - entities, repositories, and Flyway migrations for orchestration records
  - common query services used by boundary layers instead of controller-local data access

Preferred near-term direction:

- define a stable adapter contract first
- keep the first implementation compatible with a sidecar-hosted Codex CLI
- allow that adapter to move behind a wrapper service API later without changing workflow semantics
- extend the existing shared-contract and typed-client pattern instead of coupling `api-service` directly to Temporal implementation details

## Consequences

Positive:

- aligns the implementation with existing Temporal and persistence seams
- keeps workflow logic deterministic
- makes projects, tasks, sessions, and transcripts queryable through PostgreSQL
- prevents unsafe concurrent writes by making workspace isolation explicit
- keeps GitHub integration optional at the domain layer even if GitHub is the first provider used
- preserves a clean side-effect boundary around Codex execution without overcommitting on transport or hosting
- recognizes planning and work decomposition as a first-class system capability rather than an external manual process
- preserves the existing ConstructraOS policy model by keeping deterministic rules behind OPA and policy-service
- delivers a narrow first slice without blocking on every future orchestration feature

Tradeoffs:

- transcript payloads in PostgreSQL may need a later storage split if volume grows
- persistence design must be deliberate up front because project scoping and session continuity are part of the platform contract
- workspace leasing and cleanup introduce additional operational state to manage
- review abstractions add domain complexity before non-GitHub providers exist, but they avoid locking the orchestration model to one vendor
- the execution adapter contract needs to be designed carefully enough that both a sidecar and a wrapper-service implementation remain viable
- planner-generated docs will need review and acceptance rules so the system does not silently redefine its own roadmap
- some orchestration decisions will require deliberate judgment about whether they are durable policy candidates or merely local control flow
