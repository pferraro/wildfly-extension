package com.acme.tracker;

import static com.acme.tracker._private.SubsystemLogger.LOGGER;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
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
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

import com.acme.tracker.deployment.SubsystemDeploymentProcessor;

/**
 * Registers the resource definition of this subsystem.
 * @author Paul Ferraro
 */
public class SubsystemResourceDefinitionRegistrar implements org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar, Consumer<DeploymentProcessorTarget>, ResourceServiceConfigurator {

    static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of("tracker");

    // This resource provides a capability whose service provides Foo.
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(NullaryServiceDescriptor.of("mycompany.foo", String.class)).build();

    protected static final AttributeDefinition TICK =
            new SimpleAttributeDefinitionBuilder("tick", ModelType.LONG)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(2))
                    .setValidator(LongRangeValidator.POSITIVE)
                    .setMeasurementUnit(MeasurementUnit.SECONDS)
                    .setRequired(false)
                    .build();

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        ParentResourceDescriptionResolver resolver = new SubsystemResourceDescriptionResolver(REGISTRATION.getName(), SubsystemResourceDefinitionRegistrar.class);

        // Describe attribute and operations of resource
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(List.of(TICK))
//                .addCapability(CAPABILITY)
                // Specify runtime behaviour of resource
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                // If this resource contributes to the deployment chain, register any deployment unit processors
                .withDeploymentChainContributor(this)
                .build();

        // Register the definition of this resource
        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(REGISTRATION, resolver).build());

        // Registers the attributes, operations, and capabilities of this resource based on our descriptor
        ManagementResourceRegistrar.of(descriptor).register(registration);

        // Register any child resources
        // new MyChildResourceDefinitionRegistrar(resolver).register(registration, context);

        return registration;
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(REGISTRATION.getName(), SubsystemDeploymentProcessor.PHASE, SubsystemDeploymentProcessor.PRIORITY, SubsystemDeploymentProcessor.INSTANCE);
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {

        LOGGER.activatingSubsystem();

        long tick = TICK.resolveModelAttribute(context, model).asLong();
        LOGGER.checkTick(tick);
        // Resolve service dependency from model
        //ServiceDependency<Bar> bar = BAR.resolve(context, model);

        //Supplier<SimpleFoo> factory = () -> new SimpleFoo(bar.get());
        Supplier<String> factory = () -> "foo";
        // e.g. Installs service that create Foo from the specified factory when the service starts
        return CapabilityServiceInstaller.builder(CAPABILITY, factory)
                // Specify any special behaviour on service stop
                //                .onStop(SimpleFoo::close)
                // Indicate when this service should start
                .startWhen(StartWhen.AVAILABLE) // e.g. Auto-start this service when all of its dependencies have started
                // Specify any dependencies of this service
                //.requires(List.of(bar))
                .build();

    }

}