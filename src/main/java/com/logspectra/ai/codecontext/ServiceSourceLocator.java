package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.ServiceSourceMapping;
import java.util.Optional;

/**
 * Resolves where a service's source lives (YAML, Redis registry, etc.).
 */
@FunctionalInterface
public interface ServiceSourceLocator {

    Optional<ServiceSourceMapping> find(String projectId, String serviceName);
}
