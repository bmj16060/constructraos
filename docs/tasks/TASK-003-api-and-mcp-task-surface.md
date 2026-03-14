# TASK-003: API and MCP Task Surface

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-002: Persist Execution State](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-002-persist-execution-state.md)

## Goal

Expose the first operator-facing task surface through API and MCP boundaries.

## Why This Next

After invocation and persistence are real, the next step is a stable way to start work and inspect status without going directly to Temporal or the database.

## In Scope

- API endpoint to start a task
- API endpoint to read task status
- MCP tool surface that maps to those API capabilities
- project resolution based on current working directory at the entrypoint boundary
- session-aware and policy-checked task start behavior aligned with the current `HelloWorkflowController` pattern
- orchestration calls routed through typed workflow clients rather than direct API-to-worker coupling
- API handlers kept thin, with business logic and data access delegated to existing shared boundaries
- explicit handling of the `libraries/orchestration-clients` to `libraries/clients` rename if that boundary is broadened during this implementation pass

## Out of Scope

- human question handling
- transcript browsing tools
- multi-agent consultations
- review provider integration

## Work Sequence

1. Define minimal task start input and task status output contracts.
2. Decide whether to rename `libraries/orchestration-clients` to `libraries/clients` before extending that boundary, and execute the rename if the scope has broadened beyond orchestration-only clients.
3. Add or extend typed workflow client adapters for the new orchestration path.
4. Add API endpoints for task start and task status.
5. Apply session bootstrap and policy evaluation before start-side effects where the rule is reusable.
6. Wire those endpoints to the orchestration and persistence boundaries through the typed client and common read/query services rather than controller-local business or data access code.
7. Add the first MCP tools for task start and task status.
8. Verify the same task can be started and inspected through the intended operator path.

## Verification

This task is complete when:

- an operator can start a task through the intended boundary
- task status can be read without direct Temporal inspection
- project scoping is derived correctly from the working directory
- start-side policy checks follow the existing API and policy-service pattern
- API controllers remain thin and do not become the home of business or data access logic
- the typed client boundary naming is resolved deliberately rather than left as accidental legacy
- the API and MCP surfaces return consistent results
