# ConstructraOS Status

Last updated: 2026-03-13

## Current Focus

- ConstructraOS is based on the reusable starterkit at [github.com/bmj16060/starterkit](https://github.com/bmj16060/starterkit) and is ready to begin building out the real ConstructraOS domain.
- Keep the shipped demo intentionally small: one LLM-backed `hello-world` workflow plus persisted history.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- Keep the no-login baseline, but route request identity through a real anonymous session boundary.
- Keep the next architectural seam ready for a future graph database without forcing one into the first slice prematurely.

## In Progress

- The baseline is ready for the next domain-discovery interview and first durable slice.
- The compose-served local stack remains the primary deployment and verification path for baseline development.
- The current demo path is the anonymous-session-backed `hello-world` workflow, with UI history and policy enforcement serving as the reference implementation.

## Next 3 Tasks

1. Run a fresh discovery interview for the next real domain and define the first durable workflow and data slice.
2. Verify the full `docker compose up --build` path end to end, including UI, API, Temporal, policy, and persisted history.
3. Keep the baseline docs and patterns aligned as the first domain-specific implementation is introduced.

## Risks

- The LLM path requires reachable provider configuration; the repo can boot without a valid provider, but the demo workflow will fail until LLM env vars are set correctly.
- Temporal SQL bootstrap still assumes the single Postgres server can create and retain the `temporal` and `temporal_visibility` databases via the checked-in init script on first volume initialization.
- Anonymous session signing currently defaults to a local development secret unless `ANON_SESSION_SIGNING_SECRET` is overridden for deployed environments.
- The graph database boundary is intentional but not implemented yet; a later slice still needs a concrete technology choice and usage pattern.
