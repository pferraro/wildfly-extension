package com.acme.tracker;

import static com.acme.tracker._private.SubsystemLogger.LOGGER;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service that tracks the number of deployments
 */
public class TrackerService  {

    private final ScheduledFuture<?> future;

    public final AtomicInteger deployments = new AtomicInteger(0);

    public TrackerService(ManagedScheduledExecutorService executor, long tick) {
        LOGGER.checkTick(tick);
        future = executor.scheduleAtFixedRate(() -> {
            LOGGER.numberOfDeployments(deployments.get());
        }, tick, tick, TimeUnit.SECONDS);
    }


    public void stop() {
        future.cancel(true);
    }
}
