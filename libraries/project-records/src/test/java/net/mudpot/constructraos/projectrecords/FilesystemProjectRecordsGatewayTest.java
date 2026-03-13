package net.mudpot.constructraos.projectrecords;

import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectEvidenceWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestRecord;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectExecutionRequestWriteRequest;
import net.mudpot.constructraos.commons.projectrecords.model.ProjectTaskRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesystemProjectRecordsGatewayTest {
    @TempDir
    Path tempDir;

    @Test
    void loadTaskReadsStructuredFields() throws IOException {
        final Path projectRoot = seedProjectTree();
        final FilesystemProjectRecordsGateway gateway = new FilesystemProjectRecordsGateway(projectRoot.getParent());

        final ProjectTaskRecord taskRecord = gateway.loadTask("constructraos", "T-0001");

        assertEquals("Bootstrap project filesystem contract", taskRecord.title());
        assertEquals("in_progress", taskRecord.status());
        assertEquals("PM", taskRecord.owningSpecialist());
        assertEquals("project/constructraos/integration", taskRecord.parentControlBranch());
    }

    @Test
    void writeEvidenceCreatesRecordAndUpdatesIndexes() throws IOException {
        final Path projectRoot = seedProjectTree();
        final FilesystemProjectRecordsGateway gateway = new FilesystemProjectRecordsGateway(projectRoot.getParent());

        final ProjectEvidenceRecord evidenceRecord = gateway.writeEvidence(
            new ProjectEvidenceWriteRequest(
                "constructraos",
                "T-0001",
                "qa-request",
                "project/constructraos/integration",
                "planned integration environment",
                "QA",
                "requested",
                List.of("QA requested for task workflow.", "Awaiting SRE environment preparation."),
                "Operator requested the first QA pass.",
                "anonymous",
                "anon-session-1",
                "task-constructraos-T-0001"
            )
        );

        assertEquals("E-0001", evidenceRecord.id());
        assertTrue(Files.exists(Path.of(evidenceRecord.path())));
        assertTrue(Files.readString(projectRoot.resolve("evidence").resolve("index.md")).contains("E-0001"));
        assertTrue(Files.readString(projectRoot.resolve("tasks").resolve("T-0001-bootstrap-project-contract.md")).contains("E-0001"));
    }

    @Test
    void writeExecutionRequestCreatesRecordAndIndex() throws IOException {
        final Path projectRoot = seedProjectTree();
        final FilesystemProjectRecordsGateway gateway = new FilesystemProjectRecordsGateway(projectRoot.getParent());

        final ProjectExecutionRequestRecord record = gateway.writeExecutionRequest(
            new ProjectExecutionRequestWriteRequest(
                "constructraos",
                "T-0001",
                "T-0001-exec-1",
                "SRE",
                "project/constructraos/integration",
                "dispatched",
                "",
                "project-constructraos-task-t-0001",
                "reportCodexExecutionAccepted",
                "reportSreEnvironmentOutcome",
                "Dispatch request for Codex SRE execution."
            )
        );

        assertEquals("T-0001-exec-1", record.id());
        assertTrue(Files.exists(Path.of(record.path())));
        assertTrue(Files.readString(projectRoot.resolve("executions").resolve("index.md")).contains("T-0001-exec-1"));
    }

    private Path seedProjectTree() throws IOException {
        final Path projectsRoot = tempDir.resolve("projects");
        final Path projectRoot = projectsRoot.resolve("constructraos");
        Files.createDirectories(projectRoot.resolve("tasks"));
        Files.createDirectories(projectRoot.resolve("branches"));
        Files.createDirectories(projectRoot.resolve("evidence"));
        Files.createDirectories(projectRoot.resolve("executions"));
        Files.writeString(
            projectRoot.resolve("tasks").resolve("T-0001-bootstrap-project-contract.md"),
            """
            # T-0001: Bootstrap project filesystem contract

            - Status: in_progress
            - Owning specialist: PM
            - Parent control branch: `project/constructraos/integration`
            - Specialist branches: none yet
            - Linked ADRs:
              - [ADR-0001](/tmp/adr)
            - Linked bugs: none
            - Latest evidence: none
            """
        );
        Files.writeString(
            projectRoot.resolve("branches").resolve("index.md"),
            """
            # Branch Index

            | Branch | Role | Scope | Environment | Status |
            | --- | --- | --- | --- | --- |
            | `project/constructraos/integration` | project control branch | ConstructraOS project roll-up branch | planned integration environment | planned |
            """
        );
        Files.writeString(projectRoot.resolve("evidence").resolve("index.md"), "# Evidence Index\n\nNo QA or test evidence has been recorded yet.\n");
        Files.writeString(projectRoot.resolve("executions").resolve("index.md"), "# Execution Request Index\n\nNo specialist execution requests have been recorded yet.\n");
        return projectRoot;
    }
}
