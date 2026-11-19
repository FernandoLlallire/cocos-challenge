package com.cocos.llallire.infrastructure.observability;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.MetricData;

/**
 * Helpers to read metrics captured by the OpenTelemetry testing SDK.
 */
public final class TelemetryTestSupport {

    /**
     * Utility class; not instantiable.
     */
    private TelemetryTestSupport() {
    }

    /**
     * Returns the value of a long counter for the given attributes, or 0 if it was never incremented.
     *
     * @param metrics    exported metrics
     * @param name       metric name
     * @param attributes exact attributes of the point
     * @return counter value
     */
    public static long longCounter(List<MetricData> metrics, String name, Attributes attributes) {
        return metrics.stream()
                .filter(metric -> metric.getName().equals(name))
                .flatMap(metric -> metric.getLongSumData().getPoints().stream())
                .filter(point -> point.getAttributes().equals(attributes))
                .mapToLong(point -> point.getValue())
                .sum();
    }

    /**
     * Returns the value of a double counter for the given attributes, or 0 if it was never incremented.
     *
     * @param metrics    exported metrics
     * @param name       metric name
     * @param attributes exact attributes of the point
     * @return counter value
     */
    public static double doubleCounter(List<MetricData> metrics, String name, Attributes attributes) {
        return metrics.stream()
                .filter(metric -> metric.getName().equals(name))
                .flatMap(metric -> metric.getDoubleSumData().getPoints().stream())
                .filter(point -> point.getAttributes().equals(attributes))
                .mapToDouble(point -> point.getValue())
                .sum();
    }

    /**
     * Returns how many values a histogram recorded for the given attributes.
     *
     * @param metrics    exported metrics
     * @param name       metric name
     * @param attributes exact attributes of the point
     * @return number of recordings
     */
    public static long histogramCount(List<MetricData> metrics, String name, Attributes attributes) {
        return metrics.stream()
                .filter(metric -> metric.getName().equals(name))
                .flatMap(metric -> metric.getHistogramData().getPoints().stream())
                .filter(point -> point.getAttributes().equals(attributes))
                .mapToLong(point -> point.getCount())
                .sum();
    }
}
