package com.logspectra.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.logspectra.ai.model.CodeContext;
import com.logspectra.ai.model.CodeSnippet;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodeAwarePromptEnricherTest {

    private final CodeAwarePromptEnricher enricher = new CodeAwarePromptEnricher();

    @Test
    void appendsSnippets() {
        CodeContext context = CodeContext.builder()
            .service("payment-service")
            .snippets(List.of(CodeSnippet.builder()
                .relativePath("src/main/java/Demo.java")
                .startLine(1)
                .endLine(3)
                .focusLine(2)
                .language("java")
                .content("1 | class Demo {}\n")
                .reason("stack:Demo.main")
                .build()))
            .build();

        String enriched = enricher.enrich("## Logs\nok", context);
        assertThat(enriched).contains("## Codebase Context");
        assertThat(enriched).contains("Demo.java");
        assertThat(enriched).contains("class Demo");
    }
}
