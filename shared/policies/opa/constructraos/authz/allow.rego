package constructraos.authz

default allow := false
allowed_actions := {
  "workflow.hello_world.run",
  "workflow.hello_world.start",
  "workflow.hello_world.history",
  "project.task.qa_request",
  "project.task.sre_environment.report",
  "project.task.workflow.view",
}

normalized_action := lower(trim_space(object.get(input, "action", "")))

workflow_execute_supported if normalized_action == "workflow.hello_world.execute"
task_qa_request_supported if normalized_action == "workflow.task.request_qa"
task_sre_environment_supported if normalized_action == "workflow.task.report_sre_environment"

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

reason := "action-allowed" if allowed_actions[normalized_action]
reason := "hello_world_execute_allowed" if workflow_execute_allowed
reason := "task_qa_request_allowed" if task_qa_request_allowed
reason := "task_sre_environment_allowed" if task_sre_environment_allowed
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
reason := "action-not-supported" if {
  not allowed_actions[normalized_action]
  not workflow_execute_supported
  not task_qa_request_supported
  not task_sre_environment_supported
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
