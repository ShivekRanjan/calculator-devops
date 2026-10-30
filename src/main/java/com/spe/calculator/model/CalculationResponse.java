package com.spe.calculator.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/**
 * Successful response, e.g. {@code { "operation": "add", "a": 2, "b": 3, "result": 5 }}.
 */
@JsonPropertyOrder({"operation", "a", "b", "result"})
public record CalculationResponse(String operation, BigDecimal a, BigDecimal b, BigDecimal result) {

    public static CalculationResponse of(String operation, double a, double b, BigDecimal result) {
        return new CalculationResponse(operation, toDecimal(a), toDecimal(b), result);
    }

    /** 2.0 is echoed back as 2, 2.50 as 2.5. */
    private static BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros();
    }
}
