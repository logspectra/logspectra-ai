package com.logspectra.ai.model;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CodeContext {
    String service;
    String sourceRoot;
    @Builder.Default
    List<StackFrame> frames = Collections.emptyList();
    @Builder.Default
    List<CodeSnippet> snippets = Collections.emptyList();
    String notes;

    public static CodeContext empty(String service) {
        return CodeContext.builder()
            .service(service)
            .notes("No code context available")
            .build();
    }

    public boolean hasSnippets() {
        return snippets != null && !snippets.isEmpty();
    }
}
