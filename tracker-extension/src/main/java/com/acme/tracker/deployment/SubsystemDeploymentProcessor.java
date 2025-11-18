package com.acme.tracker.deployment;

import com.acme.tracker.SubsystemResourceDefinitionRegistrar;
import com.acme.tracker.TrackerService;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An example deployment unit processor that does nothing. To add more deployment
 * processors copy this class, and register it with the deployment chain via
 * {@link ${package}.SubsystemResourceDefinitionRegistrar${hash}accept(org.jboss.as.server.DeploymentProcessorTarget)}
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public enum SubsystemDeploymentProcessor implements DeploymentUnitProcessor {
    INSTANCE;

    private final Logger logger = Logger.getLogger(SubsystemDeploymentProcessor.class);

    /**
     * See {@link Phase} for a description of the different phases
     */
    public static final Phase PHASE = Phase.DEPENDENCIES;

    /**
     * The relative order of this processor within the {@link #PHASE}.
     * The current number is large enough for it to happen after all
     * the standard deployment unit processors that come with JBoss AS.
     */
    public static final int PRIORITY = 0x4000;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        this.logger.infof("Deploying %s", deploymentUnit.getName());

        // Install a service that depends on the TrackerService
        ServiceName deploymentServiceName = deploymentUnit.getServiceName().append("tracker-deployment");
        ServiceBuilder<?> serviceBuilder = phaseContext.getRequirementServiceTarget().addService(deploymentServiceName);
        Supplier<TrackerService> trackerServiceSupplier = serviceBuilder.requires(SubsystemResourceDefinitionRegistrar.TRACKER_CAPABILITY.getCapabilityServiceName());

        serviceBuilder.setInstance(new DeploymentTrackerService(trackerServiceSupplier, deploymentUnit.getName()));
        serviceBuilder.install();
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        this.logger.infof("Undeploying %s", unit.getName());
        // The service will be stopped automatically
    }

    private static class DeploymentTrackerService implements Service {
        private final Supplier<TrackerService> trackerServiceSupplier;
        private TrackerService trackerService;

        DeploymentTrackerService(Supplier<TrackerService> trackerServiceSupplier, String deploymentName) {
            this.trackerServiceSupplier = trackerServiceSupplier;
        }

        @Override
        public void start(StartContext context) {
            trackerService = trackerServiceSupplier.get();
            if (trackerService != null) {
                trackerService.DEPLOYMENTS.incrementAndGet();
            }
        }

        @Override
        public void stop(StopContext context) {
            if (trackerService != null) {
                trackerService.DEPLOYMENTS.decrementAndGet();
            }
        }
    }
}