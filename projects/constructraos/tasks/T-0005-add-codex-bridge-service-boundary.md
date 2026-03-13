# T-0005: Add codex bridge service boundary

- Status: completed
- Owning specialist: backend
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: none

## Summary

Add a dedicated `codex-bridge` service boundary so orchestration talks to a compose-managed bridge, and the bridge owns the future host-local Codex App Server conversation logic.

## Scope

- add a new `services/codex-bridge` Micronaut service
- route orchestration Codex dispatch through the bridge over HTTP
- configure Compose and Docker builds for the new service
- move the Codex App Server client placeholder into the bridge service
- keep the current workflow contract stable while making the bridge boundary explicit

## Definition Of Done

- [x] `codex-bridge` exists as a buildable service
- [x] orchestration dispatches Codex work through the bridge service boundary
- [x] Compose includes the bridge service
- [x] root docker build includes the bridge image
- [x] targeted codex-bridge and orchestration tests pass

## Notes

This still uses a placeholder Codex App Server client. The next expansion is replacing that placeholder with a real `thread/start` and `thread/resume` protocol client against the host-local `codex app-server`.
