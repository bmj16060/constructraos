# TASK-003B-02: App Server Session Runtime

Status: Planned

Date: 2026-03-14

Related:

- [ADR-002: App Server Temporal Execution Model](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-002-app-server-temporal-execution-model.md)
- [TASK-003B: App Server Temporal Runtime](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-003B-app-server-temporal-runtime.md)
- [TASK-001A: Containerized Codex Runtime Boundary](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-001A-containerized-codex-runtime-boundary.md)

## Goal

Evolve `codex-runtime` from a one-shot CLI wrapper into a runtime that manages app-server threads, turns, and normalized event streaming.

## Why This Next

Once coordination persistence exists, the runtime can safely own live app-server sessions without also owning task business state.

## In Scope

- long-lived app-server process management inside `codex-runtime`
- thread start or resume and turn start handling
- per-turn `outputSchema` support
- normalized runtime events for orchestration callbacks
- approval command forwarding and interrupt handling

## Out of Scope

- Temporal workflow state-machine changes
- operator-facing projection changes
- final recovery policy decisions

## Work Sequence

1. Introduce app-server process lifecycle management in `codex-runtime`.
2. Add session and turn start or resume support.
3. Normalize provider events into a stable internal runtime event contract.
4. Preserve the existing structured result contract for terminal completion.
5. Verify app-server-backed runs through the compose-served runtime path.

## Verification

This task is complete when:

- `codex-runtime` can run a task turn through app-server
- per-turn structured output still works through `outputSchema`
- normalized progress and approval events can be emitted to orchestration
- runtime behavior is verifiable in the compose-served stack
