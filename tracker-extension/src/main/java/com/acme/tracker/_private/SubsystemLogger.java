package com.acme.tracker._private;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import org.jboss.logging.BasicLogger;

@MessageLogger(projectCode = "ACME", length = 4)
public interface SubsystemLogger extends BasicLogger {

    SubsystemLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), SubsystemLogger.class, "com.acme.subsystem");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating com.acme:tracker Subsystem")
    void activatingSubsystem();

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Check the deployments every %s seconds")
    void checkTick(long seconds);

    @LogMessage(level = INFO)
    @Message(id = 3, value = "There are %s deployments in the server")
    void numberOfDeployments(int i);
}