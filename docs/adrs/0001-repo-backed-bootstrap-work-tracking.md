# ADR 0001: Repo-Backed Bootstrap Work Tracking

- Status: accepted
- Date: 2026-03-13

## Context

ConstructraOS has now defined its first real domain as SDLC agent-team orchestration for ConstructraOS itself. The first implementation slice needs durable records for project state, ADRs, tasks, bugs, branch state, and test evidence before a deeper tracker or graph-backed memory system exists.

The repo documentation and discovery interview already establish several constraints:

- the first source of truth should live in this repo
- the bootstrap layer should use markdown records on disk
- the project should be anchored by a dedicated folder with separate index files for ADRs, tasks, bugs, branches, and test evidence
- work records need to map task, branch, and responsible specialist so QA, SRE, and bug routing can operate on grounded state
- this bootstrap layer is expected to be replaceable later by a more durable tracking system

Without an explicit ADR, the filesystem contract would become an implicit architecture decision spread across docs and ad hoc files.

## Decision

ConstructraOS will use a repo-backed markdown work-tracking boundary as the first durable project-management substrate.

The bootstrap source of truth will live under `projects/constructraos/` and will include:

- a project record
- separate index files for ADRs, tasks, bugs, branches, and evidence
- markdown records for individual work items and related execution artifacts

This contract is a deliberate boundary, not a permanent storage choice. Early orchestration and specialist execution that need project state should read and update this boundary rather than inventing parallel representations.

## Non-Goals

This ADR does not decide:

- the long-term task or bug tracking system
- the long-term graph-store implementation
- the final workflow topology between project, task, and specialist workflows
- the final operator inbox implementation

## Consequences

Positive:

- the repo gains one grounded, inspectable source of truth for bootstrap project execution
- early QA, SRE, and PM flows can link tasks, branches, bugs, ADRs, and evidence without waiting for full database modeling
- replacement pressure stays localized behind a known boundary

Negative:

- markdown records can drift without strong conventions
- filesystem-backed links and indexes add maintenance overhead
- later migration to a durable tracker will require translation from this contract

## Follow-On

The next implementation step is to define the concrete filesystem contract under `projects/constructraos/` and seed it with the first project and task records.
