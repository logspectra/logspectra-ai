package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.CodeContext;
import java.util.List;

public class NoOpCodeContextProvider implements CodeContextProvider {

    @Override
    public CodeContext resolve(String serviceName, List<AiLogEvent> logs) {
        return CodeContext.empty(serviceName);
    }
}
