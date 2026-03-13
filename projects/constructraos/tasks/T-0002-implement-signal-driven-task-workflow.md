# T-0002: Implement signal-driven task workflow slice

- Status: completed
- Owning specialist: backend
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: none

## Summary

Implement the first long-running Temporal task workflow against the repo-backed project contract, using signals and query semantics instead of a one-shot workflow.

## Scope

- add a dedicated project-records library for filesystem-backed task, branch, and evidence access
- add shared task-workflow contracts in `libraries/commons`
- expose an API path that uses `signalWithStart` to request QA on a task workflow
- enforce auth at ingress and business-rule policy inside workflow signal handling
- write QA-request evidence into the project contract from workflow activities

## Definition Of Done

- [x] task workflow is long-running and signal-driven
- [x] API uses `signalWithStart` rather than synchronous workflow runs
- [x] workflow state is queryable
- [x] QA request signals write evidence through the project-records boundary
- [x] policy is enforced at ingress for auth and inside the workflow for signal processing
- [x] targeted tests cover the new library, API controller, and workflow behavior

## Notes

This slice intentionally stops at QA request + evidence capture. Actual SRE environment bring-up and follow-on outcome signaling are the next expansion.
