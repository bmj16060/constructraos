package net.mudpot.constructraos.commons.orchestration.project.model;

import java.util.List;

public record KubectlCommandRequest(
    List<String> arguments,
    String stdin,
    Integer timeoutSeconds
) {
}
