# TASK-001A: Containerized Codex Runtime Boundary

Status: Complete

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-001: Codex Invocation Vertical Slice](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-001-codex-invocation-vertical-slice.md)

## Goal

Replace the current host-dependent Codex CLI execution path with a runtime boundary that works in the intended containerized deployment model.

The preferred target is the sidecar-style boundary already described in ADR-001, but a small wrapper service is also acceptable if it preserves the same execution contract.

Chosen implementation:

- a small internal `codex-runtime` wrapper service with an HTTP API
- orchestration now uses an HTTP-backed execution adapter by default
- Compose supplies Codex auth explicitly through `OPENAI_API_KEY` or repo-local `.codex-runtime/`

## Why This Next

TASK-001 proved the workflow, activity, and structured-result seam, but real execution still depends on host-local `~/.codex` credentials and config.

That is acceptable for a proof slice, but it is not an acceptable platform contract for Compose or later deployed environments.

This follow-on isolates Codex runtime concerns so orchestration does not depend on host-specific operator state.

## In Scope

- choose the first supported runtime boundary for Codex execution:
  - sidecar-local process wrapper around the Codex CLI, or
  - small wrapper service with an API
- define how Codex auth/config are supplied in local Compose and later deployed environments
- make the execution boundary work without reading the host operator's `~/.codex` directory
- keep the existing execution request/result contract stable for workflow callers
- update local deployment docs and configuration for the new runtime path
- verify the Codex execution slice through the Compose-served path rather than host-local fallback

## Out of Scope

- PostgreSQL-backed orchestration persistence
- task status read APIs
- MCP task tools
- multi-agent routing
- workspace leasing

## Work Sequence

1. Decide the first deployable Codex runtime boundary and document the choice if it changes repo expectations materially.
2. Define the environment/config contract for Codex credentials and runtime configuration.
3. Implement the sidecar or wrapper boundary.
4. Repoint the reusable execution adapter to that boundary without changing workflow semantics.
5. Update Compose and local docs so the supported runtime path is explicit.
6. Verify the Codex execution workflow through the containerized path end to end.

## Verification

This task is complete when:

- the Codex execution slice runs through the intended containerized boundary
- host-local `~/.codex` state is no longer required for normal Compose verification
- the workflow still receives the same structured result contract
- local deployment documentation matches the supported runtime model

## Result

- Added `infra/codex-runtime/` with a small wrapper container that exposes `GET /healthz` and `POST /executions`.
- Repointed orchestration to an HTTP runtime adapter while preserving the workflow/activity request and result contract.
- Kept a `CODEX_RUNTIME_MODE=cli` fallback for direct worker runs outside Compose.
- Mounted the repo to `/workspace` for the wrapper container and made blank demo `workingDirectory` values resolve there.
- Updated Compose and README so the supported auth path is explicit through `.env` or repo-local `.codex-runtime/` and no longer depends on implicit host `~/.codex` state.
- Added `bin/bootstrap-codex-runtime.sh` to seed repo-local `.codex-runtime/` from an existing local Codex CLI login.
