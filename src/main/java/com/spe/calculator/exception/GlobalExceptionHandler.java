package com.spe.calculator.exception;

import com.spe.calculator.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns validation problems into a consistent HTTP 400 JSON body:
 * <pre>
 * { "timestamp": "...", "status": 400, "error": "Bad Request",
 *   "message": "Division by zero is not allowed", "path": "/api/v1/divide" }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Division by zero, NaN or Infinity. */
    @ExceptionHandler(InvalidCalculationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCalculation(InvalidCalculationException ex,
                                                                  HttpServletRequest request) {
        return badRequest(ex.getMessage(), request);
    }

    /** ?a=5 (b missing) or ?a=&b=5 (a empty). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest request) {
        String message = ex.isMissingAfterConversion()
                ? "Parameter '" + ex.getParameterName() + "' must not be empty"
                : "Missing required parameter '" + ex.getParameterName() + "'";
        return badRequest(message, request);
    }

    /** ?a=abc&b=5 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String message = "Parameter '" + ex.getName() + "' must be a number, but was '" + ex.getValue() + "'";
        return badRequest(message, request);
    }

    private static ResponseEntity<ErrorResponse> badRequest(String message, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.badRequest(message, request.getRequestURI()));
    }
}
