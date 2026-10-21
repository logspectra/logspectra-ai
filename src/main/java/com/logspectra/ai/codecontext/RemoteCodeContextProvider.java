package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.CodeContext;
import com.logspectra.ai.model.CodeSnippet;
import com.logspectra.ai.model.ServiceSourceMapping;
import com.logspectra.ai.model.StackFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches stack-relevant source files via {@link RemoteSourceReader} (no disk clone).
 */
public class RemoteCodeContextProvider implements CodeContextProvider {

    private static final Logger log = LoggerFactory.getLogger(RemoteCodeContextProvider.class);

    private final ServiceSourceLocator locator;
    private final StackTraceFrameParser frameParser;
    private final RemoteSourceReader remoteSourceReader;
    private final int maxSnippets;
    private final int contextLines;
    private final int maxFileChars;

    public RemoteCodeContextProvider(ServiceSourceLocator locator,
                                     StackTraceFrameParser frameParser,
                                     RemoteSourceReader remoteSourceReader,
                                     int maxSnippets,
                                     int contextLines) {
        this(locator, frameParser, remoteSourceReader, maxSnippets, contextLines, 40_000);
    }

    public RemoteCodeContextProvider(ServiceSourceLocator locator,
                                     StackTraceFrameParser frameParser,
                                     RemoteSourceReader remoteSourceReader,
                                     int maxSnippets,
                                     int contextLines,
                                     int maxFileChars) {
        this.locator = locator;
        this.frameParser = frameParser;
        this.remoteSourceReader = remoteSourceReader;
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
                    + " (Redis registry or YAML). Register via logspectra-starter with git-url.")
                .build();
        }

        ServiceSourceMapping mapping = mappingOpt.get();
        if (mapping.getGitUrl() == null || mapping.getGitUrl().isBlank()) {
            return CodeContext.builder()
                .service(serviceName)
                .notes("Remote context requires gitUrl on the service registry mapping")
                .build();
        }

        List<StackFrame> allFrames = frameParser.parse(logs);
        List<StackFrame> frames = frameParser.preferApplicationFrames(allFrames, maxSnippets * 2);
        List<CodeSnippet> snippets = new ArrayList<>();

        for (StackFrame frame : frames) {
            if (snippets.size() >= maxSnippets) {
                break;
            }
            resolveSnippet(mapping, frame).ifPresent(snippets::add);
        }

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
                    .fileName(SourcePathResolver.simpleName(event.getLogger()) + ".java")
                    .lineNumber(1)
                    .rawLine(event.getLogger())
                    .build();
                resolveSnippet(mapping, synthetic).ifPresent(snippets::add);
            }
        }

        String notes = snippets.isEmpty()
            ? "GitHub/remote source found but no matching files for stack frames / loggers (gitUrl="
                + mapping.getGitUrl() + ")"
            : "Resolved " + snippets.size() + " snippet(s) via remote source reader (no local clone)";

        return CodeContext.builder()
            .service(serviceName)
            .sourceRoot(mapping.getGitUrl())
            .frames(frames)
            .snippets(snippets)
            .notes(notes)
            .build();
    }

    private Optional<CodeSnippet> resolveSnippet(ServiceSourceMapping mapping, StackFrame frame) {
        for (String relativePath : SourcePathResolver.candidatePaths(frame, mapping.getSourceRoots())) {
            Optional<String> content = remoteSourceReader.readFile(mapping, relativePath);
            if (content.isEmpty()) {
                continue;
            }
            return Optional.of(SourceSnippetBuilder.build(
                relativePath, relativePath, content.get(), frame, contextLines, maxFileChars));
        }
        log.debug("No remote file for frame {}.{} under {}", frame.getClassName(), frame.getMethodName(),
            mapping.getGitUrl());
        return Optional.empty();
    }
}
