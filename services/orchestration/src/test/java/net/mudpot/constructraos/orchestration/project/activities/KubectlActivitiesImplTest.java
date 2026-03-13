package net.mudpot.constructraos.orchestration.project.activities;

import net.mudpot.constructraos.commons.orchestration.project.model.KubectlCommandRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.KubectlCommandResult;
import net.mudpot.constructraos.orchestration.config.KubectlActivityConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubectlActivitiesImplTest {
    @Test
    void runCommandReturnsSkippedWhenDisabled() throws Exception {
        final KubectlActivitiesImpl activities = new KubectlActivitiesImpl(config(false, "kubectl", 5));

        final KubectlCommandResult result = activities.runCommand(new KubectlCommandRequest(List.of("get", "pods"), "", null));

        assertEquals("skipped", result.status());
        assertEquals(List.of("kubectl", "get", "pods"), result.command());
    }

    @Test
    void runCommandExecutesConfiguredBinaryAndPassesStdin() throws Exception {
        final Path script = writeScript("""
            #!/bin/sh
            printf 'args:%s\\n' "$*"
            cat
            """);
        final KubectlActivitiesImpl activities = new KubectlActivitiesImpl(config(true, script.toString(), 5));

        final KubectlCommandResult result = activities.runCommand(
            new KubectlCommandRequest(List.of("apply", "-f", "-"), "kind: Pod\n", null)
        );

        assertEquals("succeeded", result.status());
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("args:apply -f -"));
        assertTrue(result.stdout().contains("kind: Pod"));
    }

    @Test
    void runCommandReturnsFailedForNonZeroExit() throws Exception {
        final Path script = writeScript("""
            #!/bin/sh
            echo 'boom' >&2
            exit 7
            """);
        final KubectlActivitiesImpl activities = new KubectlActivitiesImpl(config(true, script.toString(), 5));

        final KubectlCommandResult result = activities.runCommand(new KubectlCommandRequest(List.of("get", "pods"), "", null));

        assertEquals("failed", result.status());
        assertEquals(7, result.exitCode());
        assertTrue(result.stderr().contains("boom"));
    }

    @Test
    void runCommandTimesOut() throws Exception {
        final Path script = writeScript("""
            #!/bin/sh
            sleep 2
            """);
        final KubectlActivitiesImpl activities = new KubectlActivitiesImpl(config(true, script.toString(), 5));

        final KubectlCommandResult result = activities.runCommand(new KubectlCommandRequest(List.of("wait"), "", 1));

        assertEquals("timed_out", result.status());
        assertEquals(-1, result.exitCode());
    }

    private static Path writeScript(final String contents) throws Exception {
        final Path script = Files.createTempFile("kubectl-activities", ".sh");
        Files.writeString(script, contents);
        Files.setPosixFilePermissions(script, Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
        ));
        return script;
    }

    private static KubectlActivityConfig config(final boolean enabled, final String binary, final int timeoutSeconds) throws Exception {
        final KubectlActivityConfig config = new KubectlActivityConfig();
        set(config, "enabled", enabled);
        set(config, "binary", binary);
        set(config, "timeoutSeconds", timeoutSeconds);
        return config;
    }

    private static void set(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
