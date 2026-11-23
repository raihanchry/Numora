package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.engine.AngleMode;
import com.example.engine.EvaluationResult;
import com.example.engine.ExpressionEvaluator;

import org.junit.Test;

/**
 * Pure Java unit tests verifying basic arithmetic, scientific functions,
 * operator precedence, DEG/RAD angle modes, and error handling.
 */
public class ExpressionEvaluatorTest {

    @Test
    public void basicArithmeticAndPrecedence() {
        EvaluationResult res = ExpressionEvaluator.evaluate("12+3×4−6÷2", AngleMode.DEGREES);
        assertTrue(res.isSuccess());
        assertEquals("21", res.getFormattedDisplay());
    }

    @Test
    public void floatingPointNoiseIsNormalized() {
        EvaluationResult res = ExpressionEvaluator.evaluate("0.1+0.2", AngleMode.DEGREES);
        assertTrue(res.isSuccess());
        assertEquals("0.3", res.getFormattedDisplay());
    }

    @Test
    public void trigFunctionsInDegrees() {
        assertEquals("0.5", ExpressionEvaluator.evaluate("sin(30)", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("0", ExpressionEvaluator.evaluate("cos(90)", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("1", ExpressionEvaluator.evaluate("tan(45)", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("30", ExpressionEvaluator.evaluate("sin⁻¹(0.5)", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("60", ExpressionEvaluator.evaluate("cos⁻¹(0.5)", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("45", ExpressionEvaluator.evaluate("tan⁻¹(1)", AngleMode.DEGREES).getFormattedDisplay());
    }

    @Test
    public void trigFunctionsInRadians() {
        EvaluationResult res = ExpressionEvaluator.evaluate("sin(π÷2)+cos(0)", AngleMode.RADIANS);
        assertTrue(res.isSuccess());
        assertEquals("2", res.getFormattedDisplay());
    }

    @Test
    public void logarithmsBase10AndNatural() {
        assertEquals("3", ExpressionEvaluator.evaluate("log(1000)", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("1", ExpressionEvaluator.evaluate("ln(e)", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("2", ExpressionEvaluator.evaluate("ln(e^2)", AngleMode.DEGREES).getFormattedDisplay());
    }

    @Test
    public void exponentiationAndSquareRoot() {
        // Right-associative power: 2^(3^2) = 512
        assertEquals("512", ExpressionEvaluator.evaluate("2^3^2", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("0.125", ExpressionEvaluator.evaluate("2^−3", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("12", ExpressionEvaluator.evaluate("√(144)", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("24", ExpressionEvaluator.evaluate("2√(144)", AngleMode.DEGREES).getFormattedDisplay());
    }

    @Test
    public void implicitMultiplicationAndAutoCloseParens() {
        assertEquals("14", ExpressionEvaluator.evaluate("2(3+4", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("120", ExpressionEvaluator.evaluate("5!", AngleMode.DEGREES).getFormattedDisplay());
        assertEquals("20", ExpressionEvaluator.evaluate("200×10%", AngleMode.DEGREES).getFormattedDisplay());
    }

    @Test
    public void errorConditionsReturnCleanErrorTypes() {
        EvaluationResult divZero = ExpressionEvaluator.evaluate("10÷0", AngleMode.DEGREES);
        assertFalse(divZero.isSuccess());
        assertEquals(EvaluationResult.ErrorType.DIVIDE_BY_ZERO, divZero.getErrorType());

        EvaluationResult negSqrt = ExpressionEvaluator.evaluate("√(−9)", AngleMode.DEGREES);
        assertFalse(negSqrt.isSuccess());
        assertEquals(EvaluationResult.ErrorType.DOMAIN, negSqrt.getErrorType());

        EvaluationResult negLog = ExpressionEvaluator.evaluate("log(0)", AngleMode.DEGREES);
        assertFalse(negLog.isSuccess());
        assertEquals(EvaluationResult.ErrorType.DOMAIN, negLog.getErrorType());

        EvaluationResult tan90 = ExpressionEvaluator.evaluate("tan(90)", AngleMode.DEGREES);
        assertFalse(tan90.isSuccess());
        assertEquals(EvaluationResult.ErrorType.DOMAIN, tan90.getErrorType());
    }
}
