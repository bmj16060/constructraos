# Project: ConstructraOS

- Project ID: P-0001
- Status: active
- Control branch: `project/constructraos/integration`
- Trusted baseline branch: `main`
- Bootstrap contract ADR: [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)

## Purpose

Bootstrap ConstructraOS so it can increasingly run ConstructraOS through project, task, specialist, branch, and validation workflows.

## Current Focus

- define the bootstrap repo-backed work-tracking boundary
- seed the first project records and specialist guidance
- prepare for the first QA -> SRE execution slice on specialist branches

## Indexes

- [ADRs](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos/adrs/index.md)
- [Tasks](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos/tasks/index.md)
- [Bugs](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos/bugs/index.md)
- [Branches](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos/branches/index.md)
- [Evidence](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos/evidence/index.md)
- [Execution Requests](/Users/brandonjohnson/SourceCode/ConstructraOS/projects/constructraos/executions/index.md)

## Initial Execution Model

- Tasks are the primary execution record.
- QA is the first handoff responsibility.
- QA can escalate to SRE to create or repair the requested environment.
- SRE owns environment lifecycle for the branch level being validated.
- Specialist validation starts at the specialist-branch level before project-integration validation.
