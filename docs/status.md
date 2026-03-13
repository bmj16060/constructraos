# ConstructraOS Status

Last updated: 2026-03-13

## Reset Note

- The current implementation branch was intentionally reset back to the bootstrap contract/code baseline from commit `031fc49`.
- The more aggressive functional-slice experiment was preserved as git tag `archive-functional-slices-2026-03-13` for reference and selective cherry-picking.
- Discovery notes and ADR intent remain the primary guide for rebuilding cleaner abstractions from the bootstrap baseline.

## Current Focus

- Keep the reusable platform baseline buildable and deployable while shifting the target domain to SDLC agent-team orchestration.
- Preserve the full core spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- Bootstrap the first domain slice with filesystem-backed markdown artifacts before moving durable project state deeper into the platform.
- Reintroduce new execution abstractions in small demonstrable pieces instead of jumping directly to end-to-end functional slices.
- Keep project memory behind a deliberate graph-store boundary rather than scattering graph concerns through services.

## In Progress

- The branch code is intentionally back at the bootstrap baseline, with the reusable platform spine and the original hello-world workflow still serving as the executable reference point.
- Discovery has defined the target domain: project workflows that manage task and bug execution through specialist agents, with ADRs, git branching, testing, and environment lifecycle as first-class delivery artifacts.
- The repo-backed markdown contract under `projects/constructraos/` remains the intended first durable state boundary for project plans, tasks, branches, evidence, and future environment records.
- ADRs now capture forward direction for Kubernetes execution storage and the local worktree-team model, but those decisions are not yet implemented on this reset branch.
- The next implementation passes should favor explicit seams and policy surfaces before rebuilding higher-level workflow slices.

## Next 3 Tasks

1. Define the first minimal execution abstraction boundary so specialist dispatch can be demonstrated independently of a full workflow slice.
2. Define the first durable environment control abstraction, including namespace ownership, lifecycle policy inputs, and cleanup semantics, before wiring Kubernetes actions.
3. Rebuild the first project-to-task orchestration slice from the bootstrap baseline using those smaller seams instead of jumping straight to a full QA -> SRE path.

## Risks

- It is easy to recreate the same coupling that existed on the archived functional-slice branch if new work skips over core abstractions in favor of fast end-to-end slices.
- The Codex execution model and Kubernetes direction are documented, but the concrete abstraction boundaries for dispatch, environment control, and cleanup are still intentionally undecided on this branch.
- Workflow topology is still open: project workflows may own child task workflows or coordinate peer workflows, and the wrong early abstraction could create churn.
- The graph database boundary is intentional but not implemented yet; early project memory should stay behind a dedicated seam even if v1 uses simpler filesystem-backed artifacts first.
