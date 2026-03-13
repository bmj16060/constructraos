package net.mudpot.constructraos.commons.orchestration.project.model;

import java.util.List;

public record KubectlCommandResult(
    String status,
    List<String> command,
    int exitCode,
    String stdout,
    String stderr
) {
}
