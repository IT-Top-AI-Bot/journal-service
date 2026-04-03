package com.aquadev.journalservice.tracing;

import com.aquadev.journalservice.model.HomeworkExecution;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeworkExecutionSpan {

    private static final String TRACER_NAME = "journal-service";
    private static final String SPAN_NAME = "homework.execution";

    private final OpenTelemetry openTelemetry;

    public void run(HomeworkExecution execution, Runnable action) {
        Span span = openTelemetry.getTracer(TRACER_NAME)
                .spanBuilder(SPAN_NAME)
                .setNoParent()
                .setAttribute("execution.id", execution.getId().toString())
                .setAttribute("homework.id", execution.getHomeworkId())
                .setAttribute("user.telegram_id", execution.getUser().getTelegramId())
                .startSpan();
        try (var _ = span.makeCurrent()) {
            action.run();
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
