package com.spe.calculator.exception;

/**
 * Thrown when a calculation cannot be performed with the given inputs
 * (division by zero, NaN / Infinity operands). Mapped to HTTP 400.
 */
public class InvalidCalculationException extends RuntimeException {

    public InvalidCalculationException(String message) {
        super(message);
    }
}
