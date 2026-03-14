# TASK-005: Change and Review Records

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-004: Workspace Leasing](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-004-workspace-leasing.md)

## Goal

Add provider-neutral change and review records so review flows are modeled without hard-wiring the domain to GitHub.

## Why This Next

Once workspaces exist for write-capable work, the next durable boundary is the source change and review target that another agent or operator inspects.

## In Scope

- `change_sets`
- `review_targets`
- `review_system_references`
- first provider adapter contract for external review systems

## Out of Scope

- full review comment synchronization
- merge automation
- non-GitHub provider implementation beyond the abstraction layer

## Work Sequence

1. Define provider-neutral change and review record shapes.
2. Add persistence support for change and review records.
3. Define the first review-system adapter contract.
4. Link task outcomes and workspaces to change sets.
5. Add the first provider-backed implementation once the abstraction is stable.

## Verification

This task is complete when:

- change sets and review targets are first-class records
- provider-specific references are separated from the core domain records
- reviewable work can be linked back to its source workspace and task
- the first provider integration can be added without changing the core review model
