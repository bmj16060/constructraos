package net.mudpot.constructraos.commons.orchestration.project.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import net.mudpot.constructraos.commons.orchestration.project.model.KubectlCommandRequest;
import net.mudpot.constructraos.commons.orchestration.project.model.KubectlCommandResult;

@ActivityInterface
public interface KubectlActivities {
    @ActivityMethod
    KubectlCommandResult runCommand(KubectlCommandRequest request);
}
