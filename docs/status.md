# ConstructraOS Status

Last updated: 2026-03-14

## Current Focus

- ConstructraOS is the reusable baseline derived from [github.com/bmj16060/starterkit](https://github.com/bmj16060/starterkit).
- Keep the shipped demo intentionally small while introducing the first Codex orchestration slice.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- The baseline uses an anonymous session boundary for request identity and policy input.
- The typed Temporal client boundary now lives in `libraries/clients`.
- Graph support remains an open architectural seam rather than an active part of the current baseline.
- TASK-001 is now implemented: a Temporal workflow can invoke Codex through a dedicated activity and return the minimal structured result contract without persistence.
- TASK-001A is now implemented: Compose uses a dedicated `codex-runtime` wrapper service so orchestration no longer depends on the host operator's `~/.codex` directory.
- The next platform extension is TASK-002 persistence so Codex orchestration state becomes queryable outside Temporal.

## In Progress

- The compose-served local stack remains the primary deployment and verification path for baseline development.
- The current demo path is the anonymous-session-backed `hello-world` workflow, with UI history and policy enforcement serving as the reference implementation.
- ADR-001 now captures the Codex orchestration boundaries, while task documents capture execution sequencing for the remaining backend slices.
- TASK-001A replaced the worker-local CLI path with an internal HTTP runtime adapter plus a `codex-runtime` wrapper container that owns `codex exec`.
- Codex auth for Compose now comes from explicit `.env` configuration or repo-local `.codex-runtime/`, not implicit host-home state.
- The implementation ladder is now captured in `TASK-000` through `TASK-010`, including the later self-building phase.

## Next 3 Tasks

1. Add PostgreSQL schema and persistence boundaries now that the invocation and runtime seams are proven.
2. Expose task start/status and MCP task tools on top of durable orchestration state.
3. Add workspace leasing once task state and execution records are durable enough to coordinate write access safely.

## Risks

- The LLM path requires reachable provider configuration; the repo can boot without a valid provider, but the demo workflow will fail until LLM env vars are set correctly.
- Temporal SQL bootstrap still assumes the single Postgres server can create and retain the `temporal` and `temporal_visibility` databases via the checked-in init script on first volume initialization.
- Anonymous session signing currently defaults to a local development secret unless `ANON_SESSION_SIGNING_SECRET` is overridden for deployed environments.
- The graph database boundary is intentional but not implemented yet; a later slice still needs a concrete technology choice and usage pattern.
- Transcript payload size may eventually outgrow PostgreSQL-only storage, even though storing transcript events in `jsonb` is acceptable for the first slice.
- The supported Codex runtime path now depends on explicit container credentials through `OPENAI_API_KEY` or repo-local `.codex-runtime/auth.json`; an unconfigured runtime still boots but Codex workflow execution will fail until that boundary is configured.
