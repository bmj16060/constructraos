# Interview

## Date

2026-03-13

## Transcript

Q: What is the first real project you want this system to run?
A: The first project is itself.

Q: What is the first responsibility you want to hand off from you directly driving Codex to ConstructraOS managing the process?
A: Maybe testing and QA, but that implies some basic SRE capabilities as well since some testing things will need an environment to test against.

Q: What exact kind of testing should it own first?
A: QA should start above developer verification at the story and feature acceptance level, including browser-based acceptance testing.

Q: What environment should QA validate against first?
A: Local environment via Compose is likely good.

Q: When QA finds a problem during acceptance testing, what should happen first in the system?
A: Create a bug record and route it back to the responsible task or agent.

Q: How should the system know which task or agent is responsible for a bug?
A: There will need to be a branch, but we need some sort of record that maps branch to work and so on.

Q: What should the first managed control branch represent?
A: Given that this project is the first project, the whole project integration branch.

Q: What should cause work to be promoted into that project integration branch?
A: The PM agent accepts the work and I validate it as well.

Q: What are the first required definition-of-done checks?
A: We took a first stab and that felt like a good start.

Q: What should be the first specialist the system can actually spawn?
A: SRE seemed like the right starting point.

Q: Should QA only validate an already-running environment and file bugs, or should it also be allowed to request or trigger environment bring-up through the SRE specialist?
A: It needs to be able to request an environment.

Q: Which validation level should we implement first?
A: Specialist.

Q: Which specialist branch type should be the first one QA can validate?
A: SRE, since the code path is likely similar and the main difference is the specialist guidance.

Q: What is the first concrete SRE change you want this system to be able to deliver and validate on its own?
A: Bring up the local Compose stack, with enough configuration isolation to support multiples later and likely some ability to repair and make things healthy.

Q: What should identify one isolated environment in v1?
A: Branch name, though that name will likely tie back to task/work record or bug ID.

Q: Should an SRE task auto-execute immediately when QA needs an environment, or wait for approval?
A: It could auto-execute.

Q: What should QA use as the acceptance input for that branch?
A: We should take an industry-best-practice approach; a generated QA brief makes sense.

Q: What should QA produce as its output artifact?
A: A markdown validation report works.

Q: What should count as healthy enough for QA to start?
A: A smoke test passing would effectively cover the required signals.

Q: What should the first smoke test prove for ConstructraOS specifically?
A: The system should always have a hello-world workflow that can be exercised via the UI and validated by the result. The infra components should be health checked as well.

Q: What evidence should the system capture on a failed smoke or QA run?
A: All of the major evidence types depending on the nature of the failure.

Q: Where should clarification and approval conversations happen when you are not around?
A: We need a system that can queue up pending questions, approvals, and so on. Maybe that is exposed via MCP.

Q: What should make an inbox item high priority?
A: Risks data loss or creates a security issue, then blocks active execution, then clarification of architecture, infra, or coding.

Q: What should happen when an agent cannot ground a decision?
A: Escalate to PM, architect, or SRE, who can attempt to answer. It really should be the PM, architect, or SRE who can escalate to me.

Q: Should ambiguity about behavior, scope, and acceptance criteria escalate to PM first?
A: Yes.

Q: Should technical ambiguity escalate to architect first?
A: Yes.

Q: Should environment, deployment, or operational safety ambiguity escalate to SRE first?
A: Yes.

Q: What kinds of SRE actions should require your permission in v1?
A: Destroying data. Also anything outside the sandbox.

Q: What local actions should remain protected even in the Compose-first slice?
A: Deleting volumes or databases that are not test-related. We need to protect the running system if we can. SREs should ask before doing things to my larger system and should be restricted to Compose and its defined composition unless I approve otherwise.

Q: Should specialist work merge into the project integration branch automatically after acceptance?
A: It generally does not need my approval unless acceptance is questionable and feedback is needed.

Q: Where should specialist operating guidance live in v1?
A: Its own AGENTS.md is a good starting place. The canonical home should be docs/agents.

Q: Should the SRE role be defined first?
A: Yes.

Q: Should SRE be limited to specialist environments only?
A: No. SRE should be able to spin an environment at whatever branch level is requested.

Q: Should SRE reuse environments or always rebuild in v1?
A: Always rebuild.

Q: What should happen to the previous environment while a new one is being prepared?
A: Keep it around until smoke passes.

Q: What should happen if the new environment fails smoke validation?
A: Keep the old one.

Q: What should happen to the old environment after the new one passes smoke?
A: Delete it.

Q: What should be the bootstrap task ID shape?
A: A slug, though that reinforces the need to get off file-based management quickly.

Q: What should be replaced first after markdown bootstrap?
A: Task, bug, and ADR tracking first. Memory next. Inbox later because early work will stay fairly interactive through Codex.

Q: What are the non-negotiables for the first durable replacement?
A: Markdown-friendly content, links between ADRs, tasks, and bugs, and API access or eventing. An MCP server would be even more helpful.

Q: What is the first justified reason for a ConstructraOS boundary in front of an external system?
A: Lack of MCP in the system.

Q: What should the bootstrap MCP surface expose first?
A: List and fetch first, then create and update, then linking, then inbox.

Q: What should be the first record type supported by list and fetch?
A: Tasks first, and that ordering makes sense.

Q: What should the initial task status lifecycle be?
A: `draft -> ready -> in_progress -> blocked -> qa -> accepted -> merged -> done`

Q: What should move a task from qa to accepted?
A: PM verifies and marks it accepted.

Q: What should move a task from accepted to merged?
A: When all specialist branches are merged into the task’s parent branch.

Q: What should move a task from merged to done?
A: After the system updates the linked records and docs after merge.

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
