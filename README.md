# ConstructraOS

ConstructraOS is a reusable full-stack baseline derived from [starterkit](https://github.com/bmj16060/starterkit).

It includes:

- a Micronaut API boundary
- Temporal-backed orchestration
- PostgreSQL persistence
- Valkey caching
- OpenTelemetry tracing with Jaeger
- OPA-backed policy evaluation
- a React/Vite UI shell served by `nginx`

What it does today:

- serves a small UI shell through `nginx`
- exposes API endpoints for `/api/session`, `/api/workflows/hello-world/run`, `/api/workflows/hello-world/start`, and `/api/workflows/hello-world/history`
- exposes an MCP HTTP endpoint at `/mcp` with a single `hello` tool
- runs a Temporal-backed `hello-world` workflow
- evaluates policy through OPA and the policy service
- persists workflow run history in Postgres
- emits traces through OpenTelemetry and Jaeger

The local deployment stack uses one durable PostgreSQL instance with separate logical databases for:

- the app (`constructraos`)
- Temporal default persistence (`temporal`)
- Temporal visibility (`temporal_visibility`)

The baseline also includes a signed anonymous session cookie. There is still no real login flow, but the API now issues a stable per-browser session at `/api/session` so policy evaluation and future UI personalization can hang off a real session boundary instead of a hard-coded actor stub.

## What This Repo Optimizes For

- A clean baseline for building ConstructraOS.
- A practical starting point for developing ConstructraOS locally.
- Clear seams for adding the real ConstructraOS domain without rewriting the baseline first.
- Documented patterns for workflows, policy, persistence, tracing, caching, and frontend data flow.

## First Codex Prompt

Point Codex at this repo and start with the business problem, not the implementation details. `AGENTS.md` instructs Codex to begin with a short interview when the domain is not yet clearly defined.

## Repository Layout

```text
ConstructraOS/
  docs/
    status.md
    patterns.md
    interviews/
  libraries/
    commons/
    clients/
    persistence/
  services/
    api-service/
    codex-runtime/
    orchestration/
    policy-service/
    ui-service/
  shared/
    policies/opa/
```

## Quick Start

1. Copy `.env.example` to `.env` and set the LLM values that match your local setup.
2. Configure the containerized Codex runtime using one of these supported paths:

- set `OPENAI_API_KEY` in `.env`
- place `auth.json` and optional `config.toml` under repo-local `.codex-runtime/`

To seed `.codex-runtime/` from an existing local Codex CLI login, run:

```bash
./bin/bootstrap-codex-runtime.sh
```

This Compose path no longer reads the host user's `~/.codex` directory directly.

3. Start the local stack:

```bash
docker compose up --build
```

This path now uses a root multi-stage Docker build, so Compose compiles the full Gradle project and the UI assets before assembling the runtime images.

The default local `ui-service` path also mounts `services/ui-service/build/frontend-static` as an overlay when that directory contains a host build, so frontend watch output can take over without replacing the clean-checkout fallback baked into the image. Compose also runs a `codex-runtime` service that owns `codex exec` and exposes the runtime boundary to orchestration over the internal network.

4. Open:

- UI shell: [http://localhost:18090](http://localhost:18090)
- API health: [http://localhost:18080/api/healthz](http://localhost:18080/api/healthz)
- Temporal UI: [http://localhost:18233](http://localhost:18233)
- Jaeger: [http://localhost:18686](http://localhost:18686)

The compose host ports are configurable through `.env` if those defaults still collide with other local services.

For the current demo Codex workflow, a blank `workingDirectory` resolves to `/workspace` inside the wrapper container, which is the mounted repo root during Compose runs.

## Local Iteration

- Java services:

```bash
./gradlew :services:api-service:run
./gradlew :services:orchestration:run
./gradlew :services:policy-service:run
```

If you run `orchestration` outside Compose, either start `codex-runtime` through Compose as well or switch the worker back to direct CLI mode with `CODEX_RUNTIME_MODE=cli`.

- Frontend dev server:

```bash
cd services/ui-service/frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080`.

- Frontend validation through the compose-managed `nginx` container:

```bash
docker compose up --build ui-service api-service orchestration policy-service codex-runtime
```

In this mode, `ui-service` serves baked image assets until a host frontend build is present, then automatically switches to the mounted overlay for fast browser refreshes.

- Fast UI iteration with the default compose overlay:

```bash
./gradlew :services:ui-service:buildFrontendAssets
docker compose up --build ui-service api-service orchestration policy-service codex-runtime

cd services/ui-service/frontend
npm install
npm run build:watch
```

This keeps the containerized `nginx` path, but the mounted `services/ui-service/build/frontend-static` directory takes over as soon as it contains a frontend build so browser refreshes pick up rebuilds without rebuilding the image.

## Deployment Model

The first deployable path is Docker Compose. The service boundaries stay container-friendly so you can later replace compose with Kubernetes, Nomad, ECS, or another deployment layer without rewriting the application seams.

The Compose baseline intentionally keeps a single persistent Postgres server while splitting the application, Temporal, and Temporal visibility data into separate databases. The named Docker volume `postgres-data` keeps those databases across container restarts and recreations unless you explicitly remove the volume.

## Graph DB Follow-On

This baseline does not hard-wire a graph database yet, but it does reserve the seam:

- `graph.*` config exists in API and orchestration
- `docs/patterns.md` documents where a future `libraries/graph-store` boundary should sit
- domain discovery should decide whether the graph belongs on the query path, orchestration side effects, or both before code is added

## Docs

- [status.md](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/status.md)
- [patterns.md](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/patterns.md)
- [interviews/README.md](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/interviews/README.md)
