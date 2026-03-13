package constructraos.authz

default allow := false
allowed_actions := {
  "workflow.hello_world.run",
  "workflow.hello_world.start",
  "workflow.hello_world.history",
  "project.execution_request.list",
  "project.task.codex_execution.accepted",
  "project.task.qa_request",
  "project.task.sre_environment.report",
  "project.task.workflow.view",
}

normalized_action := lower(trim_space(object.get(input, "action", "")))

workflow_execute_supported if normalized_action == "workflow.hello_world.execute"
task_qa_request_supported if normalized_action == "workflow.task.request_qa"
task_sre_environment_supported if normalized_action == "workflow.task.report_sre_environment"
environment_launch_supported if normalized_action == "workflow.environment.launch"
environment_reuse_supported if normalized_action == "workflow.environment.reuse"
environment_retire_supported if normalized_action == "workflow.environment.retire"
environment_delete_supported if normalized_action == "workflow.environment.delete"

workflow_execute_allowed if {
  workflow_execute_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "use_case", ""))) >= 15
}

allow if {
  allowed_actions[normalized_action]
}

allow if workflow_execute_allowed
allow if task_qa_request_allowed
allow if task_sre_environment_allowed
allow if environment_launch_allowed
allow if environment_reuse_allowed
allow if environment_retire_allowed
allow if environment_delete_allowed

reason := "action-allowed" if allowed_actions[normalized_action]
reason := "hello_world_execute_allowed" if workflow_execute_allowed
reason := "task_qa_request_allowed" if task_qa_request_allowed
reason := "task_sre_environment_allowed" if task_sre_environment_allowed
reason := "environment_launch_allowed" if environment_launch_allowed
reason := "environment_reuse_allowed" if environment_reuse_allowed
reason := "environment_retire_allowed" if environment_retire_allowed
reason := "environment_delete_allowed" if environment_delete_allowed
reason := "hello_world_use_case_too_short" if {
  workflow_execute_supported
  not workflow_execute_allowed
}
reason := "task_qa_request_missing_project_or_task" if {
  task_qa_request_supported
  not task_qa_request_allowed
}
reason := "task_sre_environment_missing_required_fields" if {
  task_sre_environment_supported
  not task_sre_environment_allowed
}
reason := "environment_launch_missing_required_fields" if {
  environment_launch_supported
  not environment_launch_allowed
}
reason := "environment_reuse_missing_required_fields" if {
  environment_reuse_supported
  not environment_reuse_allowed
}
reason := "environment_retire_denied" if {
  environment_retire_supported
  not environment_retire_allowed
}
reason := "environment_delete_denied" if {
  environment_delete_supported
  not environment_delete_allowed
}
reason := "action-not-supported" if {
  not allowed_actions[normalized_action]
  not workflow_execute_supported
  not task_qa_request_supported
  not task_sre_environment_supported
  not environment_launch_supported
  not environment_reuse_supported
  not environment_retire_supported
  not environment_delete_supported
}

policy_version := "constructraos.v1"

task_qa_request_allowed if {
  task_qa_request_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "project_id", ""))) > 0
  count(trim_space(object.get(request, "task_id", ""))) > 0
}

task_sre_environment_allowed if {
  task_sre_environment_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "project_id", ""))) > 0
  count(trim_space(object.get(request, "task_id", ""))) > 0
  count(trim_space(object.get(request, "branch_name", ""))) > 0
  count(trim_space(object.get(request, "status", ""))) > 0
}

environment_launch_allowed if {
  environment_launch_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "project_id", ""))) > 0
  count(trim_space(object.get(request, "task_id", ""))) > 0
  count(trim_space(object.get(request, "branch_name", ""))) > 0
  count(trim_space(object.get(request, "namespace", ""))) > 0
}

environment_reuse_allowed if {
  environment_reuse_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "project_id", ""))) > 0
  count(trim_space(object.get(request, "task_id", ""))) > 0
  count(trim_space(object.get(request, "branch_name", ""))) > 0
  count(trim_space(object.get(request, "environment_id", ""))) > 0
  count(trim_space(object.get(request, "namespace", ""))) > 0
  not lower(trim_space(object.get(request, "environment_status", ""))) == "retired"
  not lower(trim_space(object.get(request, "environment_status", ""))) == "deleting"
  not lower(trim_space(object.get(request, "environment_status", ""))) == "deleted"
}

environment_retire_allowed if {
  environment_retire_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "environment_id", ""))) > 0
  count(trim_space(object.get(request, "namespace", ""))) > 0
  not object.get(request, "protected_environment", false)
}

environment_delete_allowed if {
  environment_delete_supported
  request := object.get(input, "request", {})
  count(trim_space(object.get(request, "environment_id", ""))) > 0
  count(trim_space(object.get(request, "namespace", ""))) > 0
  not object.get(request, "protected_environment", false)
  object.get(request, "active_execution_count", 0) == 0
}
