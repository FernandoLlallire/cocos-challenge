package com.cocos.llallire.infrastructure.observability;

import static com.cocos.llallire.infrastructure.observability.TelemetryTestSupport.histogramCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.cocos.llallire.domain.exception.UserNotFoundException;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * Tests of {@link UseCaseTelemetry}.
 */
class UseCaseTelemetryTest {

    @RegisterExtension
    static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

    private UseCaseTelemetry telemetry;

    @BeforeEach
    void setUp() {
        telemetry = new UseCaseTelemetry(OTEL.getOpenTelemetry());
    }

    @Test
    void successfulExecutionCreatesASpanAndRecordsTheDuration() {
        String result = telemetry.observe("GetPortfolio", () -> {
            assertThat(Span.current().getSpanContext().isValid()).as("the span is current inside the action").isTrue();
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        SpanData span = OTEL.getSpans().getFirst();
        assertThat(span.getName()).isEqualTo("UseCase GetPortfolio");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
        assertThat(histogramCount(OTEL.getMetrics(), "cocos.usecase.duration",
                Attributes.of(UseCaseTelemetry.USE_CASE, "GetPortfolio", UseCaseTelemetry.OUTCOME, "success")))
                .isEqualTo(1);
    }

    @Test
    void domainExceptionsAreRecordedButDoNotMarkTheSpanAsError() {
        assertThatThrownBy(() -> telemetry.observe("GetPortfolio", () -> {
            throw new UserNotFoundException(9L);
        })).isInstanceOf(UserNotFoundException.class);

        SpanData span = OTEL.getSpans().getFirst();
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
        assertThat(span.getEvents()).anySatisfy(event -> assertThat(event.getName()).isEqualTo("exception"));
        assertThat(histogramCount(OTEL.getMetrics(), "cocos.usecase.duration",
                Attributes.of(UseCaseTelemetry.USE_CASE, "GetPortfolio", UseCaseTelemetry.OUTCOME,
                        "UserNotFoundException")))
                .isEqualTo(1);
    }

    @Test
    void unexpectedExceptionsMarkTheSpanAsError() {
        assertThatThrownBy(() -> telemetry.observe("PlaceOrder", () -> {
            throw new IllegalStateException("database down");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(OTEL.getSpans().getFirst().getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
    }
}
