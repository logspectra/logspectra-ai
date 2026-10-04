package com.logspectra.ai;

import com.logspectra.ai.codecontext.CodeContextProvider;
import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.CodeContext;
import com.logspectra.ai.prompt.CodeAwarePromptEnricher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for logspectra-backend: resolve code context and enrich analysis prompts.
 * LLM providers remain behind the backend (or future provider SPI in this library).
 */
public class LogSpectraAiFacade {

    private static final Logger log = LoggerFactory.getLogger(LogSpectraAiFacade.class);

    private final CodeContextProvider codeContextProvider;
    private final CodeAwarePromptEnricher promptEnricher;
    private final boolean codeContextEnabled;

    public LogSpectraAiFacade(CodeContextProvider codeContextProvider,
                              CodeAwarePromptEnricher promptEnricher,
                              boolean codeContextEnabled) {
        this.codeContextProvider = codeContextProvider;
        this.promptEnricher = promptEnricher;
        this.codeContextEnabled = codeContextEnabled;
    }

    public CodeContext resolveCodeContext(String serviceName, List<AiLogEvent> logs) {
        if (!codeContextEnabled) {
            return CodeContext.builder()
                .service(serviceName)
                .notes("Code context disabled (logspectra.ai.code-context.enabled=false)")
                .build();
        }
        try {
            CodeContext context = codeContextProvider.resolve(serviceName, logs);
            log.debug("operation=resolveCodeContext feature=logspectra-ai service={} snippets={} notes={}",
                serviceName,
                context.getSnippets() != null ? context.getSnippets().size() : 0,
                context.getNotes());
            return context;
        } catch (Exception ex) {
            log.warn("operation=resolveCodeContext feature=logspectra-ai status=error service={} error={}",
                serviceName, ex.getMessage());
            return CodeContext.builder()
                .service(serviceName)
                .notes("Code context failed: " + ex.getMessage())
                .build();
        }
    }

    /**
     * Takes a log-only analysis prompt and appends retrieved codebase snippets.
     */
    public String enrichPrompt(String basePrompt, String serviceName, List<AiLogEvent> logs) {
        CodeContext context = resolveCodeContext(serviceName, logs);
        return promptEnricher.enrich(basePrompt, context);
    }

    public boolean isCodeContextEnabled() {
        return codeContextEnabled;
    }
}
