package com.vinaynalavade.expensetracker

import com.vinaynalavade.expensetracker.core.model.Amount
import com.vinaynalavade.expensetracker.domain.usecase.CalculatorEvaluationResult
import com.vinaynalavade.expensetracker.domain.usecase.EvaluateCalculatorExpressionUseCase
import com.vinaynalavade.expensetracker.domain.validation.TransactionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CalculatorExpressionEvaluatorTest {

    private lateinit var evaluator: EvaluateCalculatorExpressionUseCase

    @Before
    fun setUp() {
        evaluator = EvaluateCalculatorExpressionUseCase()
    }

    // 1. Basic Operations
    @Test
    fun testBasicAddition() {
        val result = evaluator("100 + 200")
        assertTrue(result is CalculatorEvaluationResult.Success)
        val success = result as CalculatorEvaluationResult.Success
        assertEquals("300", success.formattedAmount)
        assertEquals(0, BigDecimal("300").compareTo(success.rawValue))
        assertTrue(success.isPositive)
    }

    @Test
    fun testBasicSubtraction() {
        val result = evaluator("500 - 200")
        assertTrue(result is CalculatorEvaluationResult.Success)
        val success = result as CalculatorEvaluationResult.Success
        assertEquals("300", success.formattedAmount)
        assertEquals(0, BigDecimal("300").compareTo(success.rawValue))
        assertTrue(success.isPositive)
    }

    @Test
    fun testBasicMultiplication() {
        val resultCross = evaluator("25 × 4")
        assertTrue(resultCross is CalculatorEvaluationResult.Success)
        assertEquals("100", (resultCross as CalculatorEvaluationResult.Success).formattedAmount)

        val resultAsterisk = evaluator("25 * 4")
        assertTrue(resultAsterisk is CalculatorEvaluationResult.Success)
        assertEquals("100", (resultAsterisk as CalculatorEvaluationResult.Success).formattedAmount)
    }

    @Test
    fun testBasicDivision() {
        val resultDivide = evaluator("100 ÷ 4")
        assertTrue(resultDivide is CalculatorEvaluationResult.Success)
        assertEquals("25", (resultDivide as CalculatorEvaluationResult.Success).formattedAmount)

        val resultSlash = evaluator("100 / 4")
        assertTrue(resultSlash is CalculatorEvaluationResult.Success)
        assertEquals("25", (resultSlash as CalculatorEvaluationResult.Success).formattedAmount)
    }

    // 2. Chained Expressions
    @Test
    fun testChainedAddition() {
        val result = evaluator("100 + 200 + 300")
        assertTrue(result is CalculatorEvaluationResult.Success)
        assertEquals("600", (result as CalculatorEvaluationResult.Success).formattedAmount)
    }

    @Test
    fun testChainedSubtraction() {
        val result = evaluator("1000 - 100 - 200")
        assertTrue(result is CalculatorEvaluationResult.Success)
        assertEquals("700", (result as CalculatorEvaluationResult.Success).formattedAmount)
    }

    // 3. Operator Precedence (BODMAS/PEMDAS)
    @Test
    fun testOperatorPrecedenceMultiplicationOverAddition() {
        // 100 + (50 * 2) = 200 (not 150 * 2 = 300)
        val result = evaluator("100 + 50 × 2")
        assertTrue(result is CalculatorEvaluationResult.Success)
        assertEquals("200", (result as CalculatorEvaluationResult.Success).formattedAmount)
    }

    @Test
    fun testOperatorPrecedenceDivisionOverAddition() {
        // (1000 / 5) + 10 = 210
        val result = evaluator("1000 ÷ 5 + 10")
        assertTrue(result is CalculatorEvaluationResult.Success)
        assertEquals("210", (result as CalculatorEvaluationResult.Success).formattedAmount)
    }

    @Test
    fun testComplexOperatorPrecedence() {
        // 250 + 100 * 2 = 450
        val result1 = evaluator("250 + 100 × 2")
        assertTrue(result1 is CalculatorEvaluationResult.Success)
        assertEquals("450", (result1 as CalculatorEvaluationResult.Success).formattedAmount)

        // 10 + 20 * 3 - 4 / 2 = 10 + 60 - 2 = 68
        val result2 = evaluator("10 + 20 × 3 - 4 ÷ 2")
        assertTrue(result2 is CalculatorEvaluationResult.Success)
        assertEquals("68", (result2 as CalculatorEvaluationResult.Success).formattedAmount)
    }

    // 4. Decimal Precision (Precision-Safe without Float/Double artifacts)
    @Test
    fun testExactDecimalPrecision() {
        // 0.1 + 0.2 must equal 0.3 without 0.30000000000000004
        val result = evaluator("0.1 + 0.2")
        assertTrue(result is CalculatorEvaluationResult.Success)
        val success = result as CalculatorEvaluationResult.Success
        assertEquals("0.3", success.formattedAmount)
        assertEquals(0, BigDecimal("0.3").compareTo(success.rawValue))
    }

    @Test
    fun testDecimalAdditionWithCents() {
        // 100.50 + 99.50 = 200
        val result = evaluator("100.50 + 99.50")
        assertTrue(result is CalculatorEvaluationResult.Success)
        assertEquals("200", (result as CalculatorEvaluationResult.Success).formattedAmount)
    }

    @Test
    fun testDecimalDivisionWithRounding() {
        // 10 / 3 = 3.33 (rounded to 2 decimal places)
        val result = evaluator("10 ÷ 3", maxDecimalDigits = 2)
        assertTrue(result is CalculatorEvaluationResult.Success)
        assertEquals("3.33", (result as CalculatorEvaluationResult.Success).formattedAmount)

        // 2 / 3 = 0.67
        val result2 = evaluator("2 ÷ 3", maxDecimalDigits = 2)
        assertTrue(result2 is CalculatorEvaluationResult.Success)
        assertEquals("0.67", (result2 as CalculatorEvaluationResult.Success).formattedAmount)
    }

    // 5. Invalid States & Error Handling
    @Test
    fun testDivisionByZeroHandledGracefully() {
        val result = evaluator("100 ÷ 0")
        assertTrue(result is CalculatorEvaluationResult.Error)
        assertEquals("Cannot divide by zero", (result as CalculatorEvaluationResult.Error).message)
    }

    @Test
    fun testEmptyExpression() {
        val result = evaluator("")
        assertTrue(result is CalculatorEvaluationResult.Empty)

        val resultWhitespace = evaluator("   ")
        assertTrue(resultWhitespace is CalculatorEvaluationResult.Empty)
    }

    @Test
    fun testTrailingOperatorForLivePreview() {
        // "100 +" should evaluate the sub-expression "100" for live preview
        val result = evaluator("100 +")
        assertTrue(result is CalculatorEvaluationResult.Success)
        assertEquals("100", (result as CalculatorEvaluationResult.Success).formattedAmount)
    }

    @Test
    fun testMultipleDecimalPointsInOneNumber() {
        val result = evaluator("1.2.3 + 4")
        assertTrue(result is CalculatorEvaluationResult.Error)
    }

    // 6. Negative Values & Amount Validation
    @Test
    fun testIntermediateNegativeCalculation() {
        // 100 - 300 = -200
        val result = evaluator("100 - 300")
        assertTrue(result is CalculatorEvaluationResult.Success)
        val success = result as CalculatorEvaluationResult.Success
        assertEquals("-200", success.formattedAmount)
        assertFalse("Negative result must have isPositive = false", success.isPositive)

        // Negative amount should fail TransactionValidator
        val (isValid, error) = TransactionValidator.validateAmount(success.formattedAmount)
        assertFalse(isValid)
        assertNotNull(error)
    }

    @Test
    fun testNegativeStartingNumber() {
        // -50 + 100 = 50
        val result = evaluator("-50 + 100")
        assertTrue(result is CalculatorEvaluationResult.Success)
        val success = result as CalculatorEvaluationResult.Success
        assertEquals("50", success.formattedAmount)
        assertTrue(success.isPositive)
    }

    // 7. Transaction Integration Compatibility
    @Test
    fun testValidResultMapsToValidAmount() {
        val result = evaluator("250 + 180 + 420")
        assertTrue(result is CalculatorEvaluationResult.Success)
        val success = result as CalculatorEvaluationResult.Success
        assertEquals("850", success.formattedAmount)

        val (isValid, error) = TransactionValidator.validateAmount(success.formattedAmount)
        assertTrue(isValid)
        assertEquals(null, error)

        val amount = Amount.fromStringOrNull(success.formattedAmount)
        assertNotNull(amount)
        assertEquals(85000L, amount?.subunits)
    }
}
