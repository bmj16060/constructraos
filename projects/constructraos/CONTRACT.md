# ConstructraOS Bootstrap Contract

This document defines the first durable filesystem contract for ConstructraOS project execution.

## Scope

The bootstrap contract covers:

- project record
- ADR index and links
- task records
- bug records
- branch state
- test and QA evidence
- environment lifecycle records
- specialist execution requests

## Design Rules

- Markdown is the source format.
- Records must be linkable by stable IDs.
- Tasks are the primary work record.
- Tasks must map task <-> branch <-> responsible specialist.
- ADRs provide intent and constraints, but they do not replace task or evidence records.
- Bugs are recorded at the branch where they are observed, but the canonical bug should live at the highest branch level where the issue is confirmed.
- Evidence records capture what was validated, against which branch and environment, and with what result.
- Specialist execution requests capture durable work handoff from Temporal to Codex, including execution request ID, specialist role, callback metadata, and attached Codex thread identity when known.
- This contract is replaceable. New code should treat it as a boundary rather than scattering equivalent state elsewhere.

## Layout

```text
projects/constructraos/
  project.md
  CONTRACT.md
  adrs/
    index.md
  tasks/
    index.md
    T-0001-bootstrap-project-contract.md
  bugs/
    index.md
  branches/
    index.md
  environments/
    index.md
  evidence/
    index.md
  executions/
    index.md
```

## ID Conventions

- Tasks: `T-####`
- Bugs: `B-####`
- ADRs: `ADR-####`
- Evidence: `E-####`

The file name should start with the same ID.

## Minimum Task Record Fields

- task ID
- title
- status
- summary
- owning specialist
- parent branch or control branch
- related specialist branches
- linked ADRs
- linked bugs
- definition-of-done checklist
- latest evidence links

## Minimum Bug Record Fields

- bug ID
- title
- status
- branch where observed
- highest branch where confirmed
- linked task
- linked evidence
- disposition

## Minimum Evidence Record Fields

- evidence ID
- task or bug under validation
- branch tested
- environment identifier
- validating specialist
- result
- executed checks
- relevant logs, screenshots, or notes

## Minimum Environment Record Fields

- environment ID
- linked task
- branch
- environment name
- kubernetes namespace or equivalent runtime scope
- ownership scope such as `task-team` or `execution`
- lifecycle status
- protected flag
- last active timestamp
- retirement basis such as `retire_after`
- operator or workflow notes

## Minimum Branch Index Fields

- branch name
- branch role
- linked task or project scope
- environment expectation
- current status

## Minimum Execution Request Fields

- execution request ID
- linked task
- specialist role
- target branch or workspace
- status
- workflow ID
- callback signal metadata
- Codex thread ID when assigned

## Definition Of Done Baseline

The bootstrap definition of done for a normal task is:

1. The task record is current, including scope, decisions, branch links, and owning specialist.
2. The responsible specialist recorded developer verification or implementation checks.
3. QA acceptance passed for the relevant branch level.
4. Documentation is current when the change affects ADRs, task notes, or operator-facing usage.
5. Open bugs are fixed or explicitly linked back to the task with disposition.
