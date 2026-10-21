package com.logspectra.ai.prompt;

import com.logspectra.ai.model.CodeContext;
import com.logspectra.ai.model.CodeSnippet;

/**
 * Appends retrieved source snippets to an existing analysis prompt.
 */
public class CodeAwarePromptEnricher {

    public String enrich(String basePrompt, CodeContext codeContext) {
        if (basePrompt == null) {
            basePrompt = "";
        }
        if (codeContext == null || !codeContext.hasSnippets()) {
            String note = codeContext != null && codeContext.getNotes() != null
                ? codeContext.getNotes()
                : "No code snippets retrieved";
            return basePrompt + "\n\n## Codebase Context\n_(unavailable)_ " + note + "\n";
        }

        StringBuilder sb = new StringBuilder(basePrompt);
        sb.append("\n\n## Codebase Context\n");
        sb.append("Service: ").append(nullToEmpty(codeContext.getService())).append('\n');
        if (codeContext.getSourceRoot() != null) {
            sb.append("Source root: ").append(codeContext.getSourceRoot()).append('\n');
        }
        if (codeContext.getNotes() != null) {
            sb.append("Notes: ").append(codeContext.getNotes()).append('\n');
        }
        sb.append("\nUse the following source (stack-trace matched) when inferring root cause and suggested fixes.\n");
        sb.append("Prefer search/replace patches: set filePath + search (exact existing text) + replace.\n");
        sb.append("Only use newContent for a full-file rewrite when a small search block is impossible.\n\n");

        for (CodeSnippet snippet : codeContext.getSnippets()) {
            sb.append("### ").append(snippet.getRelativePath())
                .append(" (lines ").append(snippet.getStartLine())
                .append('-').append(snippet.getEndLine())
                .append(", focus ").append(snippet.getFocusLine()).append(")\n");
            if (snippet.getReason() != null) {
                sb.append("Matched via: ").append(snippet.getReason()).append('\n');
            }
            sb.append("```").append(nullToEmpty(snippet.getLanguage())).append('\n');
            sb.append(snippet.getContent());
            if (!snippet.getContent().endsWith("\n")) {
                sb.append('\n');
            }
            sb.append("```\n\n");
        }
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
