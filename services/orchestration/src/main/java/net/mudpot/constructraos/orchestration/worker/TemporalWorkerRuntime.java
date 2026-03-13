package net.mudpot.constructraos.orchestration.worker;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.opentracing.OpenTracingOptions;
import io.temporal.opentracing.OpenTracingWorkerInterceptor;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.mudpot.constructraos.commons.orchestration.system.workflows.HelloWorldWorkflow;
import net.mudpot.constructraos.commons.orchestration.project.workflows.TaskCoordinationWorkflow;
import net.mudpot.constructraos.orchestration.config.TemporalWorkerConfig;
import net.mudpot.constructraos.orchestration.project.activities.ProjectRecordsActivitiesImpl;
import net.mudpot.constructraos.orchestration.project.workflows.TaskCoordinationWorkflowImpl;
import net.mudpot.constructraos.orchestration.system.activities.HelloActivitiesImpl;
import net.mudpot.constructraos.orchestration.system.activities.LlmActivitiesImpl;
import net.mudpot.constructraos.orchestration.system.activities.PromptActivitiesImpl;
import net.mudpot.constructraos.orchestration.policy.activities.PolicyEvaluationActivitiesImpl;
import net.mudpot.constructraos.orchestration.system.workflows.HelloWorldWorkflowImpl;
import net.mudpot.constructraos.commons.orchestration.TaskQueues;

import java.util.concurrent.atomic.AtomicBoolean;

@Context
@Singleton
public class TemporalWorkerRuntime {

    private final TemporalWorkerConfig config;
    private final BeanContext beanContext;
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final WorkerFactory workerFactory;
    private final HelloActivitiesImpl helloActivities;
    private final PromptActivitiesImpl promptActivities;
    private final LlmActivitiesImpl llmActivities;
    private final PolicyEvaluationActivitiesImpl policyEvaluationActivities;
    private final ProjectRecordsActivitiesImpl projectRecordsActivities;

    @Inject
    public TemporalWorkerRuntime(
        final TemporalWorkerConfig config,
        final BeanContext beanContext,
        final HelloActivitiesImpl helloActivities,
        final PromptActivitiesImpl promptActivities,
        final LlmActivitiesImpl llmActivities,
        final PolicyEvaluationActivitiesImpl policyEvaluationActivities,
        final ProjectRecordsActivitiesImpl projectRecordsActivities,
        final OpenTracingOptions openTracingOptions
    ) {
        this.config = config;
        this.beanContext = beanContext;
        this.helloActivities = helloActivities;
        this.promptActivities = promptActivities;
        this.llmActivities = llmActivities;
        this.policyEvaluationActivities = policyEvaluationActivities;
        this.projectRecordsActivities = projectRecordsActivities;
        final WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder().setTarget(config.temporalAddress()).build()
        );
        final WorkflowClient client = WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder()
                .setNamespace(config.temporalNamespace())
                .setInterceptors(new OpenTracingClientInterceptor(openTracingOptions))
                .build()
        );

        this.workerFactory = WorkerFactory.newInstance(
            client,
            WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(new OpenTracingWorkerInterceptor(openTracingOptions))
                .build()
        );
        registerHelloWorker();
        registerTaskExecutionWorker();
        this.workerFactory.start();
    }

    private void registerHelloWorker() {
        final Worker helloWorker = workerFactory.newWorker(config.helloTaskQueue());
        helloWorker.registerWorkflowImplementationFactory(HelloWorldWorkflow.class, () -> beanContext.createBean(HelloWorldWorkflowImpl.class));
        helloWorker.registerActivitiesImplementations(helloActivities, promptActivities, llmActivities, policyEvaluationActivities);
    }

    private void registerTaskExecutionWorker() {
        final Worker taskWorker = workerFactory.newWorker(TaskQueues.TASK_COORDINATION);
        taskWorker.registerWorkflowImplementationFactory(TaskCoordinationWorkflow.class, () -> beanContext.createBean(TaskCoordinationWorkflowImpl.class));
        taskWorker.registerActivitiesImplementations(projectRecordsActivities, policyEvaluationActivities);
    }

    @EventListener
    void startup(final StartupEvent event) {
        ready.set(true);
        System.out.println("Temporal worker runtime started. namespace=" + config.temporalNamespace()
            + " hello_queue=" + config.helloTaskQueue()
            + " task_queue=" + TaskQueues.TASK_COORDINATION
            + " address=" + config.temporalAddress()
            + " server_port=8081");
    }

    public boolean isReady() {
        return ready.get();
    }

    @PreDestroy
    public void close() {
        ready.set(false);
        workerFactory.shutdown();
    }
}
