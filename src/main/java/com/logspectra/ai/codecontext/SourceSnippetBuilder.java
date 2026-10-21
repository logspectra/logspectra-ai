package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.CodeSnippet;
import com.logspectra.ai.model.StackFrame;

/**
 * Builds numbered source snippets for stack-matched files.
 * Prefer full file (capped) so the LLM can emit precise search/replace patches.
 */
public final class SourceSnippetBuilder {

    private SourceSnippetBuilder() {}

    /**
     * @param maxFileChars when &gt; 0 and file fits, include the whole file; otherwise a window around the focus line
     */
    public static CodeSnippet build(String relativePath,
                                    String absolutePath,
                                    String fileContent,
                                    StackFrame frame,
                                    int contextLines,
                                    int maxFileChars) {
        String[] lines = fileContent.split("\\R", -1);
        if (lines.length == 0) {
            lines = new String[]{""};
        }
        int focus = frame.getLineNumber() > 0 ? frame.getLineNumber() : 1;
        focus = Math.min(Math.max(focus, 1), lines.length);

        int start;
        int end;
        boolean fullFile = maxFileChars > 0 && fileContent.length() <= maxFileChars;
        if (fullFile) {
            start = 1;
            end = lines.length;
        } else {
            int window = Math.max(5, contextLines);
            start = Math.max(1, focus - window);
            end = Math.min(lines.length, focus + window);
        }

        StringBuilder content = new StringBuilder();
        for (int i = start; i <= end; i++) {
            content.append(String.format("%4d | %s%n", i, lines[i - 1]));
        }

        String language = relativePath.endsWith(".kt") ? "kotlin" : "java";
        String reason = "stack:" + frame.getClassName() + "." + frame.getMethodName()
            + (fullFile ? "+fullFile" : "+window");

        return CodeSnippet.builder()
            .relativePath(relativePath)
            .absolutePath(absolutePath)
            .startLine(start)
            .endLine(end)
            .focusLine(focus)
            .language(language)
            .content(content.toString())
            .reason(reason)
            .build();
    }
}
