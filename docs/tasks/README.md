# Tasks

Use this folder for execution planning and milestone breakdowns that should not live in ADRs.

Task documents should capture:

- the concrete milestone being implemented
- the sequence of work
- what is intentionally out of scope
- how the milestone will be verified

Keep ADRs focused on durable architectural decisions and boundaries.
Keep task documents focused on execution order and delivery slices.

Current planned sequence:

- `TASK-000` rename `libraries/orchestration-clients` to `libraries/clients`
- `TASK-001` prove workflow-to-Codex invocation with a simple structured result
- `TASK-001A` replace the host-dependent Codex CLI path with a containerized runtime boundary
- `TASK-002` persist execution state in PostgreSQL
- `TASK-003` expose task start/status through API and MCP
- `TASK-003A` add a basic secondary task console page in the UI shell
- `TASK-003B` add an app-server-backed Temporal runtime with child tasks under `docs/tasks/TASK-003B/`
- `TASK-004` add workspace leasing for write-capable work
- `TASK-005` add provider-neutral change and review records
- `TASK-006` add planner artifact generation and approval flow
- `TASK-007` add implementation task execution and resume loop
- `TASK-008` add review feedback and rework loop
- `TASK-009` add verification and promotion gates
- `TASK-010` add autonomous project improvement loop
