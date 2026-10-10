package com.logspectra.ai.codecontext;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logspectra.ai.model.StackFrame;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourcePathResolverTest {

    @Test
    void candidatePaths_includePackageUnderSourceRoot() {
        StackFrame frame = StackFrame.builder()
            .className("com.example.gateway.api.DemoController")
            .methodName("errorBurst")
            .fileName("DemoController.java")
            .lineNumber(40)
            .build();

        List<String> paths = SourcePathResolver.candidatePaths(frame, List.of("src/main/java"));
        assertTrue(paths.contains("src/main/java/com/example/gateway/api/DemoController.java"));
    }
}
