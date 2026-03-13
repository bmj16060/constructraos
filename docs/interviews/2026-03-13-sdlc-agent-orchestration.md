# Interview

## Date

2026-03-13

## Problem Summary

ConstructraOS is being repurposed into a system for running and managing agent teams across a full software delivery lifecycle. The platform should bootstrap itself to the point where it can increasingly take over project execution, with Codex as a key execution surface through the CLI and potentially the Codex CLI MCP server.

## Users / Operators

- Primary initial operator: Brandon
- Initial role mix: product manager and primary architect
- Expected interaction model: operate the system through a Codex project and threads that talk to ConstructraOS through MCP-connected capabilities

## Domain Vocabulary

- project workflow
- task workflow
- bug
- story / slice
- specialist agent
- PM agent
- architect agent
- backend agent
- frontend agent
- QA agent
- SRE specialist
- ADR
- project memory
- graph database

## First Workflow Slice

Bootstrap the system with filesystem-backed markdown artifacts so the platform can:

1. define a project and its ADR-backed intent
2. decompose work into tasks and bugs
3. spawn task-level specialist execution
4. track git branching and testing as part of delivery state
5. report task outcomes back to the project workflow

The first release should be capable of spawning specialists rather than only recording plans. Specialists will require their own prompts and operating guidance. An SRE specialist is part of the initial specialist set so the platform can manage its own infrastructure and deployment paths over time.

### Additional Interview Findings

- The first project is ConstructraOS itself. The system should bootstrap itself to the point where it can increasingly run ConstructraOS.
- The initial operating style is interactive through this Codex project/thread model, with responsibilities shifting gradually into the actual system rather than through a single cutover.
- The first responsibility to hand off is QA-style acceptance validation, not low-level developer test execution.
- QA in this system starts at story or feature acceptance against a running environment, including browser-based acceptance behavior where appropriate.
- The first QA target environment is local and Compose-backed.
- When QA finds an issue, the system should create a bug record and route it back to the responsible work item.
- Work records need to map task <-> branch <-> responsible specialist so bugs and validation can be routed correctly.
- Branching is hierarchical rather than flat: a parent task or project branch can collect work from multiple specialist branches before merging upward.
- `main` should stay protected as the trusted baseline. The first managed control branch should represent the whole project integration state.
- Promotion into the project integration branch should normally be driven by PM acceptance plus operator validation where needed, without making every merge require manual approval.
- The bootstrap definition of done should include current task state, developer verification, QA acceptance, current documentation, and bug disposition.

## First Durable Data Slice

Store the first domain artifacts as markdown files on the filesystem, with just enough structure to bootstrap the system:

- project records
- ADRs
- task and bug records
- specialist execution notes
- git branch state
- test evidence and results

These filesystem artifacts are a bootstrap mechanism, not the final storage model.

The first durable layout should live inside this repo under a dedicated project folder with separate index files for ADRs, tasks, bugs, branches, and test evidence.

## Key Constraints

- Preserve the baseline platform spine: API, orchestration, PostgreSQL, Valkey, tracing, policy/OPA, and UI shell.
- Use markdown files on the filesystem for the first slice where practical.
- Codex should be a first-class part of the execution model.
- Specialists need distinct prompts and guidance.
- Project memory should become durable and queryable through a graph database.
- Graph access should be exposed through MCP and kept behind a deliberate service/library boundary.
- Markdown tracking is explicitly temporary and should sit behind a replaceable boundary so the system can move quickly to a more durable tracker.
- The system should reduce ungrounded assumptions. Agents may proceed only when grounded in code/tests, project/task artifacts, repo docs/ADRs, validated evidence, or prior explicit operator decisions.
- When an execution specialist cannot ground a decision, it should escalate first to PM, architect, or SRE before surfacing the issue to the human operator.
- PM handles behavior, scope, and acceptance ambiguity.
- Architect handles technical architecture and implementation ambiguity.
- SRE handles environment, deployment, and operational safety ambiguity.
- Guidance does not imply permission. Risky actions still require operator approval.
- Destructive local data actions and any operation outside the local Compose-defined scope require operator approval.

## Integrations

- Codex CLI
- possible Codex CLI MCP server
- local filesystem for bootstrap state
- future graph database exposed through MCP
- git repositories / branching workflows
- test execution commands

## Success Criteria

- A project can be represented durably enough to bootstrap execution.
- The system can spawn at least one specialist agent from a project/task context.
- Task execution can track bugs, branch state, and testing status.
- ADRs exist as first-class project artifacts from the start.
- The architecture keeps room for later migration from filesystem-backed artifacts to deeper platform persistence and graph-backed project memory.

## Initial Specialist and Environment Model

- The first explicitly defined specialist role should be `SRE`.
- Specialist guidance should live under `docs/agents/`, starting with SRE guidance.
- SRE should own environment lifecycle at whatever branch level the workflow requests.
- Environment identity should be anchored on branch name, while branch naming links back to the work record.
- When QA needs an environment, it should be able to trigger an SRE task that auto-executes.
- SRE should always rebuild the requested branch environment from scratch before QA uses it.
- The previous environment for the same branch should remain in place until the new environment passes smoke validation.
- If the new environment fails smoke validation, keep the old environment and route remediation through SRE.
- If the new environment passes smoke validation, delete the old environment automatically.
- Smoke readiness should include infrastructure health plus successful execution of the built-in `hello-world` workflow via the UI.

## Initial Operator Interaction Model

- The system should not assume the human operator is always present in-thread.
- The platform needs an asynchronous operator inbox or decision backlog for clarifications, approvals, and triage items.
- Inbox handling is priority-based rather than FIFO.
- Highest priority items are security/data-loss risks, then active execution blockers, then architecture/infrastructure/coding clarifications.
- Work should pause only on the dependent path. Agents may continue on grounded, non-conflicting work.
- The inbox can stay lightweight in the earliest phase because the human operator will still work interactively through Codex.

## Initial Replacement Priorities

- Replace markdown-backed task, bug, and ADR tracking first.
- Replace durable project memory next.
- Keep the operator inbox lighter for longer because early interaction will remain Codex-centric.
- The first durable replacement should strongly support markdown-friendly content, links between ADRs/tasks/bugs, and API/eventing, ideally with MCP compatibility.
- For the markdown bootstrap phase, exposing repo-backed work tracking through MCP is acceptable as a thin temporary boundary, but it should not evolve into a heavy bespoke tracker.
