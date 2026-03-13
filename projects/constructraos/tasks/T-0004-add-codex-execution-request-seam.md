# T-0004: Add Codex execution request seam

- Status: completed
- Owning specialist: backend
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: none

## Summary

Pivot the SRE execution path away from container-managed Compose and toward a Codex-mediated execution seam, where the workflow dispatches durable specialist execution requests and Codex signals progress back.

## Scope

- add shared Codex dispatch request and callback signal contracts
- make execution requests first-class durable project records
- update task workflow state to track execution request ID and Codex thread ID
- dispatch Codex work through a short-lived activity instead of attempting to run Compose inside the worker container
- add the first Codex acceptance callback path back into the workflow

## Definition Of Done

- [x] workflow dispatches a durable execution request for Codex to consume
- [x] task workflow state exposes execution request ID and Codex thread ID
- [x] Codex can report execution acceptance back into the workflow
- [x] specialist execution requests are represented in the repo-backed project contract
- [x] targeted tests cover the new project-record and workflow/controller paths

## Notes

This slice now includes the first atomic claim path for Codex threads through the API. The next expansion is the true MCP/App-Server-facing consumer and richer progress/completion callbacks.
