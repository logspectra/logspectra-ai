package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.StackFrame;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Java-style stack frames from log stack traces / messages.
 */
public class StackTraceFrameParser {

    private static final Pattern FRAME = Pattern.compile(
        "\\s*at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(([^:)]+)(?::(\\d+))?\\)"
    );

    public List<StackFrame> parse(List<AiLogEvent> logs) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<StackFrame> frames = new ArrayList<>();
        for (AiLogEvent log : logs) {
            appendFromText(log.getStackTrace(), seen, frames);
            appendFromText(log.getException(), seen, frames);
            appendFromText(log.getMessage(), seen, frames);
        }
        return frames;
    }

    private void appendFromText(String text, LinkedHashSet<String> seen, List<StackFrame> frames) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = FRAME.matcher(text);
        while (matcher.find()) {
            String className = matcher.group(1);
            String method = matcher.group(2);
            String file = matcher.group(3);
            int line = 0;
            if (matcher.group(4) != null) {
                try {
                    line = Integer.parseInt(matcher.group(4));
                } catch (NumberFormatException ignored) {
                    line = 0;
                }
            }
            if (file == null || "Native Method".equals(file) || "Unknown Source".equals(file)) {
                continue;
            }
            // Prefer application frames; keep JDK frames only if nothing else later.
            String key = className + "#" + method + ":" + line;
            if (!seen.add(key)) {
                continue;
            }
            frames.add(StackFrame.builder()
                .className(className)
                .methodName(method)
                .fileName(file)
                .lineNumber(line)
                .rawLine(matcher.group(0).trim())
                .build());
        }
    }

    public List<StackFrame> preferApplicationFrames(List<StackFrame> frames, int limit) {
        List<StackFrame> app = frames.stream()
            .filter(f -> !isLibraryFrame(f.getClassName()))
            .limit(limit)
            .toList();
        if (!app.isEmpty()) {
            return app;
        }
        return frames.stream().limit(limit).toList();
    }

    private boolean isLibraryFrame(String className) {
        if (className == null) {
            return true;
        }
        return className.startsWith("java.")
            || className.startsWith("javax.")
            || className.startsWith("jakarta.")
            || className.startsWith("sun.")
            || className.startsWith("jdk.")
            || className.startsWith("org.springframework.")
            || className.startsWith("org.apache.")
            || className.startsWith("com.fasterxml.")
            || className.startsWith("reactor.")
            || className.startsWith("io.netty.")
            || className.startsWith("kotlin.")
            || className.startsWith("scala.");
    }
}
