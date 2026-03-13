# ConstructraOS Status

Last updated: 2026-03-13

## Current Focus

- ConstructraOS is the reusable baseline derived from [github.com/bmj16060/starterkit](https://github.com/bmj16060/starterkit).
- Keep the shipped demo intentionally small: one LLM-backed `hello-world` workflow plus persisted history.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- The baseline uses an anonymous session boundary for request identity and policy input.
- Graph support remains an open architectural seam rather than an active part of the current baseline.

## In Progress

- The compose-served local stack remains the primary deployment and verification path for baseline development.
- The current demo path is the anonymous-session-backed `hello-world` workflow, with UI history and policy enforcement serving as the reference implementation.
- Baseline docs are being kept aligned with the current reset state so they describe only what exists today.

## Next 3 Tasks

1. Verify the full `docker compose up --build` path end to end, including UI, API, Temporal, policy, and persisted history.
2. Remove any remaining stale baseline wording where docs or UI copy drift from the current repo state.
3. Keep the baseline docs and patterns aligned before introducing any new domain-specific work.

## Risks

- The LLM path requires reachable provider configuration; the repo can boot without a valid provider, but the demo workflow will fail until LLM env vars are set correctly.
- Temporal SQL bootstrap still assumes the single Postgres server can create and retain the `temporal` and `temporal_visibility` databases via the checked-in init script on first volume initialization.
- Anonymous session signing currently defaults to a local development secret unless `ANON_SESSION_SIGNING_SECRET` is overridden for deployed environments.
- The graph database boundary is intentional but not implemented yet; a later slice still needs a concrete technology choice and usage pattern.
