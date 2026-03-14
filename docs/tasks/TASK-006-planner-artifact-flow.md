# TASK-006: Planner Artifact Flow

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-005: Change and Review Records](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-005-change-and-review-records.md)

## Goal

Allow the system to turn a concept or requested change into draft planning artifacts:

- design notes
- ADR proposals
- task breakdown documents

## Why This Next

The system cannot enter a self-building phase responsibly until planning artifacts become part of the product workflow rather than only manual authoring steps.

## In Scope

- planner-oriented orchestration path for concept analysis
- artifact generation or update flow
- approval state for planning artifacts
- operator review and acceptance path before artifacts become active

## Out of Scope

- autonomous acceptance of generated planning artifacts
- implementation execution without approval
- roadmap reprioritization without operator input

## Work Sequence

1. Define the input contract for a new concept or requested change.
2. Define the output contracts for draft design notes, ADR proposals, and task documents.
3. Add persistence for planning artifact drafts and approval state.
4. Implement the planner-oriented workflow that proposes artifact changes.
5. Add the approval path that promotes accepted drafts into active repo-facing planning artifacts.

## Verification

This task is complete when:

- a concept can produce draft planning artifacts through the system
- generated artifacts are clearly marked as drafts until approved
- approved artifacts can become the active planning basis for later work
- the system cannot silently redefine its roadmap without approval
