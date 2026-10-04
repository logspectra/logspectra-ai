package com.logspectra.ai.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/**
 * Neutral log line used by LogSpectra AI (no Elasticsearch coupling).
 */
@Value
@Builder
public class AiLogEvent {
    String id;
    String projectId;
    Instant timestamp;
    String level;
    String service;
    String traceId;
    String spanId;
    String thread;
    String logger;
    String message;
    String endpoint;
    String exception;
    String stackTrace;
    String host;
    String environment;
}
