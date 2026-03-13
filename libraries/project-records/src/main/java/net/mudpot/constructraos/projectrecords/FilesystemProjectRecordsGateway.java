package net.mudpot.constructraos.projectrecords;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectBranchRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEnvironmentWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionClaimRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Singleton
public class FilesystemProjectRecordsGateway implements ProjectRecordsGateway {
    private final Path rootDirectory;

    public FilesystemProjectRecordsGateway(@Value("${project-records.root-dir:projects}") final String rootDirectory) {
        this(resolveRootDirectory(Path.of(rootDirectory)));
    }

    FilesystemProjectRecordsGateway(final Path rootDirectory) {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
    }

    @Override
    public ProjectTaskRecord loadTask(final String projectId, final String taskId) {
        final Path taskPath = resolveTaskPath(projectId, taskId);
        final List<String> lines = readLines(taskPath);
        return new ProjectTaskRecord(
            projectId,
            taskId,
            extractTitle(lines, taskId),
            extractValue(lines, "- Status:"),
            extractValue(lines, "- Owning specialist:"),
            stripTicks(extractValue(lines, "- Parent control branch:")),
            parseInlineList(extractValue(lines, "- Specialist branches:")),
            parseLinkedValues(lines, "- Linked ADRs:", "- Linked bugs:"),
            parseLinkedValues(lines, "- Linked bugs:", "- Latest evidence:"),
            taskPath.toString()
        );
    }

    @Override
    public ProjectBranchRecord loadBranch(final String projectId, final String branchName) {
        final Path branchIndexPath = projectDirectory(projectId).resolve("branches").resolve("index.md");
        final List<String> lines = readLines(branchIndexPath);
        for (String line : lines) {
            final String trimmed = line.trim();
            if (!trimmed.startsWith("|")) {
                continue;
            }
            final List<String> cells = parseTableRow(trimmed);
            if (cells.size() < 5 || "Branch".equalsIgnoreCase(cells.get(0))) {
                continue;
            }
            final String currentBranch = stripTicks(cells.get(0));
            if (currentBranch.equals(branchName)) {
                return new ProjectBranchRecord(projectId, currentBranch, cells.get(1), cells.get(2), cells.get(3), cells.get(4));
            }
        }
        throw new ProjectRecordNotFoundException("Branch not found: " + branchName);
    }

    @Override
    public ProjectEnvironmentRecord loadEnvironment(final String projectId, final String environmentId) {
        final Path environmentPath = resolveEnvironmentPath(projectId, environmentId);
        return loadEnvironment(projectId, environmentId, environmentPath);
    }

    @Override
    public synchronized ProjectEvidenceRecord writeEvidence(final ProjectEvidenceWriteRequest request) {
        final String projectId = normalizeRequired(request.projectId(), "projectId");
        final String taskId = normalizeRequired(request.taskId(), "taskId");
        final String evidenceType = normalizeRequired(request.evidenceType(), "evidenceType");
        final Path projectDirectory = projectDirectory(projectId);
        final Path evidenceDirectory = projectDirectory.resolve("evidence");
        createDirectories(evidenceDirectory);

        final String evidenceId = nextEvidenceId(evidenceDirectory);
        final Instant createdAt = Instant.now();
        final Path evidencePath = evidenceDirectory.resolve(
            evidenceId + "-" + slugify(taskId + "-" + evidenceType) + ".md"
        );

        writeString(evidencePath, renderEvidenceMarkdown(evidenceId, request, createdAt, evidencePath));
        updateEvidenceIndex(projectDirectory.resolve("evidence").resolve("index.md"), evidenceId, request, createdAt, evidencePath);
        updateTaskLatestEvidence(resolveTaskPath(projectId, taskId), evidenceId, evidencePath);

        return new ProjectEvidenceRecord(
            evidenceId,
            evidencePath.toString(),
            projectId,
            taskId,
            request.branchName(),
            request.environment(),
            request.status(),
            request.validatingSpecialist(),
            createdAt.toString()
        );
    }

    @Override
    public synchronized ProjectEnvironmentRecord writeEnvironment(final ProjectEnvironmentWriteRequest request) {
        final String projectId = normalizeRequired(request.projectId(), "projectId");
        final String taskId = normalizeRequired(request.taskId(), "taskId");
        final String branchName = normalizeRequired(request.branchName(), "branchName");
        final String environmentName = normalizeRequired(request.environmentName(), "environmentName");
        final String namespace = normalizeRequired(request.namespace(), "namespace");
        final Path projectDirectory = projectDirectory(projectId);
        final Path environmentDirectory = projectDirectory.resolve("environments");
        createDirectories(environmentDirectory);

        final String environmentId = normalized(request.environmentId()).isBlank()
            ? nextEnvironmentId(environmentDirectory)
            : normalized(request.environmentId());
        final Path environmentPath = environmentDirectory.resolve(
            environmentId + "-" + slugify(environmentName) + ".md"
        );
        final Instant writtenAt = Instant.now();
        final String lastActiveAt = normalized(request.lastActiveAt()).isBlank()
            ? writtenAt.toString()
            : normalized(request.lastActiveAt());

        writeString(environmentPath, renderEnvironmentMarkdown(
            environmentId,
            new ProjectEnvironmentWriteRequest(
                projectId,
                environmentId,
                taskId,
                branchName,
                environmentName,
                namespace,
                nullToBlank(request.ownershipScope()),
                nullToBlank(request.status()),
                request.protectedEnvironment(),
                lastActiveAt,
                nullToBlank(request.retireAfter()),
                nullToBlank(request.note())
            ),
            writtenAt,
            environmentPath
        ));
        updateEnvironmentIndex(
            projectDirectory.resolve("environments").resolve("index.md"),
            new ProjectEnvironmentWriteRequest(
                projectId,
                environmentId,
                taskId,
                branchName,
                environmentName,
                namespace,
                nullToBlank(request.ownershipScope()),
                nullToBlank(request.status()),
                request.protectedEnvironment(),
                lastActiveAt,
                nullToBlank(request.retireAfter()),
                nullToBlank(request.note())
            ),
            writtenAt,
            environmentPath
        );

        return new ProjectEnvironmentRecord(
            environmentId,
            environmentPath.toString(),
            projectId,
            taskId,
            branchName,
            environmentName,
            namespace,
            nullToBlank(request.ownershipScope()),
            nullToBlank(request.status()),
            request.protectedEnvironment(),
            lastActiveAt,
            nullToBlank(request.retireAfter()),
            nullToBlank(request.note())
        );
    }

    @Override
    public synchronized ProjectExecutionRequestRecord writeExecutionRequest(final ProjectExecutionRequestWriteRequest request) {
        final String projectId = normalizeRequired(request.projectId(), "projectId");
        final String taskId = normalizeRequired(request.taskId(), "taskId");
        final String executionRequestId = normalizeRequired(request.executionRequestId(), "executionRequestId");
        final Path projectDirectory = projectDirectory(projectId);
        final Path executionDirectory = projectDirectory.resolve("executions");
        createDirectories(executionDirectory);
        final Path executionPath = executionDirectory.resolve(
            executionRequestId + "-" + slugify(nullToBlank(request.specialistRole()) + "-" + taskId) + ".md"
        );
        final Instant createdAt = Instant.now();
        writeString(executionPath, renderExecutionRequestMarkdown(request, createdAt, executionPath));
        updateExecutionIndex(projectDirectory.resolve("executions").resolve("index.md"), request, createdAt, executionPath);
        return new ProjectExecutionRequestRecord(
            executionRequestId,
            executionPath.toString(),
            projectId,
            taskId,
            request.specialistRole(),
            request.branchName(),
            request.status(),
            request.codexThreadId(),
            request.workflowId(),
            request.callbackSignal(),
            request.callbackFailureSignal(),
            request.note()
        );
    }

    @Override
    public List<ProjectEnvironmentRecord> listEnvironments(final String projectId, final String status) {
        final Path environmentIndexPath = projectDirectory(projectId).resolve("environments").resolve("index.md");
        final List<String> lines = readLines(environmentIndexPath);
        final List<ProjectEnvironmentRecord> records = new ArrayList<>();
        for (String line : lines) {
            final String trimmed = line.trim();
            if (!trimmed.startsWith("|")) {
                continue;
            }
            final List<String> cells = parseTableRow(trimmed);
            if (cells.size() < 10 || "ID".equalsIgnoreCase(cells.get(0))) {
                continue;
            }
            final String environmentId = normalized(cells.get(0));
            final String environmentPathTarget = normalized(extractMarkdownLinkTarget(cells.get(9)));
            if (environmentId.isBlank() || environmentId.startsWith("---") || environmentPathTarget.isBlank() || environmentPathTarget.startsWith("---")) {
                continue;
            }
            final Path environmentPath = Path.of(environmentPathTarget);
            final ProjectEnvironmentRecord record = loadEnvironment(projectId, environmentId, environmentPath);
            if (normalized(status).isBlank() || normalized(status).equalsIgnoreCase(record.status())) {
                records.add(record);
            }
        }
        return List.copyOf(records);
    }

    @Override
    public List<ProjectExecutionRequestRecord> listExecutionRequests(final String projectId, final String status) {
        final Path executionIndexPath = projectDirectory(projectId).resolve("executions").resolve("index.md");
        final List<String> lines = readLines(executionIndexPath);
        final List<ProjectExecutionRequestRecord> records = new ArrayList<>();
        for (String line : lines) {
            final String trimmed = line.trim();
            if (!trimmed.startsWith("|")) {
                continue;
            }
            final List<String> cells = parseTableRow(trimmed);
            if (cells.size() < 8 || "ID".equalsIgnoreCase(cells.get(0))) {
                continue;
            }
            final String executionId = normalized(cells.get(0));
            final String executionPathTarget = normalized(extractMarkdownLinkTarget(cells.get(7)));
            if (executionId.isBlank() || executionId.startsWith("---") || executionPathTarget.isBlank() || executionPathTarget.startsWith("---")) {
                continue;
            }
            final Path executionPath = Path.of(executionPathTarget);
            final ProjectExecutionRequestRecord record = loadExecutionRequest(projectId, cells.get(0), executionPath);
            if (normalized(status).isBlank() || normalized(status).equalsIgnoreCase(record.status())) {
                records.add(record);
            }
        }
        return List.copyOf(records);
    }

    @Override
    public synchronized ProjectExecutionRequestRecord claimExecutionRequest(final ProjectExecutionClaimRequest request) {
        final String projectId = normalizeRequired(request.projectId(), "projectId");
        final String executionRequestId = normalizeRequired(request.executionRequestId(), "executionRequestId");
        final String codexThreadId = normalizeRequired(request.codexThreadId(), "codexThreadId");
        final Path executionPath = resolveExecutionPath(projectId, executionRequestId);
        final ProjectExecutionRequestRecord existing = loadExecutionRequest(projectId, executionRequestId, executionPath);
        if (!"dispatched".equalsIgnoreCase(existing.status())) {
            throw new IllegalStateException("Execution request is not claimable: " + existing.status());
        }
        return writeExecutionRequest(
            new ProjectExecutionRequestWriteRequest(
                existing.projectId(),
                existing.taskId(),
                existing.id(),
                existing.specialistRole(),
                existing.branchName(),
                "accepted",
                codexThreadId,
                existing.workflowId(),
                existing.callbackSignal(),
                existing.callbackFailureSignal(),
                joinNotes(existing.note(), request.note())
            )
        );
    }

    private Path resolveTaskPath(final String projectId, final String taskId) {
        final Path tasksDirectory = projectDirectory(projectId).resolve("tasks");
        try (var stream = Files.list(tasksDirectory)) {
            return stream
                .filter(path -> path.getFileName().toString().startsWith(taskId + "-"))
                .findFirst()
                .orElseThrow(() -> new ProjectRecordNotFoundException("Task not found: " + taskId));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve task record for " + taskId, exception);
        }
    }

    private Path resolveExecutionPath(final String projectId, final String executionRequestId) {
        final Path executionDirectory = projectDirectory(projectId).resolve("executions");
        try (var stream = Files.list(executionDirectory)) {
            return stream
                .filter(path -> path.getFileName().toString().startsWith(executionRequestId + "-"))
                .findFirst()
                .orElseThrow(() -> new ProjectRecordNotFoundException("Execution request not found: " + executionRequestId));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve execution request record for " + executionRequestId, exception);
        }
    }

    private Path resolveEnvironmentPath(final String projectId, final String environmentId) {
        final Path environmentDirectory = projectDirectory(projectId).resolve("environments");
        try (var stream = Files.list(environmentDirectory)) {
            return stream
                .filter(path -> path.getFileName().toString().startsWith(environmentId + "-"))
                .findFirst()
                .orElseThrow(() -> new ProjectRecordNotFoundException("Environment not found: " + environmentId));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve environment record for " + environmentId, exception);
        }
    }

    private Path projectDirectory(final String projectId) {
        final String normalizedProjectId = normalizeRequired(projectId, "projectId");
        final Path directory = rootDirectory.resolve(normalizedProjectId);
        if (!Files.isDirectory(directory)) {
            throw new ProjectRecordNotFoundException("Project not found: " + normalizedProjectId);
        }
        return directory;
    }

    private static String renderEvidenceMarkdown(
        final String evidenceId,
        final ProjectEvidenceWriteRequest request,
        final Instant createdAt,
        final Path evidencePath
    ) {
        final StringBuilder builder = new StringBuilder();
        builder.append("# ").append(evidenceId).append(": ").append(humanizeEvidenceType(request.evidenceType())).append(" for ").append(request.taskId()).append("\n\n");
        builder.append("- Evidence type: ").append(nullToBlank(request.evidenceType())).append("\n");
        builder.append("- Status: ").append(nullToBlank(request.status())).append("\n");
        builder.append("- Project: ").append(nullToBlank(request.projectId())).append("\n");
        builder.append("- Task: ").append(nullToBlank(request.taskId())).append("\n");
        builder.append("- Branch: `").append(nullToBlank(request.branchName())).append("`\n");
        builder.append("- Environment: ").append(nullToBlank(request.environment())).append("\n");
        builder.append("- Validating specialist: ").append(nullToBlank(request.validatingSpecialist())).append("\n");
        builder.append("- Requested by: ").append(nullToBlank(request.actorKind())).append("\n");
        builder.append("- Session: ").append(nullToBlank(request.sessionId())).append("\n");
        builder.append("- Workflow: ").append(nullToBlank(request.workflowId())).append("\n");
        builder.append("- Created at: ").append(createdAt).append("\n");
        builder.append("- Record: ").append(evidencePath.toAbsolutePath()).append("\n\n");
        builder.append("## Checks\n\n");
        for (String check : request.executedChecks()) {
            builder.append("- ").append(check).append("\n");
        }
        builder.append("\n## Notes\n\n");
        builder.append(nullToBlank(request.note()).isBlank() ? "No additional notes." : request.note()).append("\n");
        return builder.toString();
    }

    private static String renderEnvironmentMarkdown(
        final String environmentId,
        final ProjectEnvironmentWriteRequest request,
        final Instant writtenAt,
        final Path environmentPath
    ) {
        final StringBuilder builder = new StringBuilder();
        builder.append("# ").append(environmentId).append(": ").append(nullToBlank(request.environmentName())).append("\n\n");
        builder.append("- Status: ").append(nullToBlank(request.status())).append("\n");
        builder.append("- Project: ").append(nullToBlank(request.projectId())).append("\n");
        builder.append("- Task: ").append(nullToBlank(request.taskId())).append("\n");
        builder.append("- Branch: `").append(nullToBlank(request.branchName())).append("`\n");
        builder.append("- Environment name: ").append(nullToBlank(request.environmentName())).append("\n");
        builder.append("- Namespace: ").append(nullToBlank(request.namespace())).append("\n");
        builder.append("- Ownership scope: ").append(nullToBlank(request.ownershipScope())).append("\n");
        builder.append("- Protected: ").append(request.protectedEnvironment()).append("\n");
        builder.append("- Last active at: ").append(nullToBlank(request.lastActiveAt())).append("\n");
        builder.append("- Retire after: ").append(nullToBlank(request.retireAfter())).append("\n");
        builder.append("- Updated at: ").append(writtenAt).append("\n");
        builder.append("- Record: ").append(environmentPath.toAbsolutePath()).append("\n\n");
        builder.append("## Notes\n\n");
        builder.append(nullToBlank(request.note()).isBlank() ? "No additional notes." : request.note()).append("\n");
        return builder.toString();
    }

    private static String renderExecutionRequestMarkdown(
        final ProjectExecutionRequestWriteRequest request,
        final Instant createdAt,
        final Path executionPath
    ) {
        final StringBuilder builder = new StringBuilder();
        builder.append("# ").append(request.executionRequestId()).append(": ").append(nullToBlank(request.specialistRole())).append(" execution request\n\n");
        builder.append("- Status: ").append(nullToBlank(request.status())).append("\n");
        builder.append("- Project: ").append(nullToBlank(request.projectId())).append("\n");
        builder.append("- Task: ").append(nullToBlank(request.taskId())).append("\n");
        builder.append("- Specialist role: ").append(nullToBlank(request.specialistRole())).append("\n");
        builder.append("- Branch: `").append(nullToBlank(request.branchName())).append("`\n");
        builder.append("- Codex thread: ").append(nullToBlank(request.codexThreadId())).append("\n");
        builder.append("- Workflow: ").append(nullToBlank(request.workflowId())).append("\n");
        builder.append("- Callback signal: ").append(nullToBlank(request.callbackSignal())).append("\n");
        builder.append("- Callback failure signal: ").append(nullToBlank(request.callbackFailureSignal())).append("\n");
        builder.append("- Created at: ").append(createdAt).append("\n");
        builder.append("- Record: ").append(executionPath.toAbsolutePath()).append("\n\n");
        builder.append("## Notes\n\n");
        builder.append(nullToBlank(request.note()).isBlank() ? "No additional notes." : request.note()).append("\n");
        return builder.toString();
    }

    private static ProjectExecutionRequestRecord loadExecutionRequest(
        final String projectId,
        final String executionRequestId,
        final Path executionPath
    ) {
        final List<String> lines = readLines(executionPath);
        return new ProjectExecutionRequestRecord(
            executionRequestId,
            executionPath.toString(),
            projectId,
            extractValue(lines, "- Task:"),
            extractValue(lines, "- Specialist role:"),
            stripTicks(extractValue(lines, "- Branch:")),
            extractValue(lines, "- Status:"),
            extractValue(lines, "- Codex thread:"),
            extractValue(lines, "- Workflow:"),
            extractValue(lines, "- Callback signal:"),
            extractValue(lines, "- Callback failure signal:"),
            extractSection(lines, "## Notes")
        );
    }

    private static ProjectEnvironmentRecord loadEnvironment(
        final String projectId,
        final String environmentId,
        final Path environmentPath
    ) {
        final List<String> lines = readLines(environmentPath);
        return new ProjectEnvironmentRecord(
            environmentId,
            environmentPath.toString(),
            projectId,
            extractValue(lines, "- Task:"),
            stripTicks(extractValue(lines, "- Branch:")),
            extractValue(lines, "- Environment name:"),
            extractValue(lines, "- Namespace:"),
            extractValue(lines, "- Ownership scope:"),
            extractValue(lines, "- Status:"),
            Boolean.parseBoolean(extractValue(lines, "- Protected:")),
            extractValue(lines, "- Last active at:"),
            extractValue(lines, "- Retire after:"),
            extractSection(lines, "## Notes")
        );
    }

    private static void updateEnvironmentIndex(
        final Path indexPath,
        final ProjectEnvironmentWriteRequest request,
        final Instant writtenAt,
        final Path environmentPath
    ) {
        final String row = "| " + nullToBlank(request.environmentId())
            + " | " + request.taskId()
            + " | `" + request.branchName() + "`"
            + " | " + request.environmentName()
            + " | `" + request.namespace() + "`"
            + " | " + nullToBlank(request.ownershipScope())
            + " | " + nullToBlank(request.status())
            + " | " + nullToBlank(request.lastActiveAt())
            + " | " + nullToBlank(request.retireAfter())
            + " | [" + nullToBlank(request.environmentId()) + "](" + environmentPath.toAbsolutePath() + ") |";
        final List<String> lines = Files.exists(indexPath) ? readLines(indexPath) : List.of("# Environment Index");
        final List<String> updated = new ArrayList<>();
        boolean replacedEmpty = false;
        for (String line : lines) {
            if (line.contains("No environments have been recorded yet.")) {
                if (!replacedEmpty) {
                    updated.clear();
                    updated.add("# Environment Index");
                    updated.add("");
                    updated.add("| ID | Task | Branch | Environment | Namespace | Scope | Status | Last Active At | Retire After | Link |");
                    updated.add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |");
                    updated.add(row);
                    replacedEmpty = true;
                }
                continue;
            }
            updated.add(line);
        }
        if (!replacedEmpty) {
            if (updated.stream().noneMatch(line -> line.startsWith("| ID | Task | Branch | Environment | Namespace | Scope | Status | Last Active At | Retire After | Link |"))) {
                if (!updated.isEmpty() && !updated.get(updated.size() - 1).isBlank()) {
                    updated.add("");
                }
                updated.add("| ID | Task | Branch | Environment | Namespace | Scope | Status | Last Active At | Retire After | Link |");
                updated.add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |");
            }
            boolean replacedExisting = false;
            for (int i = 0; i < updated.size(); i++) {
                final String line = updated.get(i);
                if (line.startsWith("| " + request.environmentId() + " |")) {
                    updated.set(i, row);
                    replacedExisting = true;
                    break;
                }
            }
            if (!replacedExisting) {
                updated.add(row);
            }
        }
        writeString(indexPath, String.join("\n", updated) + "\n");
    }

    private static void updateEvidenceIndex(
        final Path indexPath,
        final String evidenceId,
        final ProjectEvidenceWriteRequest request,
        final Instant createdAt,
        final Path evidencePath
    ) {
        final String row = "| " + evidenceId
            + " | " + request.taskId()
            + " | `" + request.branchName() + "`"
            + " | " + request.status()
            + " | " + createdAt
            + " | [" + evidenceId + "](" + evidencePath.toAbsolutePath() + ") |";
        final List<String> lines = Files.exists(indexPath) ? readLines(indexPath) : List.of("# Evidence Index");
        final List<String> updated = new ArrayList<>();
        boolean replacedEmpty = false;
        for (String line : lines) {
            if (line.contains("No QA or test evidence has been recorded yet.")) {
                if (!replacedEmpty) {
                    updated.add("# Evidence Index");
                    updated.add("");
                    updated.add("| ID | Task | Branch | Status | Created At | Link |");
                    updated.add("| --- | --- | --- | --- | --- | --- |");
                    updated.add(row);
                    replacedEmpty = true;
                }
                continue;
            }
            updated.add(line);
        }
        if (!replacedEmpty) {
            if (updated.stream().noneMatch(line -> line.startsWith("| ID | Task | Branch | Status | Created At | Link |"))) {
                if (!updated.isEmpty() && !updated.get(updated.size() - 1).isBlank()) {
                    updated.add("");
                }
                updated.add("| ID | Task | Branch | Status | Created At | Link |");
                updated.add("| --- | --- | --- | --- | --- | --- |");
            }
            updated.add(row);
        }
        writeString(indexPath, String.join("\n", updated) + "\n");
    }

    private static void updateExecutionIndex(
        final Path indexPath,
        final ProjectExecutionRequestWriteRequest request,
        final Instant createdAt,
        final Path executionPath
    ) {
        final String row = "| " + request.executionRequestId()
            + " | " + request.taskId()
            + " | " + request.specialistRole()
            + " | `" + request.branchName() + "`"
            + " | " + request.status()
            + " | " + nullToBlank(request.codexThreadId())
            + " | " + createdAt
            + " | [" + request.executionRequestId() + "](" + executionPath.toAbsolutePath() + ") |";
        final List<String> lines = Files.exists(indexPath) ? readLines(indexPath) : List.of("# Execution Request Index");
        final List<String> updated = new ArrayList<>();
        boolean replacedEmpty = false;
        for (String line : lines) {
            if (line.contains("No specialist execution requests have been recorded yet.")) {
                if (!replacedEmpty) {
                    updated.add("# Execution Request Index");
                    updated.add("");
                    updated.add("| ID | Task | Specialist | Branch | Status | Codex Thread | Created At | Link |");
                    updated.add("| --- | --- | --- | --- | --- | --- | --- | --- |");
                    updated.add(row);
                    replacedEmpty = true;
                }
                continue;
            }
            updated.add(line);
        }
        if (!replacedEmpty) {
            if (updated.stream().noneMatch(line -> line.startsWith("| ID | Task | Specialist | Branch | Status | Codex Thread | Created At | Link |"))) {
                if (!updated.isEmpty() && !updated.get(updated.size() - 1).isBlank()) {
                    updated.add("");
                }
                updated.add("| ID | Task | Specialist | Branch | Status | Codex Thread | Created At | Link |");
                updated.add("| --- | --- | --- | --- | --- | --- | --- | --- |");
            }
            boolean replacedExisting = false;
            for (int i = 0; i < updated.size(); i++) {
                final String line = updated.get(i);
                if (line.startsWith("| " + request.executionRequestId() + " |")) {
                    updated.set(i, row);
                    replacedExisting = true;
                    break;
                }
            }
            if (!replacedExisting) {
                updated.add(row);
            }
        }
        writeString(indexPath, String.join("\n", updated) + "\n");
    }

    private static void updateTaskLatestEvidence(final Path taskPath, final String evidenceId, final Path evidencePath) {
        final List<String> lines = readLines(taskPath);
        final List<String> updated = new ArrayList<>();
        final String evidenceLink = "[" + evidenceId + "](" + evidencePath.toAbsolutePath() + ")";
        for (String line : lines) {
            if (line.startsWith("- Latest evidence:")) {
                final String currentValue = line.substring("- Latest evidence:".length()).trim();
                if (currentValue.isBlank() || "none".equalsIgnoreCase(currentValue)) {
                    updated.add("- Latest evidence: " + evidenceLink);
                } else if (currentValue.contains(evidenceLink)) {
                    updated.add(line);
                } else {
                    updated.add("- Latest evidence: " + currentValue + ", " + evidenceLink);
                }
                continue;
            }
            updated.add(line);
        }
        writeString(taskPath, String.join("\n", updated) + "\n");
    }

    private static List<String> parseTableRow(final String row) {
        final String trimmed = row.substring(1, row.length() - 1);
        final String[] parts = trimmed.split("\\|");
        final List<String> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            values.add(part.trim());
        }
        return values;
    }

    private static List<String> parseInlineList(final String value) {
        final String normalized = nullToBlank(value).trim();
        if (normalized.isBlank() || normalized.equalsIgnoreCase("none") || normalized.equalsIgnoreCase("none yet")) {
            return List.of();
        }
        final String[] parts = normalized.split(",");
        final List<String> items = new ArrayList<>(parts.length);
        for (String part : parts) {
            final String current = stripTicks(part.trim());
            if (!current.isBlank()) {
                items.add(current);
            }
        }
        return List.copyOf(items);
    }

    private static List<String> parseLinkedValues(final List<String> lines, final String sectionPrefix, final String nextPrefix) {
        final List<String> values = new ArrayList<>();
        boolean capturing = false;
        for (String line : lines) {
            if (line.startsWith(sectionPrefix)) {
                capturing = true;
                continue;
            }
            if (capturing && line.startsWith(nextPrefix)) {
                break;
            }
            if (capturing && line.startsWith("  - ")) {
                values.add(line.substring(4).trim());
            }
        }
        return List.copyOf(values);
    }

    private static String extractTitle(final List<String> lines, final String taskId) {
        return lines.stream()
            .filter(line -> line.startsWith("# "))
            .findFirst()
            .map(line -> {
                final String title = line.substring(2).trim();
                final String prefix = taskId + ":";
                return title.startsWith(prefix) ? title.substring(prefix.length()).trim() : title;
            })
            .orElse(taskId);
    }

    private static String extractValue(final List<String> lines, final String prefix) {
        final Optional<String> value = lines.stream()
            .filter(line -> line.startsWith(prefix))
            .findFirst()
            .map(line -> line.substring(prefix.length()).trim());
        return value.orElse("");
    }

    private static String nextEvidenceId(final Path evidenceDirectory) {
        try (var stream = Files.list(evidenceDirectory)) {
            final int nextNumber = stream
                .map(path -> path.getFileName().toString())
                .filter(name -> name.startsWith("E-"))
                .map(FilesystemProjectRecordsGateway::extractEvidenceNumber)
                .filter(number -> number > 0)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
            return "E-" + String.format(Locale.ROOT, "%04d", nextNumber);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate next evidence ID.", exception);
        }
    }

    private static String nextEnvironmentId(final Path environmentDirectory) {
        try (var stream = Files.list(environmentDirectory)) {
            final int nextNumber = stream
                .map(path -> path.getFileName().toString())
                .filter(name -> name.startsWith("ENV-"))
                .map(FilesystemProjectRecordsGateway::extractEnvironmentNumber)
                .filter(number -> number > 0)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
            return "ENV-" + String.format(Locale.ROOT, "%04d", nextNumber);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate next environment ID.", exception);
        }
    }

    private static int extractEnvironmentNumber(final String filename) {
        final int dashIndex = filename.indexOf('-', 4);
        final String number = dashIndex > 0 ? filename.substring(4, dashIndex) : "";
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int extractEvidenceNumber(final String filename) {
        final int dashIndex = filename.indexOf('-', 2);
        final String number = dashIndex > 0 ? filename.substring(2, dashIndex) : "";
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static List<String> readLines(final Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed reading " + path, exception);
        }
    }

    private static void writeString(final Path path, final String content) {
        try {
            createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed writing " + path, exception);
        }
    }

    private static void createDirectories(final Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed creating " + path, exception);
        }
    }

    private static String normalizeRequired(final String value, final String fieldName) {
        final String normalized = nullToBlank(value).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    private static String stripTicks(final String value) {
        final String normalized = nullToBlank(value).trim();
        if (normalized.startsWith("`") && normalized.endsWith("`") && normalized.length() > 1) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private static String slugify(final String value) {
        return nullToBlank(value)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    }

    private static String nullToBlank(final String value) {
        return value == null ? "" : value;
    }

    private static String normalized(final String value) {
        return nullToBlank(value).trim();
    }

    private static String extractMarkdownLinkTarget(final String markdownLink) {
        final String normalized = nullToBlank(markdownLink).trim();
        final int open = normalized.indexOf('(');
        final int close = normalized.lastIndexOf(')');
        if (open >= 0 && close > open) {
            return normalized.substring(open + 1, close);
        }
        return normalized;
    }

    private static String extractSection(final List<String> lines, final String heading) {
        boolean inSection = false;
        final StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line.equals(heading)) {
                inSection = true;
                continue;
            }
            if (inSection && line.startsWith("## ")) {
                break;
            }
            if (inSection) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString().trim();
    }

    private static String joinNotes(final String first, final String second) {
        final String left = normalized(first);
        final String right = normalized(second);
        if (left.isBlank()) {
            return right;
        }
        if (right.isBlank()) {
            return left;
        }
        return left + "\n\n" + right;
    }

    private static String humanizeEvidenceType(final String value) {
        final String normalized = nullToBlank(value).trim();
        if (normalized.isBlank()) {
            return "Evidence";
        }
        final String[] parts = normalized.split("-");
        final StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private static Path resolveRootDirectory(final Path configuredPath) {
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            final Path candidate = current.resolve(configuredPath).normalize();
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return configuredPath.toAbsolutePath().normalize();
    }
}
