package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.ServiceSourceMapping;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Static / YAML-backed source mappings (fallback when Redis has no entry). */
public class ServiceSourceRegistry implements ServiceSourceLocator {

    private final Map<String, ServiceSourceMapping> byService = new LinkedHashMap<>();

    public ServiceSourceRegistry(Collection<ServiceSourceMapping> mappings) {
        if (mappings != null) {
            for (ServiceSourceMapping mapping : mappings) {
                if (mapping != null && mapping.getServiceName() != null && !mapping.getServiceName().isBlank()) {
                    byService.put(mapping.getServiceName().trim().toLowerCase(), mapping);
                }
            }
        }
    }

    @Override
    public Optional<ServiceSourceMapping> find(String projectId, String serviceName) {
        return findByService(serviceName);
    }

    public Optional<ServiceSourceMapping> findByService(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byService.get(serviceName.trim().toLowerCase()));
    }

    public Map<String, ServiceSourceMapping> all() {
        return Map.copyOf(byService);
    }
}
