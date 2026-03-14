# ConstructraOS Status

Last updated: 2026-03-14

## Current Focus

- ConstructraOS is the reusable baseline derived from [github.com/bmj16060/starterkit](https://github.com/bmj16060/starterkit).
- Keep the shipped demo intentionally small while introducing the first Codex orchestration slice.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- The baseline uses an anonymous session boundary for request identity and policy input.
- Graph support remains an open architectural seam rather than an active part of the current baseline.
- The next platform extension is project-aware Codex orchestration backed by Temporal workflows, activities, and PostgreSQL persistence.

## In Progress

- The compose-served local stack remains the primary deployment and verification path for baseline development.
- The current demo path is the anonymous-session-backed `hello-world` workflow, with UI history and policy enforcement serving as the reference implementation.
- ADR-001 now captures the Codex orchestration boundaries, while task documents capture execution sequencing for the next backend slice.

## Next 3 Tasks

1. Prove the first closed loop: a Temporal workflow invokes Codex through an activity and receives a simple structured response.
2. Add PostgreSQL schema and persistence boundaries after the invocation seam is proven.
3. Expose the first task start/status path through API and MCP boundaries once invocation and persistence are both in place.

## Risks

- The LLM path requires reachable provider configuration; the repo can boot without a valid provider, but the demo workflow will fail until LLM env vars are set correctly.
- Temporal SQL bootstrap still assumes the single Postgres server can create and retain the `temporal` and `temporal_visibility` databases via the checked-in init script on first volume initialization.
- Anonymous session signing currently defaults to a local development secret unless `ANON_SESSION_SIGNING_SECRET` is overridden for deployed environments.
- The graph database boundary is intentional but not implemented yet; a later slice still needs a concrete technology choice and usage pattern.
- Transcript payload size may eventually outgrow PostgreSQL-only storage, even though storing transcript events in `jsonb` is acceptable for the first slice.
