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

- `TASK-001` prove workflow-to-Codex invocation with a simple structured result
- `TASK-002` persist execution state in PostgreSQL
- `TASK-003` expose task start/status through API and MCP
- `TASK-004` add workspace leasing for write-capable work
- `TASK-005` add provider-neutral change and review records
- `TASK-006` add planner artifact generation and approval flow
- `TASK-007` add implementation task execution and resume loop
- `TASK-008` add review feedback and rework loop
- `TASK-009` add verification and promotion gates
- `TASK-010` add autonomous project improvement loop
