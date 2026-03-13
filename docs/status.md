# ConstructraOS Status

Last updated: 2026-03-13

## Current Focus

- Keep the reusable platform baseline buildable and deployable while shifting the target domain to SDLC agent-team orchestration.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- Bootstrap the first domain slice with filesystem-backed markdown artifacts before moving durable project state deeper into the platform.
- Make Codex a first-class execution path so project, task, and specialist workflows can eventually operate through Codex projects, threads, and MCP-connected tools.
- Keep project memory behind a deliberate graph-store boundary rather than scattering graph concerns through services.

## In Progress

- Simplified Gradle multi-project layout is replacing Aviation's included build-logic pattern.
- A compose-first deployment path is being wired for local deployment of the full platform baseline, including one durable Postgres instance split into separate app, Temporal, and Temporal visibility databases.
- Compose image builds now run from the repo root through a shared multi-stage Dockerfile so `docker compose up --build` can compile the full Gradle project and package the resulting artifacts directly into each service image.
- API requests now bootstrap through a signed anonymous session cookie and `/api/session`, and policy input uses session context instead of a fixed builder actor.
- The default local UI path now combines baked frontend assets with a host-mounted overlay fallback so clean checkouts still boot while `build:watch` can take over immediately once frontend assets exist on disk.
- Discovery has now defined the first real domain: project workflows that manage task and bug execution through specialist agents, with ADRs, git branching, and testing as first-class delivery artifacts.
- The initial domain slice now has a repo-backed markdown contract under `projects/constructraos/` for ADRs, project plans, task state, branch state, and execution evidence.
- v1 is expected to spawn specialist agents, including an SRE specialist, and provide specialist-specific prompts/guidance instead of stopping at planning-only workflow records.
- The first long-running task workflow slice now exists as a signal-driven Temporal path that can `signalWithStart` a task workflow, query its state, and write QA evidence through the repo-backed project-records boundary.
- The task workflow now models the first QA -> SRE handoff state, including SRE environment outcome signals and evidence capture for branch-scoped environment readiness.
- Compose/runtime roots are now explicitly separated between repo-backed project records and mutable execution workspaces so worker filesystem access is deliberate rather than implicit.
- The SRE execution path is now pivoting to a Codex-mediated model: workflows dispatch durable execution requests and track Codex thread identity instead of assuming the orchestration container directly runs Compose.
- The first durable Codex execution-request path now exists end to end: workflows write execution requests, API can list them for consumption, and Codex can callback with acceptance and SRE outcomes.

## Next 3 Tasks

1. Add the first true MCP-facing Codex claim path so a real Codex thread can atomically claim a pending execution request instead of relying on a plain API read plus callback pattern.
2. Introduce the first long-running project workflow that coordinates task workflows through signals instead of treating task execution as isolated workflow starts.
3. Establish the graph-store boundary and Codex integration seam without locking in premature choices about child workflows versus peer workflows.

## Risks

- The first slice now spans orchestration, task management, git workflow, and test execution, so scope can expand too quickly unless the bootstrap contract stays narrow.
- The Codex execution model is directionally clear, but the concrete v1 integration boundary between CLI usage and a Codex MCP server is not finalized yet.
- Workflow topology is still open: project workflows may own child task workflows or coordinate peer workflows, and the wrong early abstraction could create churn.
- The graph database boundary is intentional but not implemented yet; early project memory should stay behind a dedicated seam even if v1 uses simpler filesystem-backed artifacts first.
