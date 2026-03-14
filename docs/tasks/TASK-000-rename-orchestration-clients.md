# TASK-000: Rename `libraries/orchestration-clients` to `libraries/clients`

Status: Completed

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-001: Codex Invocation Vertical Slice](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-001-codex-invocation-vertical-slice.md)

## Goal

Rename `libraries/orchestration-clients` to `libraries/clients` before the boundary grows further.

## Why This First

The current name is already narrower than the intended direction of the boundary.

Doing the rename first prevents the broader client boundary from growing behind a misleading library name and avoids carrying accidental legacy terminology into the new orchestration work.

## In Scope

- rename the Gradle module directory
- update project/module references
- update imports, package names, and documentation references as needed
- keep behavior unchanged while clarifying the boundary name

## Out of Scope

- adding new client families beyond what is needed to preserve the current module behavior
- changing the responsibility split between API, workflow, and persistence boundaries

## Work Sequence

1. Rename `libraries/orchestration-clients` to `libraries/clients`.
2. Update Gradle settings and dependent module references.
3. Update Java package naming if needed to keep the library internally consistent.
4. Update documentation references to the renamed boundary.
5. Verify the existing typed workflow client path still builds and behaves the same after the rename.

## Verification

This task is complete when:

- the library is named `libraries/clients`
- dependent modules resolve against the renamed boundary
- the existing typed workflow client path still builds after the rename
- the docs no longer rely on the old library name except where historical context is intentional
