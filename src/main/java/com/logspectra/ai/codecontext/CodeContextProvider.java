package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.CodeContext;
import java.util.List;

/**
 * Resolves source snippets for a service from stack traces / logger names.
 * Implementations: local filesystem now; git clone / RAG later.
 */
public interface CodeContextProvider {

    CodeContext resolve(String serviceName, List<AiLogEvent> logs);
}
