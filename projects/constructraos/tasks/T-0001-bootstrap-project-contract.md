# T-0001: Bootstrap project filesystem contract

- Status: in_progress
- Owning specialist: PM
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: [E-0001](/var/lib/constructraos/projects/constructraos/evidence/E-0001-t-0001-qa-request.md), [E-0002](/var/lib/constructraos/projects/constructraos/evidence/E-0002-t-0001-qa-request.md), [E-0003](/var/lib/constructraos/projects/constructraos/evidence/E-0003-t-0001-qa-request.md), [E-0004](/var/lib/constructraos/projects/constructraos/evidence/E-0004-t-0001-sre-environment-outcome.md), [E-0005](/var/lib/constructraos/projects/constructraos/evidence/E-0005-t-0001-qa-request.md), [E-0006](/var/lib/constructraos/projects/constructraos/evidence/E-0006-t-0001-sre-environment-outcome.md), [E-0007](/var/lib/constructraos/projects/constructraos/evidence/E-0007-t-0001-qa-request.md), [E-0008](/var/lib/constructraos/projects/constructraos/evidence/E-0008-t-0001-qa-request.md), [E-0009](/var/lib/constructraos/projects/constructraos/evidence/E-0009-t-0001-qa-request.md), [E-0010](/var/lib/constructraos/projects/constructraos/evidence/E-0010-t-0001-sre-environment-outcome.md), [E-0011](/var/lib/constructraos/projects/constructraos/evidence/E-0011-t-0001-qa-request.md), [E-0012](/var/lib/constructraos/projects/constructraos/evidence/E-0012-t-0001-sre-environment-outcome.md), [E-0013](/var/lib/constructraos/projects/constructraos/evidence/E-0013-t-0001-qa-request.md), [E-0014](/var/lib/constructraos/projects/constructraos/evidence/E-0014-t-0001-sre-environment-outcome.md), [E-0015](/var/lib/constructraos/projects/constructraos/evidence/E-0015-t-0001-qa-request.md), [E-0016](/var/lib/constructraos/projects/constructraos/evidence/E-0016-t-0001-qa-request.md), [E-0017](/var/lib/constructraos/projects/constructraos/evidence/E-0017-t-0001-sre-environment-outcome.md), [E-0018](/var/lib/constructraos/projects/constructraos/evidence/E-0018-t-0001-qa-request.md), [E-0019](/var/lib/constructraos/projects/constructraos/evidence/E-0019-t-0001-qa-request.md), [E-0020](/var/lib/constructraos/projects/constructraos/evidence/E-0020-t-0001-sre-environment-outcome.md), [E-0021](/var/lib/constructraos/projects/constructraos/evidence/E-0021-t-0001-qa-request.md), [E-0022](/var/lib/constructraos/projects/constructraos/evidence/E-0022-t-0001-sre-environment-outcome.md), [E-0023](/var/lib/constructraos/projects/constructraos/evidence/E-0023-t-0001-qa-request.md)

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
- [x] first specialist guidance is present
- [x] status docs reflect the repo-backed contract as the active baseline

## Notes

This task exists so the project contract is self-hosting from its first introduction.
