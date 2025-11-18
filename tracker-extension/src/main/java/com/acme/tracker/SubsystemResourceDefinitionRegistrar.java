package com.acme.tracker;

import static com.acme.tracker._private.SubsystemLogger.LOGGER;
import static com.acme.tracker.deployment.SubsystemDeploymentProcessor.PHASE;
import static com.acme.tracker.deployment.SubsystemDeploymentProcessor.PRIORITY;
import static org.wildfly.service.Installer.StartWhen.AVAILABLE;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import com.acme.tracker.deployment.SubsystemDeploymentProcessor;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.jboss.as.controller.OperationStepHandler;

/**
 * Registers the resource definition of this subsystem.
 * @author Paul Ferraro
 */
public class SubsystemResourceDefinitionRegistrar implements org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar, Consumer<DeploymentProcessorTarget>, ResourceServiceConfigurator {

    static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of("tracker");

    public static NullaryServiceDescriptor<TrackerService> SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("com.acme.tracker", TrackerService.class);
    public static final RuntimeCapability<Void> TRACKER_CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).build();

    static final UnaryServiceDescriptor<ManagedScheduledExecutorService> EXECUTOR_SERVICE = UnaryServiceDescriptor.of("org.wildfly.ee.concurrent.scheduled-executor",
            ManagedScheduledExecutorService.class);

    static final CapabilityReferenceAttributeDefinition<ManagedScheduledExecutorService> EXECUTOR = new CapabilityReferenceAttributeDefinition.Builder<>("executor", CapabilityReference.builder(TRACKER_CAPABILITY, EXECUTOR_SERVICE).build())
            .build();

    protected static final AttributeDefinition TICK =
            new SimpleAttributeDefinitionBuilder("tick", ModelType.LONG)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(2))
                    .setValidator(LongRangeValidator.POSITIVE)
                    .setMeasurementUnit(MeasurementUnit.SECONDS)
                    .setRequired(false)
                    .build();

    protected static final AttributeDefinition DEPLOYMENTS =
            new SimpleAttributeDefinitionBuilder("deployments", ModelType.LONG)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(false)
                    .setStorageRuntime()
                    .build();


    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        ParentResourceDescriptionResolver resolver = new SubsystemResourceDescriptionResolver(REGISTRATION.getName(), SubsystemResourceDefinitionRegistrar.class);

        // Describe attribute and operations of resource
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(List.of(TICK, EXECUTOR))
                .addCapability(TRACKER_CAPABILITY)
                // Specify runtime behaviour of resource
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                // If this resource contributes to the deployment chain, register any deployment unit processors
                .withDeploymentChainContributor(this)
                .build();

        // Register the definition of this resource
        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(REGISTRATION, resolver).build());

        // Registers the attributes, operations, and capabilities of this resource based on our descriptor
        ManagementResourceRegistrar.of(descriptor).register(registration);

        // Register the DEPLOYMENTS attribute with a custom read handler
        registration.registerReadOnlyAttribute(DEPLOYMENTS, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.addStep((ctx, op) -> {
                    TrackerService trackerService = ctx.getCapabilityRuntimeAPI(
                            TRACKER_CAPABILITY.getName(),
                            TrackerService.class
                    );
                    if (trackerService != null) {
                        ctx.getResult().set(trackerService.deployments.get());
                    } else {
                        ctx.getResult().set(0L);
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        });
        return registration;
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(REGISTRATION.getName(), PHASE, PRIORITY, new SubsystemDeploymentProcessor());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {

        LOGGER.activatingSubsystem();

        long tick = TICK.resolveModelAttribute(context, model).asLong();

        ServiceDependency<ManagedScheduledExecutorService> executor = EXECUTOR.resolve(context, model);
        Supplier<TrackerService> trackerService = () -> new TrackerService(executor.get(), tick);

        return CapabilityServiceInstaller.builder(TRACKER_CAPABILITY, trackerService)
                .provides(TRACKER_CAPABILITY.getCapabilityServiceName())
                .requires(executor)
                .startWhen(AVAILABLE)
                .onStop(TrackerService::stop)
                .build();
    }

}