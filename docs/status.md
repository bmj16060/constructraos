# ConstructraOS Status

Last updated: 2026-03-13

## Current Focus

- Land the reusable platform baseline as a standalone, buildable, deployable repo.
- Keep the demo intentionally small: one LLM-backed `hello-world` workflow plus persisted history.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- Keep the no-login baseline, but route request identity through a real anonymous session boundary.
- Keep the next architectural seam ready for a future graph database without forcing one into the first slice prematurely.

## In Progress

- Simplified Gradle multi-project layout is replacing Aviation's included build-logic pattern.
- A compose-first deployment path is being wired for local deployment of the full platform baseline, including one durable Postgres instance split into separate app, Temporal, and Temporal visibility databases.
- Compose image builds now run from the repo root through a shared multi-stage Dockerfile so `docker compose up --build` can compile the full Gradle project and package the resulting artifacts directly into each service image.
- The `hello-world` workflow is being upgraded to use prompt rendering, real LLM calls, Postgres-backed history, and OPA-gated API access.
- The `hello-world` workflow now also carries anonymous session context into orchestration and evaluates workflow-side policy through a dedicated policy activity so business rules can live in Rego instead of only at API ingress.
- API requests now bootstrap through a signed anonymous session cookie and `/api/session`, and policy input uses session context instead of a fixed builder actor.
- The default local UI path now combines baked frontend assets with a host-mounted overlay fallback so clean checkouts still boot while `build:watch` can take over immediately once frontend assets exist on disk.
- Repo guidance is being rewritten so Codex starts with a short business-domain interview when the domain is not yet defined.

## Next 3 Tasks

1. Finish the deployable compose stack and verify the services start together.
2. Verify the UI shell can trigger the workflow end to end and render history.
3. Document the graph-store extension seam once the first buildable baseline is green.

## Risks

- The LLM path requires reachable provider configuration; the repo can boot without a valid provider, but the demo workflow will fail until LLM env vars are set correctly.
- Temporal SQL bootstrap now assumes the single Postgres server can create and retain the `temporal` and `temporal_visibility` databases via the checked-in init script on first volume initialization.
- Anonymous session signing currently defaults to a local development secret unless `ANON_SESSION_SIGNING_SECRET` is overridden for deployed environments.
- The graph database boundary is intentional but not implemented yet; a later slice still needs a concrete technology choice and usage pattern.
