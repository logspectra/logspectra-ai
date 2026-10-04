package com.logspectra.ai.codecontext;

import static org.assertj.core.api.Assertions.assertThat;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.CodeContext;
import com.logspectra.ai.model.ServiceSourceMapping;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFilesystemCodeContextProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesSnippetFromStackFrame() throws Exception {
        Path javaFile = tempDir.resolve("src/main/java/com/example/payment/api/DemoController.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
            package com.example.payment.api;

            public class DemoController {
                public void errorDemo() {
                    throw new IllegalStateException("boom");
                }
            }
            """);

        ServiceSourceRegistry registry = new ServiceSourceRegistry(List.of(
            ServiceSourceMapping.builder()
                .serviceName("payment-service")
                .localPath(tempDir.toString())
                .sourceRoots(List.of("src/main/java"))
                .build()
        ));

        LocalFilesystemCodeContextProvider provider = new LocalFilesystemCodeContextProvider(
            (ServiceSourceLocator) registry, new StackTraceFrameParser(), 5, 10
        );

        String stack = "at com.example.payment.api.DemoController.errorDemo(DemoController.java:5)";
        CodeContext context = provider.resolve(
            "payment-service",
            List.of(AiLogEvent.builder().service("payment-service").stackTrace(stack).build())
        );

        assertThat(context.hasSnippets()).isTrue();
        assertThat(context.getSnippets().getFirst().getRelativePath())
            .contains("DemoController.java");
        assertThat(context.getSnippets().getFirst().getContent()).contains("IllegalStateException");
    }
}
