package com.logspectra.ai.codecontext;

import static org.assertj.core.api.Assertions.assertThat;

import com.logspectra.ai.model.AiLogEvent;
import com.logspectra.ai.model.StackFrame;
import java.util.List;
import org.junit.jupiter.api.Test;

class StackTraceFrameParserTest {

    private final StackTraceFrameParser parser = new StackTraceFrameParser();

    @Test
    void parsesJavaStackFrames() {
        String stack = """
            java.lang.IllegalStateException: boom
            	at com.example.payment.api.DemoController.errorDemo(DemoController.java:27)
            	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:255)
            """;

        List<StackFrame> frames = parser.parse(List.of(AiLogEvent.builder().stackTrace(stack).build()));
        List<StackFrame> app = parser.preferApplicationFrames(frames, 5);

        assertThat(app).isNotEmpty();
        assertThat(app.getFirst().getClassName()).isEqualTo("com.example.payment.api.DemoController");
        assertThat(app.getFirst().getLineNumber()).isEqualTo(27);
        assertThat(app.getFirst().getFileName()).isEqualTo("DemoController.java");
    }
}
