package com.acme.tracker;

import static com.acme.tracker._private.SubsystemLogger.LOGGER;

import java.util.function.Consumer;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service that tracks deployments using a ManagedExecutorService.
 * @author WildFly Extension
 */
public class TrackerService implements Service {

    private final ManagedExecutorService executor;
    private final long tick;

    public TrackerService(ManagedExecutorService executor, long tick) {
        System.out.println("TrackerService.TrackerService");
        this.executor = executor;
        this.tick = tick;
    }

    @Override
    public void start(StartContext context) {
        LOGGER.infof("Starting TrackerService with tick interval of %d seconds", tick);
        // TODO: Implement tracking logic using the executor and tick interval
    }

    @Override
    public void stop(StopContext context) {
        LOGGER.infof("Stopping TrackerService");
        // TODO: Implement cleanup logic
    }
}
