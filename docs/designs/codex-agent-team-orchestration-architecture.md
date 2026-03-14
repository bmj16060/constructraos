# Codex Agent Team Orchestration Architecture

## Overview

This document describes an architecture for coordinating a team of AI agents using the Codex CLI while driving the system from an interactive Codex session through an MCP server.

The design emphasizes:

- Deterministic orchestration
- Structured outputs
- Reusable Codex sessions
- Isolated worker turns
- Full transcript observability
- Minimal context passing
- Interactive control from Codex via MCP
- Human-in-the-loop consultation
- Project-aware orchestration based on the user's working directory
- Planning artifacts as first-class system outputs
- Deterministic rule evaluation through policy boundaries rather than ad hoc Java logic

The system uses a **coordinator + sidecar pattern** where Codex agents run as isolated worker turns via:

    codex exec --output-schema

and resume prior sessions using:

    codex exec resume <session_id>

An MCP server exposes the orchestration system to an interactive Codex session so Codex can start tasks, run steps, inspect state, answer questions, and review transcripts.

---

# High-Level Architecture

    Interactive Codex Session
            |
            v
        MCP Server
            |
            v
        Coordinator
            |
            v
     Codex Sidecar Runner
            |
            v
codex exec / codex exec resume
|
v
Worker Agents

---

# Core Concepts

## Project

The system treats the interactive user's **current working directory** as the active project.

Each unique directory is registered with the coordinator and assigned a **project ID**.

Example:

    {
      "project_id": "PROJ-12",
      "name": "aviation",
      "root_path": "/Users/brandonjohnson/SourceCode/Aviation"
    }

All orchestration state is scoped to a project.

Project-scoped entities include:

- tasks
- consultations
- human questions
- transcripts
- Codex session IDs
- workflow policies

---

# Core Components

## 1. Interactive Codex Session

The interactive Codex session is the human-facing interface.

Responsibilities:

- Accept operator instructions
- Call MCP tools
- Display orchestration results
- Surface pending human questions
- Inspect transcripts
- Summarize project state

The interactive session does **not implement orchestration logic**.

All orchestration is handled by the MCP server and coordinator.

---

## 2. MCP Server

The MCP server provides the structured tool interface between Codex and the orchestration runtime.

Responsibilities:

- Expose orchestration tools
- Detect the caller's working directory
- Resolve the current project
- Validate tool inputs
- Forward requests to the coordinator
- Return structured responses

The MCP surface should remain **coarse-grained**, allowing the coordinator to manage the workflow.

---

## 3. Coordinator

The coordinator is the orchestration control plane.

Responsibilities:

- Project registration
- Task lifecycle management
- Agent routing
- Consultation management
- Human consultation management
- Session tracking
- Transcript storage
- History summarization
- Retry and escalation policies
- Main orchestration loop

The coordinator **never runs Codex directly**.

It invokes the sidecar runner to execute Codex workers.

---

## 4. Codex Sidecar

The sidecar wraps the Codex CLI.

Responsibilities:

- Execute Codex workers
- Resume prior Codex sessions
- Enforce output schemas
- Capture JSONL transcript streams
- Persist artifacts
- Return structured results

Example invocation:

    codex exec \
      --json \
      --output-schema /schemas/reviewer_result.json \
      - < prompt.txt

Resume prior session:

    codex exec resume <session_id> \
      --json \
      --output-schema /schemas/reviewer_result.json \
      - < prompt.txt

---

# Agent Model

Agents represent specialized roles.

Example agents:

- planner
- implementer
- reviewer
- tester
- bugfixer
- docs
- architect

Each agent:

- performs a bounded task
- runs in a single Codex execution turn
- returns structured output

Agents **never invoke each other directly**.

The coordinator handles all routing.

The `planner` role is not limited to routing recommendations. It can also be used to turn an initial concept into durable planning artifacts such as:

- design notes
- ADR proposals
- task breakdown documents

Those artifacts should be treated as part of the system's intended output surface, not just as external operator workflow.

---

# Planning Artifact Capability

An important follow-on capability is for the system to help formalize work before implementation begins.

Target behavior:

1. operator introduces a concept or change
2. planner agent analyzes the concept
3. system proposes or updates:
   - a design note when the shape is still exploratory
   - an ADR when durable boundaries or decisions are ready
   - task documents when the work can be sequenced into delivery slices
4. operator reviews and approves those artifacts
5. implementation work proceeds from the approved task ladder

This capability is not a required first implementation milestone, but it is part of the intended product shape because it lets the system participate in its own planning loop.

---

# Structured Output Contract

Agents return structured results.

Example schema:

    {
      "status": "completed | blocked | failed",
      "summary": "string",
      "issues": ["string"],
      "recommended_next_agent": "planner | implementer | reviewer | tester | bugfixer | docs | none",
      "human_question": {
        "question": "string",
        "reason": "string",
        "suggested_options": ["string"]
      }
    }

Example output:

    {
      "status": "completed",
      "summary": "Implemented authz middleware and added unit tests.",
      "issues": [],
      "recommended_next_agent": "reviewer"
    }

---

# Policy Boundary

Deterministic business rules should not be delegated to Codex reasoning or scattered through orchestration code.

Expected rule split:

- Codex agents
  - planning
  - implementation reasoning
  - review reasoning
  - summarization and proposal generation
- Policy boundary
  - authorization decisions
  - deterministic business rules
  - promotion and gating decisions that can be expressed as policy

In ConstructraOS, those deterministic rules should be evaluated through the existing policy boundary rather than codified ad hoc in Java services.

That means:

- reusable deterministic decisions belong in `policy-service` and OPA
- workflows should call policy evaluation activities when a rule affects orchestration
- Java should coordinate policy evaluation and apply decisions, not become the long-term home of business policy

---

# Transcript Handling

Codex can emit a JSONL event stream with:

    --json

Example fragment:

    {"type":"message","role":"assistant","content":"Analyzing ADR-027."}
    {"type":"tool.call","tool":"repo_read"}
    {"type":"tool.result","tool":"repo_read"}
    {"type":"structured_output","value":{...}}

Artifacts are stored per project:

    runs/
      PROJ-12/
        TASK-184/
          implementer/
            turn-1.jsonl
            result.json
          reviewer/
            turn-2.jsonl
            result.json

The coordinator uses only structured results for routing.

---

# Session Continuity

Codex sessions can be resumed:

    codex exec resume <session_id>

Example:

    {
      "session_id": "abc123",
      "result": {...}
    }

Coordinator rules:

Resume when:

- same agent continues work
- consultation continues
- human answer unblocks a task

Start fresh when:

- switching agents
- work is unrelated
- context should reset

Sessions are always scoped to the project.

---

# Task Model

Example task:

    {
      "task_id": "TASK-184",
      "project_id": "PROJ-12",
      "type": "implementation",
      "goal": "Implement tenant-scoped authz middleware",
      "status": "active"
    }

---

# Agent Consultation Model

Agents may consult each other.

Example:

    CONS-42
      turn 1 implementer -> reviewer question
      turn 2 reviewer -> implementer clarification
      turn 3 implementer -> reviewer answer
      turn 4 reviewer -> final result

Consultations are bounded by:

- max_turns
- timeout
- goal completion

---

# Human Consultation Model

Agents may require operator input.

Example structured result:

    {
      "status": "blocked",
      "summary": "Need operator decision on tenant bypass policy.",
      "human_question": {
        "question": "Should background jobs bypass tenant scoping?",
        "reason": "Policy unclear from ADR-027.",
        "suggested_options": [
          "Allow bypass for system jobs",
          "Require strict tenant scoping"
        ]
      }
    }

Coordinator converts this to a human question.

Example record:

    {
      "question_id": "HQ-17",
      "project_id": "PROJ-12",
      "task_id": "TASK-184",
      "from_agent": "reviewer",
      "status": "pending"
    }

---

# Human Question Queue

Questions move through states:

    pending -> answered -> task resumed

When answered:

- the blocked task is requeued
- the coordinator resumes the agent session

---

# Context Passing Strategy

Each worker receives a minimal context packet.

Example:

    {
      "project_id": "PROJ-12",
      "task_id": "TASK-184",
      "goal": "Review middleware change against ADR-027",
      "rolling_summary": [
        "Implementer updated authz middleware.",
        "Tenant bypass exists for system jobs."
      ],
      "current_question": "Does this violate ADR-027?"
    }

Full transcripts are rarely replayed.

---

# Main Agent Loop

Coordinator main loop:

    while system_active():

        project = resolve_active_project()

        if pending_human_questions(project):
            surface_questions_to_operator(project)

        if not has_runnable_work(project):
            wait_for_input()
            continue

        work_item = dequeue_next_work_item(project)

        agent = select_agent(work_item)

        mode = select_invocation_mode(work_item, agent)

        prompt = render_prompt(work_item, agent)

        context_packet = build_context_packet(work_item, agent)

        response = invoke_sidecar(
            agent=agent,
            mode=mode,
            session_id=work_item.session_id,
            prompt=prompt,
            context=context_packet
        )

        persist_transcript(work_item, response.transcript)
        persist_result(work_item, response.result)
        persist_session_id(work_item, response.session_id)

        decision = evaluate_result(response.result)

        route_decision(decision)

---

# Result Evaluation

Rules:

If:

    status = completed
    and recommended_next_agent != none

Then:

    enqueue next work item

If:

    status = blocked
    and human_question present

Then:

    create pending human question

If:

    status = failed

Then:

    retry or escalate

If:

    consultation requested

Then:

    open consultation

---

# Retry and Escalation Policy

Example policy:

    max_retries = 3
    consultation_max_turns = 6
    task_timeout_minutes = 30

Escalation examples:

- repeated failures escalate to architect
- missing context escalates to human
- unresolved consultation escalates to planner

---

# MCP Tool Surface

Project tools:

- get_current_project
- list_projects

Task tools:

- start_task
- run_next_step
- get_task_status
- get_task_result

Human interaction:

- list_pending_questions
- get_question_details
- answer_question

Consultations:

- start_consultation
- reply_to_consultation
- list_active_consultations

Debugging:

- open_transcript
- list_tasks
- retry_task_step

---

# Example MCP Tool: list_pending_questions

Example result:

    {
      "questions": [
        {
          "question_id": "HQ-17",
          "task_id": "TASK-184",
          "summary": "Need decision on tenant bypass policy"
        }
      ]
    }

---

# Example MCP Tool: answer_question

Example input:

    {
      "question_id": "HQ-17",
      "answer": "Allow bypass for system maintenance jobs but enforce tenant scoping elsewhere."
    }

Example output:

    {
      "question_id": "HQ-17",
      "status": "answered",
      "requeued_task_id": "TASK-184"
    }

---

# Interactive Codex Usage Model

Operator workflow:

    "Start task"
      -> start_task

    "Run next step"
      -> run_next_step

    "Do I have any questions?"
      -> list_pending_questions

    "Answer HQ-17"
      -> answer_question

    "Show transcript"
      -> open_transcript

Codex acts as the operator interface while orchestration runs inside the MCP server.

---

# Workflow Example

    planner
       ↓
    implementer
       ↓
    reviewer
       ↓
    tester
       ↓
    docs

Each Codex interaction is a single worker turn.

A higher-level workflow step may still require multiple worker turns when the coordinator needs follow-up execution, consultation, or human input before that step is considered complete.

---

# Design Principles

Atomic worker turns  
Each Codex invocation is a bounded reasoning task.

Coordinator-owned state  
All orchestration state lives in the coordinator.

Schema-first communication  
Agents return structured JSON results.

Transcript separation  
Transcripts are stored separately from routing decisions.

Project isolation  
All state is scoped by project.

Human-in-the-loop control  
Operator decisions are first-class workflow events.

Interactive orchestration  
Codex interacts through MCP tools.

---

# Benefits

This architecture provides:

- deterministic orchestration
- reproducible agent runs
- strong schema contracts
- scalable multi-agent collaboration
- human-in-the-loop decision making
- interactive control from Codex
- robust transcript-based debugging
- compatibility with Codex CLI automation
