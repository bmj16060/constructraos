# TASK-004: Workspace Leasing

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-003: API and MCP Task Surface](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003-api-and-mcp-task-surface.md)

## Goal

Introduce isolated execution workspaces for write-capable agent work.

## Why This Next

The system can tolerate read-only concurrency earlier, but write-capable work needs explicit isolation before multi-agent development flows are safe.

## In Scope

- workspace persistence model
- workspace lease lifecycle
- link between task steps and workspaces
- activity support for acquiring and releasing workspaces

## Out of Scope

- provider-backed review flows
- merge automation
- advanced workspace cleanup policies

## Work Sequence

1. Define the `workspaces` record shape and lease states.
2. Add persistence and repository support for workspace records.
3. Implement activity-backed workspace acquisition for write-capable work.
4. Record workspace linkage on task steps.
5. Verify that concurrent write-capable work does not share the same mutable workspace.

## Verification

This task is complete when:

- write-capable work receives an isolated workspace lease
- workspace assignment is visible through durable records
- task steps can be linked to the workspace they ran in
- the system prevents unsafe concurrent write access to one workspace
