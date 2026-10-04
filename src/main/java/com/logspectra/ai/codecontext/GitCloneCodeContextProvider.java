package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.CodeContext;
import com.logspectra.ai.model.ServiceSourceMapping;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder for future git-based context.
 * Currently delegates to {@link LocalFilesystemCodeContextProvider} when a localPath exists;
 * otherwise returns notes telling operators how to clone.
 *
 * <p>Replace this class (or wrap a real cloner) when you wire git credentials / CI index.
 */
public class GitCloneCodeContextProvider implements CodeContextProvider {

    private static final Logger log = LoggerFactory.getLogger(GitCloneCodeContextProvider.class);

    private final ServiceSourceLocator locator;
    private final LocalFilesystemCodeContextProvider localProvider;

    public GitCloneCodeContextProvider(ServiceSourceLocator locator,
                                       LocalFilesystemCodeContextProvider localProvider) {
        this.locator = locator;
        this.localProvider = localProvider;
    }

    @Override
    public CodeContext resolve(String serviceName, List<AiLogEvent> logs) {
        String projectId = logs == null ? null : logs.stream()
            .map(AiLogEvent::getProjectId)
            .filter(id -> id != null && !id.isBlank())
            .findFirst()
            .orElse(null);
        Optional<ServiceSourceMapping> mapping = locator.find(projectId, serviceName);
        if (mapping.isEmpty()) {
            return CodeContext.empty(serviceName);
        }

        ServiceSourceMapping source = mapping.get();
        if (source.getLocalPath() != null && !source.getLocalPath().isBlank()) {
            return localProvider.resolve(serviceName, logs);
        }

        log.info("Git clone provider not implemented yet for service={} gitUrl={}",
            serviceName, source.getGitUrl());
        return CodeContext.builder()
            .service(serviceName)
            .notes("Git clone/index not wired yet. Set local-path for now, or follow SETUP-CODE-CONTEXT.md "
                + "to implement clone for gitUrl=" + source.getGitUrl())
            .build();
    }
}
