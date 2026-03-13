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

## Decision

ConstructraOS will implement Codex orchestration with the following boundaries:

1. Temporal workflows own the coordinator logic.
2. Temporal activities own side effects, including Codex CLI execution via the sidecar.
3. PostgreSQL is the system of record for projects, tasks, agent sessions, transcript records, and human questions.
4. The persistence model uses normalized relational tables for workflow-driving entities and `jsonb` columns for flexible payloads and transcript detail.
5. The active project is resolved from the interactive caller's working directory and mapped to a durable project record.
6. The first implementation slice is a single-agent path:
   `start_task -> run one Codex agent turn -> persist result and transcript -> route to next task state`

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
- load task and session context from persistence
- invoke `codex exec` or `codex exec resume`
- capture transcript output and structured result
- persist transcript records, session IDs, and task-step results
- answer query-oriented reads needed by API or MCP tools

This keeps non-deterministic Codex process execution out of workflow code.

## Persistence Model

The first implementation will normalize the orchestration backbone and use `jsonb` where payload shape is expected to vary.

Normalized tables:

- `projects`
- `tasks`
- `task_steps`
- `agent_sessions`
- `human_questions`

Tables with mixed relational metadata plus `jsonb` payloads:

- `transcript_records`
- `task_step_results`

Expected table roles:

- `projects`
  - durable project identity
  - unique root path
  - human-readable name derived from the directory or explicit input
- `tasks`
  - project-scoped orchestration unit
  - goal, status, requested agent, requested by, timestamps
- `task_steps`
  - one row per atomic agent turn
  - agent role, turn number, status, linked session, timing metadata
- `agent_sessions`
  - project-scoped Codex session continuity by task and agent role
- `human_questions`
  - blocked-work coordination and operator answers
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
- execute Codex through an activity-backed sidecar
- persist the structured result
- persist transcript metadata and transcript payload
- persist or update the agent session ID
- route the task into `completed`, `blocked`, or `ready_for_next_agent`

Deferred until after the first slice works:

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
  - activities that execute the Codex sidecar and persistence operations
- `libraries/persistence`
  - entities, repositories, and Flyway migrations for orchestration records

If Codex execution later needs separate operational isolation, the activity worker can be split into a dedicated service without changing the workflow contract.

## Consequences

Positive:

- aligns the implementation with existing Temporal and persistence seams
- keeps workflow logic deterministic
- makes projects, tasks, sessions, and transcripts queryable through PostgreSQL
- preserves a clean side-effect boundary around Codex execution
- delivers a narrow first slice without blocking on every future orchestration feature

Tradeoffs:

- transcript payloads in PostgreSQL may need a later storage split if volume grows
- persistence design must be deliberate up front because project scoping and session continuity are part of the platform contract
- the first slice will intentionally leave consultation and resume loops incomplete until the single-agent path is stable

## Implementation Order

1. Add the orchestration persistence schema and repositories in `libraries/persistence`.
2. Define workflow input/output contracts and structured result models shared between API and orchestration.
3. Implement the first coordinator workflow for a single-agent task.
4. Implement Codex sidecar execution as an orchestration activity.
5. Expose task start and task status through API and MCP boundaries.
6. Verify the end-to-end path against a real local Codex invocation.

## Verification Expectation

The first implementation pass is complete when:

- a task can be started for a project resolved from the working directory
- the workflow runs one agent turn through the Codex sidecar activity
- the database contains the project, task, task step, session, transcript, and structured result records
- the API or MCP layer can read back task status without inspecting Temporal history directly
