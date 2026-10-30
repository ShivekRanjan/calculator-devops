package com.spe.calculator.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;

/**
 * Structured error body returned with HTTP 400.
 */
@JsonPropertyOrder({"timestamp", "status", "error", "message", "path"})
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ErrorResponse badRequest(String message, String path) {
        return new ErrorResponse(Instant.now(), 400, "Bad Request", message, path);
    }
}
