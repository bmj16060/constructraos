# ConstructraOS Patterns

## Service Boundaries

- `api-service`
  - external HTTP boundary
  - thin request/response boundary over synchronous read paths
  - workflow start/run adapters
  - policy checks before side effects
- `orchestration`
  - Temporal workflows and activities
  - durable retries/timeouts
  - LLM, cache, and persistence side effects
- `policy-service`
  - small adapter over OPA
  - central place for policy decision translation
- `ui-service`
  - static SPA host
  - same-origin proxy for `/api` and tracing

## Workflow Pattern

1. API accepts typed input.
2. API checks policy.
3. API calls a typed workflow client.
4. Workflow can call policy evaluation activities for workflow-side auth and business rules.
5. Workflow renders a prompt.
6. Workflow calls the shared LLM activity.
7. Activity persists durable results.
8. API and UI read the resulting state back through normal service boundaries.

## Caching Pattern

- LLM caching lives inside the shared `LlmActivitiesImpl` path.
- Valkey is optional in local code paths and can fail over to in-memory cache when disabled.
- Cache keys should be explicit and domain-aware.

## Persistence Pattern

- Shared persistence entities and repositories live in `libraries/persistence`.
- Query services and data access helpers should live in shared library boundaries such as `libraries/persistence`, not inside `api-service` controllers.
- Schema changes land as Flyway migrations in that same library so both API and worker see the same migration set.
- The first demo persists workflow history, not domain complexity.

## Policy Pattern

- Services do not embed authorization logic directly in controllers when a reusable decision can live in OPA.
- Deterministic business rules should live behind the policy boundary when they are reusable policy decisions rather than controller-local request handling.
- API calls `policy-service`.
- `policy-service` delegates to OPA.
- Workflows can call `policy-service` through a dedicated Temporal activity when policy-evaluable business rules belong inside orchestration rather than only at API ingress.
- Rego policies live under `shared/policies/opa/`.

## Session Pattern

- ConstructraOS ships with anonymous sessions even though it does not ship a login system.
- `api-service` owns the session cookie and exposes `/api/session` for bootstrap.
- Policy input should use session-backed actor context instead of controller-local hard-coded identities.
- Real authentication can replace or enrich the anonymous actor later without changing every endpoint contract first.

## Tracing Pattern

- Java services emit OTLP spans.
- Temporal client and worker tracing is bridged through `OpenTracingOptions`.
- The UI shell forwards browser fetch traces to the same OTLP endpoint through `nginx`.

## Frontend Pattern

- Use a thin fetch client.
- Put server state behind TanStack Query.
- Keep the shell simple enough to swap in a real domain quickly.

## Codex Pattern

- Start with discovery when the domain is unclear.
- Convert interview output into the first durable workflow and data slice.
- Keep the starter repo generic; domain-specific complexity belongs in the next repo phase, not in the baseline.

## Graph Store Follow-On

When a graph database becomes real work:

1. Add a dedicated `libraries/graph-store` boundary instead of importing a graph driver directly into multiple services.
2. Decide whether the graph is:
   - read-optimized query infrastructure
   - orchestration-side relationship storage
   - both
3. Document the decision before spreading graph semantics into the domain model.
