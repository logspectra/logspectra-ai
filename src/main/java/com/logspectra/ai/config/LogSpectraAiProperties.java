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
        /**
         * local | git | github.
         * <ul>
         *   <li>local — filesystem; if host provides {@code RemoteSourceReader}, cascades to GitHub</li>
         *   <li>git — local when local-path set; else cascade / stub</li>
         *   <li>github — remote Contents API only (no disk)</li>
         * </ul>
         */
        private String provider = "local";
        private int maxSnippets = 8;
        private int contextLines = 20;
        /**
         * When a stack-matched file is at most this many characters, send the full file
         * (Approach 2) so the model can emit search/replace patches. Larger files fall back
         * to a ±contextLines window.
         */
        private int maxFileChars = 40_000;
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
