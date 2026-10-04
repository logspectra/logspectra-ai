package com.logspectra.ai.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StackFrame {
    String className;
    String methodName;
    String fileName;
    int lineNumber;
    String rawLine;

    public String packagePrefix() {
        if (className == null || !className.contains(".")) {
            return "";
        }
        return className.substring(0, className.lastIndexOf('.'));
    }

    public String simpleClassName() {
        if (className == null || className.isBlank()) {
            return fileName != null ? fileName.replace(".java", "") : "";
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
}
