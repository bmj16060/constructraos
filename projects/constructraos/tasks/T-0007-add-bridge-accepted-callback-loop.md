# T-0007: Add bridge accepted callback loop

- Status: completed
- Owning specialist: backend
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: none

## Summary

Add the first bridge-owned callback path so a successful Codex thread start/resume and initial turn submission can signal execution acceptance back into the task workflow through the API.

## Scope

- align the workflow dispatch request metadata so the bridge sees the correct accepted callback signal name
- add bridge config and HTTP callback logic for posting accepted execution back to the API
- invoke the accepted callback after the app-server accepts the initial specialist turn
- cover the callback client with a focused HTTP test
- keep the existing live app-server integration and bridge/orchestration tests passing

## Definition Of Done

- [x] task workflow dispatch request carries `reportCodexExecutionAccepted` as the accepted callback signal
- [x] bridge can POST accepted execution payloads back to the ConstructraOS API when configured
- [x] accepted callback fires only after the initial turn is submitted successfully
- [x] targeted codex-bridge and orchestration tests pass

## Notes

This is the first real bridge-to-workflow progress loop, but it is still only the accepted-execution milestone. The bridge does not yet translate completed Codex turn output into `reportSreEnvironmentOutcome`, and it still declines app-server-driven tool and approval requests.
