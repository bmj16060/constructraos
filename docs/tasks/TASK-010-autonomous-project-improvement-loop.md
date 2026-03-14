# TASK-010: Autonomous Project Improvement Loop

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-009: Verification and Promotion Gates](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-009-verification-and-promotion-gates.md)

## Goal

Let the system identify, plan, execute, review, and advance bounded improvements to the project through its own orchestration loop.

## Why This Last

This is the earliest point where the system can responsibly be described as entering a self-building phase, because planning, execution, review, and safety gates all exist as durable capabilities.

## In Scope

- bounded autonomous improvement proposals
- planner-generated artifact updates for proposed work
- controlled execution through implementation, review, and gate flows
- operator oversight and stop controls

## Out of Scope

- unrestricted self-directed roadmap changes
- fully unsupervised production-impacting changes
- removal of human approval and stop controls

## Work Sequence

1. Define what kinds of improvements the system may propose autonomously.
2. Connect planner, implementation, review, and gate flows into one bounded loop.
3. Add operator controls for approval, pause, and stop.
4. Define limits on scope, concurrency, and self-directed change volume.
5. Verify that the system can complete bounded improvements without bypassing approval and safety rules.

## Verification

This task is complete when:

- the system can originate bounded improvement proposals
- those proposals move through planning, execution, review, and gate workflows
- operator oversight remains effective throughout the loop
- the system improves the project without bypassing the control model
