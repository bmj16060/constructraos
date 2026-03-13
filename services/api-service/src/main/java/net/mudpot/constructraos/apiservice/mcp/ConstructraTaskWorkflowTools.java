package net.mudpot.constructraos.apiservice.mcp;

import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.apiservice.workflow.TaskWorkflowOperationsService;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowSignalResponse;
import net.mudpot.constructraos.commons.orchestration.project.model.TaskWorkflowState;

@Singleton
public class ConstructraTaskWorkflowTools {
    private final TaskWorkflowOperationsService taskWorkflowOperationsService;

    public ConstructraTaskWorkflowTools(final TaskWorkflowOperationsService taskWorkflowOperationsService) {
        this.taskWorkflowOperationsService = taskWorkflowOperationsService;
    }

    @Tool(
        name = "constructra_get_task_workflow_state",
        description = "Read the current ConstructraOS task workflow state for a project task.",
        annotations = @Tool.ToolAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false)
    )
    public TaskWorkflowState getTaskWorkflowState(
        @ToolArg(description = "ConstructraOS project ID, for example constructraos.") final String projectId,
        @ToolArg(description = "ConstructraOS task ID, for example T-0001.") final String taskId
    ) {
        return taskWorkflowOperationsService.currentState(normalize(projectId), normalize(taskId));
    }

    @Tool(
        name = "constructra_report_sre_environment_outcome",
        description = "Record the SRE environment outcome for a ConstructraOS task workflow.",
        annotations = @Tool.ToolAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public TaskWorkflowSignalResponse reportSreEnvironmentOutcome(
        @ToolArg(description = "ConstructraOS project ID.") final String projectId,
        @ToolArg(description = "ConstructraOS task ID.") final String taskId,
        @ToolArg(description = "Branch name receiving the environment outcome.") final String branchName,
        @ToolArg(description = "Environment name that was prepared or attempted.") final String environmentName,
        @ToolArg(description = "Outcome status. Use ready or failed.") final String status,
        @ToolArg(description = "Concise durable note describing the environment result.") final String note
    ) {
        return taskWorkflowOperationsService.reportSreEnvironmentOutcome(
            normalize(projectId),
            normalize(taskId),
            normalize(branchName),
            normalize(environmentName),
            normalize(status),
            normalize(note),
            TaskWorkflowOperationsService.MCP_ACTOR_KIND,
            TaskWorkflowOperationsService.MCP_SESSION_ID
        );
    }

    @Tool(
        name = "constructra_report_codex_execution_accepted",
        description = "Record that a Codex thread accepted responsibility for a ConstructraOS execution request.",
        annotations = @Tool.ToolAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public TaskWorkflowSignalResponse reportCodexExecutionAccepted(
        @ToolArg(description = "ConstructraOS project ID.") final String projectId,
        @ToolArg(description = "ConstructraOS task ID.") final String taskId,
        @ToolArg(description = "Execution request ID, for example T-0001-exec-1.") final String executionRequestId,
        @ToolArg(description = "Codex thread ID handling the work.") final String codexThreadId,
        @ToolArg(description = "Specialist role claiming the work, for example SRE.") final String specialistRole,
        @ToolArg(description = "Optional note about the acceptance event.") final String note
    ) {
        return taskWorkflowOperationsService.reportCodexExecutionAccepted(
            normalize(projectId),
            normalize(taskId),
            normalize(executionRequestId),
            normalize(codexThreadId),
            normalize(specialistRole),
            normalize(note)
        );
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
