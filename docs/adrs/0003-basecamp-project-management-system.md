# ADR 0003: Basecamp As Project Management System

- Status: accepted
- Date: 2026-03-13

## Context

ConstructraOS already has an accepted bootstrap decision to keep early project execution records in repo-backed markdown under `projects/constructraos/`. That boundary exists so the platform can start operating on durable project state before a deeper project-management system is chosen and integrated.

The long-term project-management decision has now been made for this project:

- project planning and coordination need a deliberate external system rather than an indefinitely extended markdown bootstrap
- the team wants an existing product instead of inventing a custom project-management tool inside the platform
- early repo-backed markdown records are still useful as a temporary execution boundary while the Basecamp-facing operating model is defined and implemented

Without recording this decision, repo docs would continue to imply that the long-term project-management substrate is still undecided.

## Decision

ConstructraOS will use Basecamp as the project-management system for project planning, coordination, and related work tracking.

Until the Basecamp operating model is implemented, the existing repo-backed markdown contract under `projects/constructraos/` remains the active bootstrap boundary for work records and execution artifacts inside this repo.

New implementation work should treat that markdown layer as transitional and avoid designing it as the permanent project-management model.

## Non-Goals

This ADR does not decide:

- the exact Basecamp structure for projects, to-dos, schedules, or message threads
- the first API or synchronization model between ConstructraOS and Basecamp
- whether every execution artifact should be mirrored into Basecamp or remain in repo/object storage boundaries
- the timing for retiring the markdown bootstrap contract

## Consequences

Positive:

- the project now has a clear long-term project-management direction
- future platform work can design around a known external PM system instead of keeping that decision open
- the markdown bootstrap layer stays explicitly temporary

Negative:

- ConstructraOS now has an external product dependency for project-management workflows
- project state may temporarily exist in both repo markdown and Basecamp until the transition boundary is implemented
- Basecamp integration and operating conventions become follow-on work that must be defined deliberately

## Follow-On

The next follow-on decisions should define:

1. which project-management records remain in repo markdown versus move to Basecamp first
2. how ConstructraOS identifies and links Basecamp records to internal tasks, branches, bugs, and evidence
3. what minimum Basecamp integration or operator workflow is needed before the markdown bootstrap layer can shrink
