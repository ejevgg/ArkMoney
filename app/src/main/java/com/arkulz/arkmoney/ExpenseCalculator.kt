package com.arkulz.arkmoney

import java.math.BigDecimal
import java.math.RoundingMode

internal data class CalculatorState(
    val tokens: List<String> = emptyList(),
    val current: String = "0",
    val result: BigDecimal? = null,
) {
    val expression: String
        get() = (tokens + current.takeIf { result == null }).filterNotNull().joinToString(" ")

    val display: String
        get() = (result ?: previewValue() ?: current.toBigDecimalOrNull() ?: BigDecimal.ZERO).displayValue()

    private fun previewValue(): BigDecimal? =
        if (tokens.isEmpty() || tokens.lastOrNull() !in setOf("+", "-", "×", "÷")) null
        else evaluate(tokens + current)
}

internal fun CalculatorState.pressDigit(digit: Char): CalculatorState {
    require(digit.isDigit())
    val base = if (result != null) "0" else current
    val next = if (base == "0") digit.toString() else base + digit
    return copy(tokens = if (result != null) emptyList() else tokens, current = next, result = null)
}

internal fun CalculatorState.pressDecimal(): CalculatorState {
    if (result != null) return CalculatorState(current = "0.")
    return if ('.' in current) this else copy(current = "$current.")
}

internal fun CalculatorState.pressOperation(operation: Char): CalculatorState {
    require(operation in "+-×÷")
    val value = result?.displayValue() ?: current
    if (result != null) return CalculatorState(tokens = listOf(value, operation.toString()), current = "0")
    return if (current == "0" && tokens.lastOrNull() in setOf("+", "-", "×", "÷")) {
        copy(tokens = tokens.dropLast(1) + operation.toString(), result = null)
    } else {
        copy(tokens = tokens + value + operation.toString(), current = "0", result = null)
    }
}

internal fun CalculatorState.pressEquals(): CalculatorState {
    if (tokens.isEmpty()) return this
    val complete = tokens + current
    return copy(tokens = complete, result = evaluate(complete))
}

internal fun CalculatorState.pressBackspace(): CalculatorState {
    if (result != null) return CalculatorState()
    return copy(current = current.dropLast(1).ifEmpty { "0" })
}

internal fun CalculatorState.clearEntry(): CalculatorState =
    if (result != null || tokens.isEmpty()) CalculatorState() else copy(current = "0", result = null)

internal fun CalculatorState.amountCents(): Long? {
    val value = result ?: if (tokens.isNotEmpty()) evaluate(tokens + current) else current.toBigDecimalOrNull()
    return value
        ?.takeIf { it > BigDecimal.ZERO }
        ?.setScale(2, RoundingMode.HALF_UP)
        ?.movePointRight(2)
        ?.longValueExact()
}

private fun evaluate(tokens: List<String>): BigDecimal? {
    val values = ArrayDeque<BigDecimal>()
    val operations = ArrayDeque<Char>()

    fun applyTop(): Boolean {
        val operation = operations.removeLastOrNull() ?: return false
        val right = values.removeLastOrNull() ?: return false
        val left = values.removeLastOrNull() ?: return false
        val value = when (operation) {
            '+' -> left + right
            '-' -> left - right
            '×' -> left * right
            '÷' -> if (right.compareTo(BigDecimal.ZERO) == 0) return false
            else left.divide(right, 8, RoundingMode.HALF_UP)
            else -> return false
        }
        values.addLast(value)
        return true
    }

    tokens.forEach { token ->
        val number = token.toBigDecimalOrNull()
        if (number != null) {
            values.addLast(number)
        } else {
            val operation = token.singleOrNull() ?: return null
            while (operations.isNotEmpty() && precedence(operations.last()) >= precedence(operation)) {
                if (!applyTop()) return null
            }
            operations.addLast(operation)
        }
    }
    while (operations.isNotEmpty()) if (!applyTop()) return null
    return values.singleOrNull()
}

private fun precedence(operation: Char): Int = if (operation == '×' || operation == '÷') 2 else 1

private fun BigDecimal.displayValue(): String =
    stripTrailingZeros().toPlainString().let { if (it == "-0") "0" else it }
