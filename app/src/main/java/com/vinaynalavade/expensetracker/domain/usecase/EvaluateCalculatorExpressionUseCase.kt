package com.vinaynalavade.expensetracker.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.ArrayDeque

sealed interface CalculatorEvaluationResult {
    data class Success(
        val rawValue: BigDecimal,
        val formattedAmount: String,
        val displayString: String,
        val isPositive: Boolean
    ) : CalculatorEvaluationResult

    data class Error(val message: String) : CalculatorEvaluationResult
    data object Empty : CalculatorEvaluationResult
    data object Incomplete : CalculatorEvaluationResult
}

/**
 * Precision-safe, offline arithmetic expression evaluator for the in-app calculator.
 *
 * Implements Dijkstra's Shunting-Yard algorithm with standard mathematical operator precedence
 * (× and ÷ evaluated before + and -) using [BigDecimal] arithmetic.
 */
class EvaluateCalculatorExpressionUseCase {

    private sealed interface Token {
        data class Number(val value: BigDecimal) : Token
        data class Operator(val symbol: Char, val precedence: Int) : Token
    }

    /**
     * Evaluates a mathematical expression string (e.g. "100 + 50 × 2") safely.
     *
     * @param expression The mathematical expression to evaluate.
     * @param maxDecimalDigits The currency decimal scale to round to (default: 2).
     * @return [CalculatorEvaluationResult] containing the evaluated result or descriptive error.
     */
    operator fun invoke(expression: String, maxDecimalDigits: Int = 2): CalculatorEvaluationResult {
        val sanitized = sanitizeExpression(expression)
        if (sanitized.isBlank()) {
            return CalculatorEvaluationResult.Empty
        }

        // Check if expression ends with an operator (incomplete live expression)
        val lastChar = sanitized.last()
        if (isOperatorChar(lastChar)) {
            // Attempt to evaluate the expression without the trailing operator for live preview
            val trimmed = sanitized.dropLast(1).trim()
            return if (trimmed.isBlank()) {
                CalculatorEvaluationResult.Incomplete
            } else {
                when (val subResult = invoke(trimmed, maxDecimalDigits)) {
                    is CalculatorEvaluationResult.Success -> subResult
                    else -> CalculatorEvaluationResult.Incomplete
                }
            }
        }

        return try {
            val tokens = tokenize(sanitized)
            if (tokens.isEmpty()) {
                return CalculatorEvaluationResult.Empty
            }

            val postfix = infixToPostfix(tokens)
            val result = evaluatePostfix(postfix)

            val scaled = result.setScale(maxDecimalDigits, RoundingMode.HALF_UP)
            val stripped = scaled.stripTrailingZeros()
            val formatted = if (stripped.scale() <= 0) {
                stripped.setScale(0).toPlainString()
            } else {
                stripped.toPlainString()
            }

            val isPositive = result.compareTo(BigDecimal.ZERO) > 0

            CalculatorEvaluationResult.Success(
                rawValue = result,
                formattedAmount = formatted,
                displayString = formatted,
                isPositive = isPositive
            )
        } catch (e: ArithmeticException) {
            CalculatorEvaluationResult.Error(e.message ?: "Calculation error")
        } catch (e: IllegalArgumentException) {
            CalculatorEvaluationResult.Error(e.message ?: "Invalid expression")
        } catch (e: Exception) {
            CalculatorEvaluationResult.Error("Invalid expression")
        }
    }

    private fun sanitizeExpression(expression: String): String {
        return expression
            .replace("*", "×")
            .replace("/", "÷")
            .trim()
    }

    private fun isOperatorChar(c: Char): Boolean = c == '+' || c == '-' || c == '×' || c == '÷'

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = expr.length

        while (i < len) {
            val c = expr[i]

            if (c.isWhitespace()) {
                i++
                continue
            }

            // Check for negative number: '-' at start or '-' preceded by an operator
            val isUnaryMinus = c == '-' && (tokens.isEmpty() || tokens.last() is Token.Operator)

            if (c.isDigit() || c == '.' || isUnaryMinus) {
                val start = i
                if (isUnaryMinus) {
                    i++ // Skip '-'
                }
                var hasDecimal = false
                while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                    if (expr[i] == '.') {
                        if (hasDecimal) {
                            throw IllegalArgumentException("Multiple decimal points in number")
                        }
                        hasDecimal = true
                    }
                    i++
                }

                val numStr = expr.substring(start, i)
                if (numStr == "-" || numStr == "." || numStr == "-.") {
                    throw IllegalArgumentException("Invalid number: $numStr")
                }

                val number = BigDecimal(numStr)
                tokens.add(Token.Number(number))
            } else if (isOperatorChar(c)) {
                val precedence = when (c) {
                    '×', '÷' -> 2
                    '+', '-' -> 1
                    else -> 0
                }
                tokens.add(Token.Operator(c, precedence))
                i++
            } else {
                throw IllegalArgumentException("Unexpected character: $c")
            }
        }

        return tokens
    }

    private fun infixToPostfix(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val opStack = ArrayDeque<Token.Operator>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> output.add(token)
                is Token.Operator -> {
                    while (opStack.isNotEmpty() && (opStack.peek()?.precedence ?: 0) >= token.precedence) {
                        output.add(opStack.pop())
                    }
                    opStack.push(token)
                }
            }
        }

        while (opStack.isNotEmpty()) {
            output.add(opStack.pop())
        }

        return output
    }

    private fun evaluatePostfix(postfix: List<Token>): BigDecimal {
        val stack = ArrayDeque<BigDecimal>()

        for (token in postfix) {
            when (token) {
                is Token.Number -> stack.push(token.value)
                is Token.Operator -> {
                    if (stack.size < 2) {
                        throw IllegalArgumentException("Malformed expression")
                    }
                    val right = stack.pop()
                    val left = stack.pop()

                    val result = when (token.symbol) {
                        '+' -> left.add(right)
                        '-' -> left.subtract(right)
                        '×' -> left.multiply(right)
                        '÷' -> {
                            if (right.compareTo(BigDecimal.ZERO) == 0) {
                                throw ArithmeticException("Cannot divide by zero")
                            }
                            left.divide(right, 10, RoundingMode.HALF_UP)
                        }
                        else -> throw IllegalArgumentException("Unknown operator: ${token.symbol}")
                    }
                    stack.push(result)
                }
            }
        }

        if (stack.size != 1) {
            throw IllegalArgumentException("Malformed expression")
        }

        return stack.pop()
    }
}
