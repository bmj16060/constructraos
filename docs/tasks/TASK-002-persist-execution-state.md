# TASK-002: Persist Execution State

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-001: Codex Invocation Vertical Slice](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-001-codex-invocation-vertical-slice.md)

## Goal

Persist the result of the Codex invocation flow so orchestration state is queryable outside Temporal.

## Why This Next

Once the workflow can invoke Codex and receive structured output, the next limitation is that Temporal is the only verification surface.

This milestone makes execution state durable and queryable through PostgreSQL.

## In Scope

- persistence schema for the first orchestration records
- shared entities and repositories in `libraries/persistence`
- activity-backed writes for execution results
- read path for task and execution status
- reuse of the current shared Flyway and repository pattern already used by the persisted hello-world history

## Out of Scope

- multi-agent routing
- human question resume flows
- workspace leasing
- review provider integration

## Records In Scope

- `projects`
- `tasks`
- `task_steps`
- `agent_sessions`
- `transcript_records`
- `task_step_results`

For this task, `task_steps` should be treated as workflow-managed steps, not as a claim that every step maps one-to-one with a single Codex turn.

Deferred unless needed immediately:

- `agent_turns` as a finer-grained record for individual Codex interactions within one workflow step

## Work Sequence

1. Define the minimum database schema for the first orchestration records.
2. Add Flyway migrations and shared persistence models.
3. Persist workflow execution results through orchestration activities.
4. Persist transcript payloads or transcript references for the first execution path.
5. Keep the schema compatible with optional later `agent_turns` without requiring that table in the first persistence slice.
6. Add read-side queries for task status and latest step result.
7. Verify that execution state can be read back without inspecting Temporal history.

## Verification

This task is complete when:

- a successful Codex invocation creates the expected database records
- task status can be queried from PostgreSQL-backed reads
- the structured result is durable outside Temporal
- transcript metadata is persisted for the first slice
- the persistence model does not force a one-step-equals-one-turn assumption
