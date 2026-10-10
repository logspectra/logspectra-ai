package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.ServiceSourceMapping;
import java.util.Optional;

/**
 * Host-provided port for reading source files without a local clone
 * (e.g. GitHub Contents API via logspectra-backend).
 *
 * <p>logspectra-ai stays free of GitHub credentials / HTTP clients.
 */
@FunctionalInterface
public interface RemoteSourceReader {

    /**
     * @param mapping service registry entry (must include gitUrl / defaultBranch when reading from GitHub)
     * @param relativePath path relative to repo root, e.g. {@code src/main/java/.../Foo.java}
     * @return UTF-8 file contents when found
     */
    Optional<String> readFile(ServiceSourceMapping mapping, String relativePath);
}
