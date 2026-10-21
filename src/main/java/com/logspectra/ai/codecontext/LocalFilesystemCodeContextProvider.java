package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.CodeContext;
import com.logspectra.ai.model.CodeSnippet;
import com.logspectra.ai.model.ServiceSourceMapping;
import com.logspectra.ai.model.StackFrame;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads source files from a local checkout configured per service.
 * Default for local/dev; replace/augment with git clone or RAG later.
 */
public class LocalFilesystemCodeContextProvider implements CodeContextProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalFilesystemCodeContextProvider.class);

    private final ServiceSourceLocator locator;
    private final StackTraceFrameParser frameParser;
    private final int maxSnippets;
    private final int contextLines;
    private final int maxFileChars;

    public LocalFilesystemCodeContextProvider(ServiceSourceLocator locator,
                                              StackTraceFrameParser frameParser,
                                              int maxSnippets,
                                              int contextLines) {
        this(locator, frameParser, maxSnippets, contextLines, 40_000);
    }

    public LocalFilesystemCodeContextProvider(ServiceSourceLocator locator,
                                              StackTraceFrameParser frameParser,
                                              int maxSnippets,
                                              int contextLines,
                                              int maxFileChars) {
        this.locator = locator;
        this.frameParser = frameParser;
        this.maxSnippets = Math.max(1, maxSnippets);
        this.contextLines = Math.max(5, contextLines);
        this.maxFileChars = Math.max(0, maxFileChars);
    }

    @Override
    public CodeContext resolve(String serviceName, List<AiLogEvent> logs) {
        String projectId = logs == null ? null : logs.stream()
            .map(AiLogEvent::getProjectId)
            .filter(id -> id != null && !id.isBlank())
            .findFirst()
            .orElse(null);

        Optional<ServiceSourceMapping> mappingOpt = locator.find(projectId, serviceName);
        if (mappingOpt.isEmpty()) {
            return CodeContext.builder()
                .service(serviceName)
                .notes("No source mapping for service=" + serviceName
                    + " (Redis registry or YAML). Client services should register via logspectra-starter.")
                .build();
        }

        ServiceSourceMapping mapping = mappingOpt.get();
        Path root = Path.of(expandHome(mapping.getLocalPath())).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return CodeContext.builder()
                .service(serviceName)
                .sourceRoot(root.toString())
                .notes("Configured local-path does not exist: " + root
                    + (mapping.getGitUrl() != null ? " (gitUrl=" + mapping.getGitUrl() + " — clone it first)" : ""))
                .build();
        }

        List<StackFrame> allFrames = frameParser.parse(logs);
        List<StackFrame> frames = frameParser.preferApplicationFrames(allFrames, maxSnippets * 2);
        List<CodeSnippet> snippets = new ArrayList<>();

        for (StackFrame frame : frames) {
            if (snippets.size() >= maxSnippets) {
                break;
            }
            findFile(root, mapping.getSourceRoots(), frame).ifPresent(file -> {
                readSnippet(file, root, frame).ifPresent(snippets::add);
            });
        }

        // Fallback: logger FQCN → source file
        if (snippets.isEmpty() && logs != null) {
            for (AiLogEvent event : logs) {
                if (snippets.size() >= maxSnippets) {
                    break;
                }
                if (event.getLogger() == null || event.getLogger().isBlank()) {
                    continue;
                }
                StackFrame synthetic = StackFrame.builder()
                    .className(event.getLogger())
                    .methodName("log")
                    .fileName(simpleName(event.getLogger()) + ".java")
                    .lineNumber(1)
                    .rawLine(event.getLogger())
                    .build();
                findFile(root, mapping.getSourceRoots(), synthetic).ifPresent(file ->
                    readSnippet(file, root, synthetic).ifPresent(snippets::add));
            }
        }

        String notes = snippets.isEmpty()
            ? "Source root found but no matching files for stack frames / loggers"
            : "Resolved " + snippets.size() + " snippet(s) from local filesystem";

        return CodeContext.builder()
            .service(serviceName)
            .sourceRoot(root.toString())
            .frames(frames)
            .snippets(snippets)
            .notes(notes)
            .build();
    }

    private Optional<Path> findFile(Path root, List<String> sourceRoots, StackFrame frame) {
        String fileName = frame.getFileName();
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }

        String packagePath = frame.packagePrefix().replace('.', '/');
        List<String> roots = (sourceRoots == null || sourceRoots.isEmpty())
            ? List.of("src/main/java")
            : sourceRoots;

        for (String sourceRoot : roots) {
            Path candidate = root.resolve(sourceRoot).resolve(packagePath).resolve(fileName).normalize();
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }

        // Last resort: walk for file name (bounded)
        try (Stream<Path> walk = Files.walk(root, 12)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().equals(fileName))
                .filter(p -> packagePath.isBlank() || p.toString().replace('\\', '/').contains(packagePath))
                .findFirst();
        } catch (IOException ex) {
            log.debug("Walk failed under {}: {}", root, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<CodeSnippet> readSnippet(Path file, Path root, StackFrame frame) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            if (raw.isEmpty()) {
                return Optional.empty();
            }
            String relative = root.relativize(file).toString().replace('\\', '/');
            return Optional.of(SourceSnippetBuilder.build(
                relative,
                file.toAbsolutePath().toString(),
                raw,
                frame,
                contextLines,
                maxFileChars));
        } catch (IOException ex) {
            log.debug("Failed reading {}: {}", file, ex.getMessage());
            return Optional.empty();
        }
    }

    private static String simpleName(String fqcn) {
        int idx = fqcn.lastIndexOf('.');
        return idx >= 0 ? fqcn.substring(idx + 1) : fqcn;
    }

    private static String expandHome(String path) {
        if (path == null) {
            return "";
        }
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
