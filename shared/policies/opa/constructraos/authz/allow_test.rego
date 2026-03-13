package constructraos.authz

test_run_allowed if {
  allow with input as {"action": "workflow.hello_world.run"}
}

test_execute_allowed_for_valid_use_case if {
  allow with input as {
    "action": "workflow.hello_world.execute",
    "request": {
      "name": "Builder",
      "use_case": "Build the first durable domain slice.",
    },
  }
}

test_execute_denied_for_short_use_case if {
  not allow with input as {
    "action": "workflow.hello_world.execute",
    "request": {
      "name": "Builder",
      "use_case": "Too short",
    },
  }
}

test_unknown_action_denied if {
  not allow with input as {"action": "workflow.unknown"}
}

test_project_task_qa_request_allowed if {
  allow with input as {"action": "project.task.qa_request"}
}

test_project_task_sre_environment_report_allowed if {
  allow with input as {"action": "project.task.sre_environment.report"}
}

test_project_task_codex_execution_accepted_allowed if {
  allow with input as {"action": "project.task.codex_execution.accepted"}
}

test_project_execution_request_list_allowed if {
  allow with input as {"action": "project.execution_request.list"}
}

test_workflow_task_request_qa_allowed_for_project_and_task if {
  allow with input as {
    "action": "workflow.task.request_qa",
    "request": {
      "project_id": "constructraos",
      "task_id": "T-0001",
    },
  }
}

test_workflow_task_request_qa_denied_without_task if {
  not allow with input as {
    "action": "workflow.task.request_qa",
    "request": {
      "project_id": "constructraos",
      "task_id": "",
    },
  }
}

test_workflow_task_report_sre_environment_allowed_with_required_fields if {
  allow with input as {
    "action": "workflow.task.report_sre_environment",
    "request": {
      "project_id": "constructraos",
      "task_id": "T-0001",
      "branch_name": "project/constructraos/integration",
      "status": "ready",
    },
  }
}

test_workflow_task_report_sre_environment_denied_without_branch if {
  not allow with input as {
    "action": "workflow.task.report_sre_environment",
    "request": {
      "project_id": "constructraos",
      "task_id": "T-0001",
      "branch_name": "",
      "status": "ready",
    },
  }
}
