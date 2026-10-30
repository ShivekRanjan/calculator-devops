package com.spe.calculator.service;

import com.spe.calculator.exception.InvalidCalculationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain JUnit 5 unit tests for {@link CalculatorService} (no Spring context needed).
 */
class CalculatorServiceTest {

    private final CalculatorService service = new CalculatorService();

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
            "2,    3,    5",
            "-4,   1.5,  -2.5",
            "0.1,  0.2,  0.3",
            "0,    0,    0",
            "1e15, 1,    1000000000000001"
    })
    void add(double a, double b, String expected) {
        assertDecimalEquals(expected, service.add(a, b));
    }

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource({
            "10,   4,    6",
            "4,    10,   -6",
            "1.5,  1.5,  0",
            "0.3,  0.1,  0.2"
    })
    void subtract(double a, double b, String expected) {
        assertDecimalEquals(expected, service.subtract(a, b));
    }

    @ParameterizedTest(name = "{0} * {1} = {2}")
    @CsvSource({
            "6,    7,    42",
            "-3,   4,    -12",
            "2.5,  0.4,  1",
            "123,  0,    0",
            "-2,   -2.5, 5"
    })
    void multiply(double a, double b, String expected) {
        assertDecimalEquals(expected, service.multiply(a, b));
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource({
            "10,   2,    5",
            "7,    2,    3.5",
            "-9,   3,    -3",
            "0,    5,    0",
            "1,    3,    0.3333333333333333333333333333333333",
            "2,    3,    0.6666666666666666666666666666666667"
    })
    void divide(double a, double b, String expected) {
        assertDecimalEquals(expected, service.divide(a, b));
    }

    @Test
    @DisplayName("divide by zero throws InvalidCalculationException")
    void divideByZeroThrows() {
        InvalidCalculationException ex =
                assertThrows(InvalidCalculationException.class, () -> service.divide(5, 0));
        assertEquals("Division by zero is not allowed", ex.getMessage());
    }

    @Test
    @DisplayName("divide by negative zero is also division by zero")
    void divideByNegativeZeroThrows() {
        assertThrows(InvalidCalculationException.class, () -> service.divide(5, -0.0));
    }

    @Test
    @DisplayName("modulo returns the remainder and rejects zero")
    void modulo() {
        assertDecimalEquals("1", service.modulo(10, 3));
        assertDecimalEquals("0.5", service.modulo(5.5, 1));
        assertThrows(InvalidCalculationException.class, () -> service.modulo(10, 0));
    }

    @ParameterizedTest(name = "rejects {0} as operand a")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteFirstOperand(double value) {
        InvalidCalculationException ex =
                assertThrows(InvalidCalculationException.class, () -> service.add(value, 1));
        assertEquals("Parameter 'a' must be a finite number", ex.getMessage());
    }

    @ParameterizedTest(name = "rejects {0} as operand b")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteSecondOperand(double value) {
        InvalidCalculationException ex =
                assertThrows(InvalidCalculationException.class, () -> service.multiply(1, value));
        assertEquals("Parameter 'b' must be a finite number", ex.getMessage());
    }

    /** Compares by numeric value, so 5, 5.0 and 5E+0 are all equal. */
    private static void assertDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual.toPlainString());
    }
}
