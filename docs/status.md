# ConstructraOS Status

Last updated: 2026-03-13

## Current Focus

- Keep the renamed ConstructraOS repo aligned with the generic starter baseline rather than carrying forward domain-specific workflow code.
- Keep the demo intentionally small: one LLM-backed `hello-world` workflow plus persisted history.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- Keep the no-login baseline, but route request identity through a real anonymous session boundary.
- Keep the next architectural seam ready for a future graph database without forcing one into the first slice prematurely.

## In Progress

- The repo has been reset to the generic baseline shape and renamed to ConstructraOS, but some baseline wording still needs cleanup where `StarterKit` or old reference text survives in docs or UI copy.
- The codebase builds successfully with `./gradlew build`, but the compose-served stack should be re-verified in the renamed state before treating the local deployment path as settled.
- Repo guidance now assumes domain discovery happens before new domain code is added, but the next real domain slice is intentionally undefined after the reset.

## Next 3 Tasks

1. Remove any remaining renamed-baseline drift, especially leftover `StarterKit` or stale reference text in docs, UI copy, and runtime-facing labels.
2. Run and verify the full `docker compose up --build` path in the renamed ConstructraOS state, including UI, API, Temporal, policy, and history flow.
3. Start the next real domain only after a fresh discovery interview, keeping the baseline generic until that first slice is deliberately chosen.

## Risks

- The LLM path requires reachable provider configuration; the repo can boot without a valid provider, but the demo workflow will fail until LLM env vars are set correctly.
- Temporal SQL bootstrap still assumes the single Postgres server can create and retain the `temporal` and `temporal_visibility` databases via the checked-in init script on first volume initialization.
- Anonymous session signing currently defaults to a local development secret unless `ANON_SESSION_SIGNING_SECRET` is overridden for deployed environments.
- The graph database boundary is intentional but not implemented yet; a later slice still needs a concrete technology choice and usage pattern.
