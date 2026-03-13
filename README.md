# ConstructraOS

ConstructraOS is an SDLC agent-orchestration platform built on a reusable full-stack baseline:

- a Micronaut API boundary
- Temporal-backed orchestration
- PostgreSQL persistence
- Valkey caching
- OpenTelemetry tracing with Jaeger
- OPA-backed policy evaluation
- a React/Vite UI shell served by `nginx`

The repo is no longer just a generic starter kit. Its active domain is running software delivery work through durable project, task, specialist, branch, and validation workflows.

## What It Does Today

ConstructraOS currently ships the first execution slice for running ConstructraOS through its own platform:

- repo-backed markdown project records under [`projects/constructraos/`](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos)
- a long-running task workflow that can accept QA requests and hand off to SRE
- durable Codex execution requests with claim, acceptance, and SRE outcome callbacks
- an MCP surface at `/mcp` for Codex-facing workflow tools
- a containerized local Codex runtime path for bridge-driven execution

The original `hello-world` workflow still exists as the smallest smokeable orchestration path, but it is no longer the main story of the repo.

## Current Direction

The platform baseline remains container-friendly, but the current product direction is:

- keep Docker Compose as the local baseline for building and single-stack validation
- use GitHub as the source of truth for branch, push, PR, and merge workflows
- move isolated specialist execution toward Kubernetes-backed environments instead of fighting branch-scoped host-port allocation in Compose
- keep environment lifecycle, execution records, and workflow state inside ConstructraOS rather than scattering that logic across ad hoc scripts

In short: Compose is still the developer bootstrap path; GitHub and Kubernetes are the likely execution backbone for real concurrent agent-team work.

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
    project-records/
  projects/
    constructraos/
  runtime/
    workspaces/
  services/
    api-service/
    codex-bridge/
    orchestration/
    policy-service/
    ui-service/
  shared/
    policies/opa/
```

## Local Baseline

1. Copy `.env.example` to `.env` and set the LLM values that match your local setup.
2. Start the local baseline stack:

```bash
docker compose up --build
```

This path uses the repo-root multi-stage Docker build, so Compose compiles the Gradle project and frontend assets before assembling runtime images.

3. Open:

- UI shell: [http://localhost:18090](http://localhost:18090)
- API health: [http://localhost:18080/api/healthz](http://localhost:18080/api/healthz)
- Temporal UI: [http://localhost:18233](http://localhost:18233)
- Jaeger: [http://localhost:18686](http://localhost:18686)

The host ports are configurable through `.env`.

## Local Iteration

- Java services:

```bash
./gradlew :services:api-service:run
./gradlew :services:orchestration:run
./gradlew :services:policy-service:run
```

- Frontend dev server:

```bash
cd services/ui-service/frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080`.

- Frontend validation through the compose-served `nginx` path:

```bash
docker compose up --build ui-service api-service orchestration policy-service
```

- Fast UI iteration with the overlay path:

```bash
./gradlew :services:ui-service:buildFrontendAssets
docker compose up --build ui-service api-service orchestration policy-service

cd services/ui-service/frontend
npm install
npm run build:watch
```

## Execution Model

The current execution model is centered on durable workflow records rather than local shell sessions:

- project state lives in markdown-first records under [`projects/constructraos/`](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos)
- workflows dispatch specialist execution requests
- `codex-bridge` starts or resumes Codex threads and reports acceptance back into ConstructraOS
- specialist tools report workflow outcomes through explicit MCP tools instead of relying on conversational summaries

The next major seam is isolated execution environments scoped to a concrete run, with GitHub-backed code movement and Kubernetes-backed environment scheduling.

## GitHub App Helper

The repo includes a helper to mint and verify a GitHub App installation token:

- [scripts/github-app-installation-token.sh](/Users/brandonjohnson/SourceCode/ConstructraOS/scripts/github-app-installation-token.sh)

Required environment variables:

- `GITHUB_APP_ID`
- `GITHUB_APP_INSTALLATION_ID`
- `GITHUB_APP_PRIVATE_KEY_FILE`

Example:

```bash
GITHUB_APP_ID=3083664 \
GITHUB_APP_INSTALLATION_ID=116126747 \
GITHUB_APP_PRIVATE_KEY_FILE=$HOME/.config/constructraos/github/constructraos-app.pem \
./scripts/github-app-installation-token.sh
```

## Docs

- [status.md](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/status.md)
- [patterns.md](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/patterns.md)
- [tools.md](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tools.md)
- [interviews/README.md](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/interviews/README.md)
- [projects/constructraos/README.md](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos/README.md)
