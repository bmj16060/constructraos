# TASK-003B-04: Task Projection and Approval Surface

Status: Planned

Date: 2026-03-14

Related:

- [ADR-002: App Server Temporal Execution Model](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-002-app-server-temporal-execution-model.md)
- [TASK-003B: App Server Temporal Runtime](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003B-app-server-temporal-runtime.md)
- [TASK-003: API and MCP Task Surface](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003-api-and-mcp-task-surface.md)
- [TASK-003A: Task Console UI](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003A-task-console-ui.md)

## Goal

Expose the richer app-server-backed execution state through the existing task projection model so operators can inspect progress and respond to approval waits without relying on Temporal history.

## Why This Next

The workflow and runtime changes are only useful to operators if the resulting state is queryable through API, MCP, and later UI surfaces.

## In Scope

- projection updates for coarse execution progress
- projection updates for approval wait states
- API and MCP read or write hooks needed for approval response flows
- compatibility with later task console UI work

## Out of Scope

- final UI polish
- full multi-agent visualization
- external review provider integration

## Work Sequence

1. Define the minimum projection fields needed for progress and approval state.
2. Persist those fields through orchestration-owned activities.
3. Extend API and MCP task surfaces where required.
4. Verify operator-visible state can be inspected and acted on without Temporal history inspection.

## Verification

This task is complete when:

- task reads can show coarse in-flight progress
- blocked approval state is queryable outside Temporal
- approval responses can flow back into orchestration through supported surfaces
- the projection remains owned by orchestration rather than the runtime layer
