# T-0001: Bootstrap project filesystem contract

- Status: in_progress
- Owning specialist: PM
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: none

## Summary

Create the first durable repo-backed contract for project, ADR, task, bug, branch, and evidence records so later orchestration can operate on grounded state.

## Scope

- establish ADR convention
- add the initial project record
- add indexes for ADRs, tasks, bugs, branches, and evidence
- define the bootstrap filesystem contract
- seed first specialist guidance

## Definition Of Done

- [x] ADR for the bootstrap tracking boundary is accepted
- [x] `projects/constructraos/` exists with project record and indexes
- [x] bootstrap contract is documented
- [ ] first specialist guidance is present
- [ ] status docs reflect the repo-backed contract as the active baseline

## Notes

This task exists so the project contract is self-hosting from its first introduction.
