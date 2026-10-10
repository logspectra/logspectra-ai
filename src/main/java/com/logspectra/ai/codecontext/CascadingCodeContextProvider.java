package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.CodeContext;
import java.util.List;

/**
 * Prefer local checkout when available; otherwise use remote (GitHub) reader.
 */
public class CascadingCodeContextProvider implements CodeContextProvider {

    private final LocalFilesystemCodeContextProvider local;
    private final RemoteCodeContextProvider remote;

    public CascadingCodeContextProvider(LocalFilesystemCodeContextProvider local,
                                        RemoteCodeContextProvider remote) {
        this.local = local;
        this.remote = remote;
    }

    @Override
    public CodeContext resolve(String serviceName, List<AiLogEvent> logs) {
        CodeContext localCtx = local.resolve(serviceName, logs);
        if (localCtx.getSnippets() != null && !localCtx.getSnippets().isEmpty()) {
            return localCtx;
        }
        CodeContext remoteCtx = remote.resolve(serviceName, logs);
        if (remoteCtx.getSnippets() != null && !remoteCtx.getSnippets().isEmpty()) {
            return remoteCtx;
        }
        String notes = "local: " + nullSafe(localCtx.getNotes()) + "; remote: " + nullSafe(remoteCtx.getNotes());
        return CodeContext.builder()
            .service(serviceName)
            .sourceRoot(remoteCtx.getSourceRoot() != null ? remoteCtx.getSourceRoot() : localCtx.getSourceRoot())
            .frames(remoteCtx.getFrames() != null && !remoteCtx.getFrames().isEmpty()
                ? remoteCtx.getFrames() : localCtx.getFrames())
            .snippets(List.of())
            .notes(notes)
            .build();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
