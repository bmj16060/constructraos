# T-0006: Implement Codex App Server conversation client

- Status: completed
- Owning specialist: backend
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: none

## Summary

Replace the placeholder bridge conversation client with a real `codex app-server` protocol client that can initialize a session, start or resume a Codex thread, and submit the first turn for a specialist execution request.

## Scope

- extend the dispatch contract so the bridge can resume an existing Codex thread when one is already known
- implement the live JSON-RPC session flow against `codex app-server`
- resolve a branch workspace path for the Codex thread context
- keep unsupported app-server tool and approval callbacks from hanging the session by declining them explicitly
- cover the start, resume, and failed-turn paths with focused bridge tests

## Definition Of Done

- [x] bridge dispatch performs `initialize`, `thread/start` or `thread/resume`, and `turn/start`
- [x] dispatch returns the Codex thread ID when the app-server accepts the request
- [x] execution dispatch can carry an existing Codex thread ID for resume-aware follow-up work
- [x] unsupported app-server callback requests are declined instead of silently deadlocking
- [x] targeted codex-bridge and orchestration tests pass, including a live app-server integration test for thread start and resume

## Notes

This does not make the bridge a full execution host yet. The current bridge can create or resume the durable Codex thread and submit the initial specialist turn, but it still declines app-server-driven command approvals and dynamic tool calls until the host-local execution boundary is implemented.
The live integration test also exposed a real rollout-persistence race on immediate resume, so the test now waits for the non-empty rollout artifact before asserting the resume path.
