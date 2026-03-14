# TASK-009: Verification and Promotion Gates

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)
- [TASK-008: Review and Rework Loop](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/tasks/TASK-008-review-and-rework-loop.md)

## Goal

Add the safety gates required before the system can move accepted work toward merge or promotion.

## Why This Next

Without explicit verification and promotion gates, the system may be able to write and review code but still cannot be trusted to advance changes safely.

## In Scope

- build and test verification states
- policy and approval gates
- required checks before merge or promotion
- durable gate results that can block or allow progression

## Out of Scope

- fully autonomous merge and deploy in all environments
- production release automation beyond the first gated path

## Work Sequence

1. Define the required verification and approval gates for a change.
2. Identify which gates are deterministic policy candidates and express those through the policy boundary.
3. Add durable records for gate execution and outcomes.
4. Implement workflow routing based on gate pass/fail/block results.
5. Connect review acceptance to the verification and promotion path.
6. Verify that unsafe or unverified changes cannot progress automatically.

## Verification

This task is complete when:

- accepted changes must pass explicit gates before promotion
- gate results are durable and inspectable
- failed or missing checks block automatic progression
- the promotion path reflects the intended safety controls
- deterministic gating rules are evaluated through the policy boundary rather than embedded ad hoc in Java
