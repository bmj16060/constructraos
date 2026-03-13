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
- The API now also supports atomic claim of pending execution requests so a Codex thread can stamp its thread ID onto the durable request and acknowledge the workflow in one step.
- A dedicated `codex-bridge` service boundary now exists in Compose, so orchestration targets the bridge while the bridge owns the future host-local `codex app-server` conversation protocol.
- The bridge now speaks the live `codex app-server` JSON-RPC protocol for `initialize`, `thread/start`, `thread/resume`, and `turn/start`, and the execution dispatch contract can carry an existing Codex thread ID for resume-aware follow-up work.
- The codex-bridge test suite now includes a live integration test that spawns a real `codex app-server`, verifies `thread/start` returns a real thread ID, and proves the bridge can resume that thread after rollout persistence lands.
- The bridge now also owns the first real callback back into ConstructraOS: after a successful thread start/resume and initial turn submission, it POSTs the accepted execution signal back through the API so the task workflow can update durable state from bridge-driven progress.
- The bridge now also watches async Codex App Server turn-completion notifications and can post a fallback `reportSreEnvironmentOutcome` callback when a specialist turn completes or fails without a durable tool-driven outcome signal.
- `api-service` now hosts an explicit Micronaut MCP surface at `/mcp`, `ui-service` proxies that path, and Codex-facing workflow tools now live there instead of on the bridge transport boundary.
- Task workflow execution request IDs now advance from the repo-backed execution index instead of relying only on in-memory workflow counters, so retries create new durable execution records after worker restarts.
- The local Codex runtime is now also being containerized inside the Compose stack with a dedicated `codex-runtime` service and an internal Docker daemon sidecar so the bridge, agent runtime, and execution workspaces can share one reproducible filesystem/network boundary instead of relying on host-local path translation.
- GitHub App credentials are now available for `bmj16060/constructraos`, establishing the first real path toward GitHub-backed branch, push, and PR operations for isolated specialist executions.
- The next environment-isolation direction is shifting away from branch-scoped Compose concurrency toward execution-scoped Kubernetes environments, with Compose retained as the local baseline stack.
- The Kubernetes storage direction is now to keep execution workspaces ephemeral per run, leave source-of-truth code in GitHub, and move durable execution artifacts to an S3-compatible object-store boundary instead of shared writable volumes.
- The local Kubernetes branch-team path is now narrowed to one team PVC with a shared repo clone and per-agent `git worktree` directories, avoiding shared writable checkouts while keeping local storage simple.
- Orchestration now has a first `KubectlActivities` seam so future environment launch flows can centralize `kubectl` execution instead of scattering shell calls through workflows.

## Next 3 Tasks

1. Define the first GitHub-backed execution model so specialist runs can clone, branch, push, and open PRs without relying on host-local worktree reconciliation.
2. Introduce an execution-scoped Kubernetes environment launcher, starting locally with a team-PVC-plus-worktrees model and then layering artifact handling on top of it instead of continuing to treat branch-scoped Compose startup as the main concurrency path.
3. Introduce the first long-running project workflow that coordinates task workflows through signals instead of treating task execution as isolated workflow starts.

## Risks

- The first slice now spans orchestration, task management, git workflow, and test execution, so scope can expand too quickly unless the bootstrap contract stays narrow.
- The bridge can now start or resume Codex threads against a containerized app-server/runtime path, submit turns, and signal accepted execution back into the workflow, but specialist runs are still not yet completing the full SRE environment path and the current branch workspace is still shared rather than isolated per execution request.
- Moving execution into GitHub-backed Kubernetes environments will reduce local filesystem and host-port coupling, but it introduces new requirements around credentials, namespace lifecycle, and environment readiness contracts.
- Workflow topology is still open: project workflows may own child task workflows or coordinate peer workflows, and the wrong early abstraction could create churn.
- The graph database boundary is intentional but not implemented yet; early project memory should stay behind a dedicated seam even if v1 uses simpler filesystem-backed artifacts first.
