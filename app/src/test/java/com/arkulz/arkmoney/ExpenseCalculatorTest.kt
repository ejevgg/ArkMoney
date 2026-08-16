package com.arkulz.arkmoney

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseCalculatorTest {
    @Test fun `calculates an expression and converts it to cents`() {
        val state = CalculatorState()
            .pressDigit('1')
            .pressDigit('2')
            .pressOperation('+')
            .pressDigit('3')
            .pressDecimal()
            .pressDigit('5')
            .pressEquals()

        assertEquals("15.5", state.display)
        assertEquals("12 + 3.5", state.expression)
        assertEquals(1550L, state.amountCents())
    }

    @Test fun `zero cannot be saved as an expense`() {
        assertNull(CalculatorState().amountCents())
    }

    @Test fun `division by zero keeps the left operand`() {
        val state = CalculatorState().pressDigit('8').pressOperation('÷').pressDigit('0').pressEquals()
        assertNull(state.amountCents())
    }

    @Test fun `multiplication has precedence and expression remains visible`() {
        val state = CalculatorState().pressDigit('2').pressOperation('+').pressDigit('3')
            .pressOperation('×').pressDigit('4').pressEquals()
        assertEquals("14", state.display)
        assertEquals("2 + 3 × 4", state.expression)
    }

    @Test fun `shows a live preview while expression is being entered`() {
        val state = CalculatorState().pressDigit('1').pressDigit('0').pressOperation('+').pressDigit('5')
        assertEquals("10 + 5", state.expression)
        assertEquals("15", state.display)
        assertEquals(1500L, state.amountCents())
    }

    @Test fun `replacing an operator does not duplicate operands`() {
        val state = CalculatorState().pressDigit('8').pressOperation('+').pressOperation('×').pressDigit('2')
        assertEquals("8 × 2", state.expression)
        assertEquals("16", state.display)
    }

    @Test fun `continues calculation from equals result`() {
        val state = CalculatorState().pressDigit('2').pressOperation('+').pressDigit('3').pressEquals().pressOperation('×').pressDigit('4')
        assertEquals("5 × 4", state.expression)
        assertEquals("20", state.display)
    }

    @Test fun `decimal is inserted only once`() {
        val state = CalculatorState().pressDigit('1').pressDecimal().pressDecimal().pressDigit('5')
        assertEquals("1.5", state.expression)
        assertEquals(150L, state.amountCents())
    }
}
