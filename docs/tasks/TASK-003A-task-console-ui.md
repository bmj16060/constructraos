# TASK-003A: Task Console UI

Status: Planned

Date: 2026-03-14

Related:

- [TASK-003: API and MCP Task Surface](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003-api-and-mcp-task-surface.md)
- [TASK-002: Persist Execution State](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-002-persist-execution-state.md)

## Goal

Add a basic operator-facing UI page for task execution without replacing the current landing page.

## Why This Next

Once task start and status APIs exist, the next useful operator surface is a simple UI page that proves the persisted task model can be used from the browser.

This should extend the current shell, not replace it.

## In Scope

- a new secondary route in the existing UI shell, such as `/tasks`
- a visible link from the current shell, ideally in or near the existing stack shortcuts
- a simple task start form backed by the intended API
- a recent task list backed by the intended status/read API
- display of task status, latest step summary, and key timestamps
- preserve the current landing page and keep it as the default entry point

## Out of Scope

- replacing the current landing page
- complex dashboard layout or multi-pane task management
- transcript browsing UI
- human question handling UI
- review workflow UI

## Work Sequence

1. Confirm the minimal API contracts from TASK-003 that the UI will call.
2. Add frontend routing support while keeping the current home page intact.
3. Add a navigation entry from the existing shell to the new task page.
4. Build the basic task start and recent task status UI using the existing fetch and TanStack Query patterns.
5. Keep styling aligned with the current shell rather than introducing a new visual system.
6. Verify the UI can start a task and render persisted task state from the intended API path.

## Verification

This task is complete when:

- the landing page still loads as before
- the new task page is reachable from the existing shell
- an operator can start a task from the UI
- recent task state is shown from persisted backend reads
- the UI validates the intended API path rather than bypassing it
