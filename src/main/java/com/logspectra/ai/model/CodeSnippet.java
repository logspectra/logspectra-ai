package com.logspectra.ai.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CodeSnippet {
    String relativePath;
    String absolutePath;
    int startLine;
    int endLine;
    int focusLine;
    String language;
    String content;
    String reason;
}
