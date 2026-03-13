# T-0003: Add QA to SRE task handoff signals

- Status: completed
- Owning specialist: backend
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: [E-0024](/var/lib/constructraos/projects/constructraos/evidence/E-0024-t-0003-qa-request.md)

## Summary

Extend the long-running task coordination workflow so a QA request can place the task into a waiting-on-SRE state and an SRE outcome signal can move it forward with durable evidence.

## Scope

- generalize project-record evidence writes so both QA and SRE events use the same boundary
- add SRE environment outcome signal contracts
- add an API path for reporting SRE environment outcomes back into the task workflow
- update task workflow state to expose waiting-on and environment-readiness details
- enforce workflow-side policy for the SRE outcome signal

## Definition Of Done

- [x] QA request signals place the task workflow into a waiting-on-SRE state
- [x] SRE environment outcome signals are accepted by the long-running task workflow
- [x] SRE outcomes write evidence through the repo-backed project-records boundary
- [x] workflow query state exposes environment status and environment identity
- [x] targeted tests cover the new controller, workflow, and project-record behavior

## Notes

This still stops short of running Compose directly. The next expansion is to produce the SRE outcome signal from real SRE execution rather than an external caller.
