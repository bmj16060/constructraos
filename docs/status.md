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
- The next platform extension is a containerized Codex runtime boundary so the execution path no longer depends on host-local operator credentials.

## In Progress

- The compose-served local stack remains the primary deployment and verification path for baseline development.
- The current demo path is the anonymous-session-backed `hello-world` workflow, with UI history and policy enforcement serving as the reference implementation.
- ADR-001 now captures the Codex orchestration boundaries, while task documents capture execution sequencing for the remaining backend slices.
- TASK-001 added a minimal API trigger surface plus reusable Codex CLI adapter code under `libraries/commons` for the local `codex exec --json --output-schema` path.
- TASK-001A is now the deployability follow-on for replacing the current host-dependent execution fallback with the intended sidecar or wrapper boundary.
- The implementation ladder is now captured in `TASK-000` through `TASK-010`, including the later self-building phase.

## Next 3 Tasks

1. Replace the host-dependent Codex CLI path with a containerized runtime boundary.
2. Add PostgreSQL schema and persistence boundaries now that the invocation seam is proven.
3. Expose task start/status and MCP task tools on top of durable orchestration state.

## Risks

- The LLM path requires reachable provider configuration; the repo can boot without a valid provider, but the demo workflow will fail until LLM env vars are set correctly.
- Temporal SQL bootstrap still assumes the single Postgres server can create and retain the `temporal` and `temporal_visibility` databases via the checked-in init script on first volume initialization.
- Anonymous session signing currently defaults to a local development secret unless `ANON_SESSION_SIGNING_SECRET` is overridden for deployed environments.
- The graph database boundary is intentional but not implemented yet; a later slice still needs a concrete technology choice and usage pattern.
- Transcript payload size may eventually outgrow PostgreSQL-only storage, even though storing transcript events in `jsonb` is acceptable for the first slice.
- The first Codex execution slice depends on a locally available Codex CLI plus valid `~/.codex/auth.json` and `~/.codex/config.toml`; TASK-001A exists to remove that host dependency from the supported runtime path.
