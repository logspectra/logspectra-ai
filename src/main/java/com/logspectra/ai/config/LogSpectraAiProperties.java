package com.logspectra.ai.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "logspectra.ai")
public class LogSpectraAiProperties {

    /** Master switch for the library facade. */
    private boolean enabled = true;

    private CodeContext codeContext = new CodeContext();

    @Data
    public static class CodeContext {
        private boolean enabled = true;
        /** local | git (git currently falls back to local when local-path is set). */
        private String provider = "local";
        private int maxSnippets = 8;
        private int contextLines = 20;
        private Map<String, ServiceMapping> services = new LinkedHashMap<>();
    }

    @Data
    public static class ServiceMapping {
        private String localPath;
        private String gitUrl;
        private String defaultBranch = "main";
        private List<String> sourceRoots = new ArrayList<>(List.of("src/main/java", "src/main/kotlin"));
    }
}
