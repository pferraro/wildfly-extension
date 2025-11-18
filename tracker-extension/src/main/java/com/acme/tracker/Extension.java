package com.acme.tracker;

import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemPersistence;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Extension extends org.wildfly.subsystem.SubsystemExtension<SubsystemSchema> {

    public Extension() {
        super(SubsystemConfiguration.of(SubsystemResourceDefinitionRegistrar.REGISTRATION,
                SubsystemModel.CURRENT,
                SubsystemResourceDefinitionRegistrar::new),
                SubsystemPersistence.of(SubsystemSchema.CURRENT));
    }
}
