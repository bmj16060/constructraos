# T-0008: Add bridge turn outcome fallback callback

- Status: completed
- Owning specialist: backend
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
- Linked bugs: none
- Latest evidence: none

## Summary

Teach the `codex-bridge` to watch the live `codex app-server` turn lifecycle and post a fallback SRE environment outcome back into the task workflow when a specialist turn reaches a terminal state without a durable tool-driven callback.

## Scope

- extend the websocket session boundary so the bridge can observe turn-completion notifications and last-agent-message payloads
- add bridge callback support for posting `reportSreEnvironmentOutcome` back into the API
- keep the accepted callback behavior intact while handing off in-progress turn monitoring asynchronously
- preserve the workflow's existing environment name when the fallback callback omits one
- cover the callback path with focused bridge and workflow tests while keeping the live app-server integration harness passing

## Definition Of Done

- [x] bridge captures terminal turn notifications from the live app-server websocket session
- [x] bridge can POST a fallback `reportSreEnvironmentOutcome` payload back into the ConstructraOS API
- [x] in-progress turns can be monitored after dispatch returns instead of requiring the dispatch HTTP request to stay open
- [x] task workflow no longer clears the tracked environment name when an outcome signal omits it
- [x] targeted `codex-bridge` and orchestration tests pass

## Notes

This is still a fallback path, not a replacement for explicit workflow MCP tools. The bridge currently reports terminal turns back into the workflow as failed environment outcomes when no durable tool-driven callback arrived, which keeps the QA -> SRE loop from stalling while dynamic tool-call support is still unavailable on the bridge boundary.
