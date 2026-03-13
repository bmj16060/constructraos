# ConstructraOS Status

Last updated: 2026-03-13

## Current Focus

- Keep the reusable platform baseline buildable and deployable while shifting the target domain to SDLC agent-team orchestration.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- Bootstrap the first domain slice with filesystem-backed markdown artifacts before moving durable project state deeper into the platform.
- Make Codex a first-class execution path so project, task, and specialist workflows can eventually operate through Codex projects, threads, and MCP-connected tools.
- Keep project memory behind a deliberate graph-store boundary rather than scattering graph concerns through services.

## In Progress

- The repo-backed markdown project contract under `projects/constructraos/` is still the bootstrap system-of-record while a deeper durable replacement remains undecided.
- The long-running task workflow and QA -> SRE handoff are in place, but the SRE environment outcome is still reported back by an external caller rather than produced by real specialist execution.
- The Codex execution seam is durable enough to dispatch, list, claim, and accept execution requests, but it does not yet expose richer progress or completion callbacks back into workflows.
- `codex-bridge` exists as a dedicated service boundary in Compose, but its `codex app-server` conversation client is still a placeholder rather than a real `thread/start` / `thread/resume` transport implementation.
- The graph-store seam is still only documented; no concrete graph boundary or implementation has been introduced yet.

## Next 3 Tasks

1. Replace the placeholder bridge conversation client with a real `codex app-server` protocol client using `thread/start`, `thread/resume`, and turn submission.
2. Produce the SRE environment outcome signal from real Codex-mediated specialist execution instead of relying on an external caller to report that outcome back into the task workflow.
3. Extend the Codex execution seam beyond acceptance-only callbacks so workflows can observe richer execution progress and completion state without collapsing back into direct container-side execution.

## Risks

- The first slice now spans orchestration, task management, git workflow, and test execution, so scope can expand too quickly unless the bootstrap contract stays narrow.
- The Codex execution model is directionally clear, but the concrete v1 integration boundary between CLI usage and a Codex MCP server is not finalized yet.
- Workflow topology is still open: project workflows may own child task workflows or coordinate peer workflows, and the wrong early abstraction could create churn.
- The graph database boundary is intentional but not implemented yet; early project memory should stay behind a dedicated seam even if v1 uses simpler filesystem-backed artifacts first.
