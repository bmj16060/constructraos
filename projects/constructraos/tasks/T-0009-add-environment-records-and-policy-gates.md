# T-0009: Add environment records and policy gates

- Status: completed
- Owning specialist: backend
- Parent control branch: `project/constructraos/integration`
- Specialist branches: none yet
- Linked ADRs:
  - [ADR-0001](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0001-repo-backed-bootstrap-work-tracking.md)
  - [ADR-0002](/Users/brandonjohnson/SourceCode/ConstructraOS/docs/adrs/0002-kubernetes-execution-storage.md)
- Linked bugs: none
- Latest evidence: none

## Summary

Add first-class environment lifecycle records to the repo-backed project contract and introduce workflow policy actions for environment launch, reuse, retirement, and deletion so namespace management starts from durable state and explicit policy checks.

## Scope

- add `environments/` to the project-records contract with markdown records and an index
- extend the project-records gateway and workflow activities to read, list, and upsert environment records
- add workflow-side policy actions for environment lifecycle transitions
- record task-team namespace ownership in the task workflow during QA request and SRE outcome handling
- expose the current workflow namespace through task workflow state

## Definition Of Done

- [x] environment records are persisted behind the shared project-records boundary
- [x] environment lifecycle actions have explicit Rego policy entries and tests
- [x] task workflow writes environment records before and after SRE outcomes
- [x] task workflow state includes the namespace it believes it owns
- [x] targeted gateway, API, and orchestration tests pass

## Notes

This does not launch Kubernetes namespaces yet. It establishes the durable environment control plane first so later namespace creation, reuse, retirement, and cleanup logic can be policy-driven from the start instead of hidden inside workflow code.
