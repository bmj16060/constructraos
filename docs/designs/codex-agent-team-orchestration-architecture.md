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

The system uses a **coordinator + sidecar pattern** where Codex agents run as isolated worker turns via:

    codex exec --output-schema

and resume prior sessions using:

    codex exec resume <session_id>

An MCP server exposes the orchestration system to an interactive Codex session so Codex can start tasks, run steps, inspect state, answer questions, and review transcripts without embedding orchestration logic directly in the interactive prompt.

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

# Core Components

## 1. Interactive Codex Session

The interactive Codex session is the human-facing control surface.

Responsibilities:

- Accept operator instructions
- Call MCP tools
- Display orchestration results
- Surface pending human questions
- Provide transcript inspection
- Summarize task state

The interactive session does **not** implement orchestration logic. It calls MCP tools exposed by the orchestration server.

---

## 2. MCP Server

The MCP server provides a structured tool interface between the interactive Codex session and the orchestration runtime.

Responsibilities:

- Expose orchestration functions as MCP tools
- Validate inputs
- Route requests to the coordinator
- Return structured orchestration responses
- Provide access to transcripts and task state
- Surface human consultation questions

The MCP surface should remain **coarse-grained**, leaving workflow logic inside the coordinator.

---

## 3. Coordinator

The coordinator is the orchestration control plane.

Responsibilities:

- Task lifecycle management
- Agent routing
- Consultation management
- Human consultation management
- Session tracking
- Transcript storage
- History summarization
- Retry and escalation policy
- Main agent loop execution

The coordinator **never runs Codex directly**. It calls the sidecar runner.

The coordinator treats every agent invocation as a **single atomic turn**.

---

## 4. Codex Sidecar

The sidecar is a thin wrapper around the Codex CLI.

Responsibilities:

- Execute Codex worker turns
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

Typical roles:

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
- returns a structured result

Agents do not directly invoke each other. All routing occurs through the coordinator.

---

# Structured Output Contract

Each agent must return structured output conforming to a schema.

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

Example result:

    {
      "status": "completed",
      "summary": "Implemented authz middleware and added unit tests.",
      "issues": [],
      "recommended_next_agent": "reviewer"
    }

---

# Transcript Handling

Codex emits a JSONL event stream when invoked with:

    --json

The stream includes:

- assistant messages
- tool calls
- tool results
- execution metadata
- structured output events

Example fragment:

    {"type":"message","role":"assistant","content":"Analyzing ADR-027."}
    {"type":"tool.call","tool":"repo_read"}
    {"type":"tool.result","tool":"repo_read"}
    {"type":"structured_output","value":{...}}

Artifacts are stored separately:

    runs/
      TASK-184/
        implementer/
          turn-1.jsonl
          result.json
        reviewer/
          turn-2.jsonl
          result.json

The coordinator consumes only the structured result for routing decisions.

---

# Session Continuity

Codex sessions can be resumed using:

    codex exec resume <session_id>

Example result payload:

    {
      "session_id": "abc123",
      "result": {...}
    }

Coordinator rules:

Resume session when:

- same agent continues related work
- consultation continues
- human answer unblocks a prior agent

Start fresh run when:

- a different agent runs
- work is unrelated
- context should be reset

---

# Agent Consultation Model

Agents may ask questions of other agents.

Consultations are mediated by the coordinator.

Example consultation:

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

Agents may require human input.

When an agent requires operator input, it returns a structured human question.

Example result:

    {
      "status": "blocked",
      "summary": "Need operator decision on tenant bypass policy.",
      "human_question": {
        "question": "Should background jobs bypass tenant scoping?",
        "reason": "Policy intent unclear from ADR-027.",
        "suggested_options": [
          "Allow bypass for system maintenance jobs",
          "Require strict tenant scoping everywhere"
        ]
      }
    }

The coordinator converts this into a **pending human question**.

Example stored question:

    {
      "question_id": "HQ-17",
      "task_id": "TASK-184",
      "from_agent": "reviewer",
      "question": "Should background jobs bypass tenant scoping?",
      "status": "pending"
    }

---

# Human Question Queue

The coordinator maintains a queue of unanswered human questions.

State transitions:

    pending -> answered -> task resumed

When answered:

- blocked task is requeued
- coordinator resumes appropriate agent session

---

# Context Passing Strategy

Each Codex turn receives a minimal context packet.

Example:

    {
      "task_id": "TASK-184",
      "goal": "Review middleware change against ADR-027",
      "rolling_summary": [
        "Implementer updated authz middleware.",
        "Tenant bypass exists for system jobs."
      ],
      "current_question": "Does this violate ADR-027?",
      "artifacts": {
        "files": [
          "docs/decisions/ADR-027.md",
          "server/src/main/java/.../AuthzFilter.java"
        ]
      }
    }

Full transcripts are rarely replayed.

---

# Main Agent Loop

The coordinator runs a deterministic orchestration loop.

## High-Level Loop

    while system_active():

        if pending_human_questions():
            surface_questions_to_operator()

        if not has_runnable_work():
            wait_for_input()
            continue

        work_item = dequeue_next_work_item()

        agent = select_agent(work_item)

        mode = select_invocation_mode(work_item, agent)

        prompt = render_prompt(work_item, agent)

        context_packet = build_context_packet(work_item, agent)

        response = invoke_sidecar(
            agent=agent,
            mode=mode,
            session_id=work_item.session_id_for(agent),
            prompt=prompt,
            context=context_packet,
            schema=schema_for(agent)
        )

        persist_transcript(work_item, response.transcript)
        persist_result(work_item, response.result)
        persist_session_id(work_item, response.session_id)

        decision = evaluate_result(response.result)

        route_decision(decision)

---

# Result Evaluation

Coordinator decision rules:

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

    open or continue consultation

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

Recommended MCP tools:

Task management:

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

Returns unanswered operator questions.

Example result:

    {
      "questions": [
        {
          "question_id": "HQ-17",
          "task_id": "TASK-184",
          "from_agent": "reviewer",
          "summary": "Need decision on tenant bypass policy"
        }
      ]
    }

---

# Example MCP Tool: answer_question

Example input:

    {
      "question_id": "HQ-17",
      "answer": "Allow bypass for system maintenance jobs but enforce tenant scope everywhere else."
    }

Example response:

    {
      "question_id": "HQ-17",
      "status": "answered",
      "requeued_task_id": "TASK-184"
    }

---

# Interactive Codex Usage Model

Operator workflow inside Codex:

    "Start a new task"
    -> start_task

    "Run the next step"
    -> run_next_step

    "Do I have any questions to answer?"
    -> list_pending_questions

    "Answer HQ-17"
    -> answer_question

    "Show transcript of last reviewer step"
    -> open_transcript

This allows Codex to act as the operator interface while orchestration logic lives entirely in the MCP server.

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

Each step runs as a single Codex execution turn.

---

# Design Principles

Atomic worker turns  
Each Codex invocation is a bounded reasoning unit.

Coordinator-owned state  
All workflow state lives in the coordinator.

Schema-first communication  
Agents communicate via structured JSON outputs.

Transcript separation  
Transcripts are persisted but not required for routing.

Minimal context passing  
Agents receive only relevant history.

Human-in-the-loop control  
Operator questions are first-class workflow events.

Interactive orchestration  
Codex interacts with the system via MCP tools.

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