package com.logspectra.ai.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Maps a log {@code service} name to a local checkout (or future git clone root).
 */
@Value
@Builder
public class ServiceSourceMapping {
    String serviceName;
    /** Absolute or expandable path to the service repository root. */
    String localPath;
    /** Optional git URL for later clone/index (not used by local provider). */
    String gitUrl;
    String defaultBranch;
    @Builder.Default
    List<String> sourceRoots = List.of("src/main/java", "src/main/kotlin");
}
