package com.logspectra.ai.codecontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logspectra.ai.model.CodeSnippet;
import com.logspectra.ai.model.StackFrame;
import org.junit.jupiter.api.Test;

class SourceSnippetBuilderTest {

    @Test
    void prefersFullFileWhenUnderCap() {
        String file = "line1\nline2\nline3\n";
        StackFrame frame = StackFrame.builder()
            .className("com.example.Checkout")
            .methodName("total")
            .fileName("Checkout.java")
            .lineNumber(2)
            .build();

        CodeSnippet snippet = SourceSnippetBuilder.build(
            "src/Checkout.java", "/tmp/Checkout.java", file, frame, 1, 10_000);

        assertEquals(1, snippet.getStartLine());
        assertTrue(snippet.getEndLine() >= 3);
        assertTrue(snippet.getReason().contains("fullFile"));
        assertTrue(snippet.getContent().contains("line1"));
        assertTrue(snippet.getContent().contains("line3"));
    }

    @Test
    void usesWindowWhenFileTooLarge() {
        String file = "a\n".repeat(200);
        StackFrame frame = StackFrame.builder()
            .className("com.example.Big")
            .methodName("run")
            .fileName("Big.java")
            .lineNumber(100)
            .build();

        CodeSnippet snippet = SourceSnippetBuilder.build(
            "src/Big.java", "/tmp/Big.java", file, frame, 5, 50);

        assertTrue(snippet.getReason().contains("window"));
        assertEquals(95, snippet.getStartLine());
        assertEquals(105, snippet.getEndLine());
    }
}
