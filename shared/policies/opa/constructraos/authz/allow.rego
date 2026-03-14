package constructraos.authz

default allow := false
allowed_actions := {
  "task.codex_execution.list",
  "task.codex_execution.read",
  "task.codex_execution.start",
  "workflow.codex_execution.run",
  "workflow.codex_execution.start",
  "workflow.hello_world.run",
  "workflow.hello_world.start",
  "workflow.hello_world.history",
}

normalized_action := lower(trim_space(object.get(input, "action", "")))

workflow_execute_supported if normalized_action == "workflow.hello_world.execute"

codex_execution_supported if normalized_action == "workflow.codex_execution.execute"

workflow_execute_allowed if {
  workflow_execute_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "use_case", ""))) >= 15
}

codex_execution_allowed if {
  codex_execution_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "prompt", ""))) >= 12
}

allow if {
  allowed_actions[normalized_action]
}

allow if workflow_execute_allowed

allow if codex_execution_allowed

reason := "action-allowed" if allowed_actions[normalized_action]
reason := "hello_world_execute_allowed" if workflow_execute_allowed
reason := "codex_execution_allowed" if codex_execution_allowed
reason := "hello_world_use_case_too_short" if {
  workflow_execute_supported
  not workflow_execute_allowed
}
reason := "codex_execution_prompt_too_short" if {
  codex_execution_supported
  not codex_execution_allowed
}
reason := "action-not-supported" if {
  not allowed_actions[normalized_action]
  not workflow_execute_supported
  not codex_execution_supported
}

policy_version := "constructraos.v1"
