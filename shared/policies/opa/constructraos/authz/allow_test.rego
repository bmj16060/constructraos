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
