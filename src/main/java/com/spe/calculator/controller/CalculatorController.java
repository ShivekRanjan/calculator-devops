package com.spe.calculator.controller;

import com.spe.calculator.model.CalculationResponse;
import com.spe.calculator.service.CalculatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints under /api/v1. Parameters are boxed {@code Double} so that an
 * empty value (?a=&b=1) is reported as "must not be empty" instead of a conversion error.
 */
@RestController
@RequestMapping("/api/v1")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @GetMapping("/add")
    public CalculationResponse add(@RequestParam("a") Double a, @RequestParam("b") Double b) {
        return CalculationResponse.of("add", a, b, calculatorService.add(a, b));
    }

    @GetMapping("/subtract")
    public CalculationResponse subtract(@RequestParam("a") Double a, @RequestParam("b") Double b) {
        return CalculationResponse.of("subtract", a, b, calculatorService.subtract(a, b));
    }

    @GetMapping("/multiply")
    public CalculationResponse multiply(@RequestParam("a") Double a, @RequestParam("b") Double b) {
        return CalculationResponse.of("multiply", a, b, calculatorService.multiply(a, b));
    }

    @GetMapping("/divide")
    public CalculationResponse divide(@RequestParam("a") Double a, @RequestParam("b") Double b) {
        return CalculationResponse.of("divide", a, b, calculatorService.divide(a, b));
    }
}
