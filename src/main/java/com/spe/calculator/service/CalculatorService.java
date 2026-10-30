package com.spe.calculator.service;

import com.spe.calculator.exception.InvalidCalculationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Business logic for the four arithmetic operations.
 *
 * <p>Inputs arrive as {@code double} (Spring rejects anything that is not a number),
 * but the maths is done with {@link BigDecimal} so that decimal results are exact,
 * e.g. {@code 0.1 + 0.2 = 0.3} instead of {@code 0.30000000000000004}.
 */
@Service
public class CalculatorService {

    /** 34 significant digits (IEEE 754 decimal128), rounding HALF_EVEN. */
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    public BigDecimal add(double a, double b) {
        return toDecimal("a", a).add(toDecimal("b", b), PRECISION).stripTrailingZeros();
    }

    public BigDecimal subtract(double a, double b) {
        return toDecimal("a", a).subtract(toDecimal("b", b), PRECISION).stripTrailingZeros();
    }

    public BigDecimal multiply(double a, double b) {
        return toDecimal("a", a).multiply(toDecimal("b", b), PRECISION).stripTrailingZeros();
    }

    public BigDecimal divide(double a, double b) {
        BigDecimal dividend = toDecimal("a", a);
        BigDecimal divisor = toDecimal("b", b);
        if (divisor.signum() == 0) {
            throw new InvalidCalculationException("Division by zero is not allowed");
        }
        return dividend.divide(divisor, PRECISION).stripTrailingZeros();
    }

    public BigDecimal modulo(double a, double b) {
        BigDecimal dividend = toDecimal("a", a);
        BigDecimal divisor = toDecimal("b", b);
        if (divisor.signum() == 0) {
            throw new InvalidCalculationException("Modulo by zero is not allowed");
        }
        return dividend.remainder(divisor, PRECISION).stripTrailingZeros();
    }

    /**
     * Rejects NaN and Infinity (Java happily parses "NaN" and "Infinity" as doubles)
     * and converts the value to an exact decimal.
     */
    private static BigDecimal toDecimal(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new InvalidCalculationException("Parameter '" + name + "' must be a finite number");
        }
        return BigDecimal.valueOf(value);
    }
}
