# TASK-008: Review and Rework Loop

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-007: Implementation Execution Loop](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-007-implementation-execution-loop.md)

## Goal

Route implementation output into structured review, capture review findings, and drive rework when required.

## Why This Next

Self-building requires a closed development loop, not just task execution. The system needs a first-class way to review changes and route feedback back into implementation.

## In Scope

- reviewer-oriented task flow against a stable review target
- durable review findings
- routing from review outcomes to follow-up implementation or completion
- repeated review and rework cycles until the change is accepted or blocked

## Out of Scope

- fully automated approval without review policy
- merge automation
- deployment promotion

## Work Sequence

1. Define review outcomes and finding shapes.
2. Implement reviewer task execution against change and review records.
3. Persist findings and acceptance/blocking outcomes.
4. Route rework tasks back to implementation when needed.
5. Verify that the same change can move through multiple review and rework cycles.

## Verification

This task is complete when:

- a completed implementation can enter review through the system
- review findings are durable and inspectable
- rework tasks can be generated from review outcomes
- accepted changes can move forward without losing review history
