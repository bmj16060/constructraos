# TASK-001: Codex Invocation Vertical Slice

Status: Planned

Date: 2026-03-14

Related:

- [ADR-001: Codex Orchestration Boundaries](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/decisions/ADR-001-codex-orchestration-boundaries.md)

## Goal

Prove the narrowest closed loop for the orchestration system:

`Temporal workflow -> Codex execution activity -> simple structured response`

This milestone intentionally stops before database-backed orchestration state becomes required.
This task is specifically about proving one atomic Codex interaction, not about proving every higher-level workflow step can be completed in a single turn.

## Why This First

The highest-risk integration seam is whether a workflow can reliably invoke Codex through the execution boundary and receive a schema-valid result back.

If that seam is not solid, persistence and richer domain modeling are premature.

## In Scope

- one Temporal workflow that invokes one Codex execution activity
- one minimal execution adapter contract
- one minimal structured result schema
- one atomic Codex interaction for the first execution path
- shared workflow and activity contracts added through `libraries/commons`
- a typed workflow client path added through `libraries/orchestration-clients`
- verification through Temporal workflow result, activity output, and logs

## Out of Scope

- PostgreSQL-backed task and transcript persistence
- workspace leasing records
- multi-agent routing
- human question flows
- provider-backed review flows
- UI or MCP operator surfaces beyond what is strictly needed to trigger the workflow

## Minimal Result Contract

The first result shape should stay small:

```json
{
  "status": "completed | blocked | failed",
  "summary": "string",
  "recommended_next_agent": "string | none"
}
```

Later milestones can add:

- `human_question`
- session continuity metadata
- transcript references
- richer routing fields

Later milestones may also allow a single workflow-managed step to span multiple Codex turns. That is intentionally out of scope for this first proof point.

## Work Sequence

1. Define the minimal workflow input and output contracts in `libraries/commons`.
2. Define the first Codex execution adapter contract.
3. Add a typed workflow client adapter in `libraries/orchestration-clients`.
4. Implement one Temporal workflow that performs a single execution step.
5. Implement one activity that calls the execution adapter.
6. Run the workflow against a real local Codex invocation path.
7. Confirm the workflow receives and returns the structured result.

## Follow-On Milestone

After this task is proven, the next task should add durable persistence for:

- projects
- tasks
- task steps
- agent sessions
- transcript records
- structured execution results

That follow-on should make orchestration state queryable outside Temporal rather than using Temporal as the only verification surface.

## Verification

This task is complete when:

- the workflow successfully invokes Codex through the execution boundary
- the activity returns a schema-valid structured result
- the workflow exposes that result without requiring database persistence
- the integration is verified through Temporal execution state and logs
