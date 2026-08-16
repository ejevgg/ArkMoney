package com.arkulz.arkmoney

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattingTest {
    @Test fun `parses comma and dot currency inputs`() {
        assertEquals(12345L, parseMoneyCents("123,45"))
        assertEquals(12345L, parseMoneyCents("123.45"))
    }

    @Test fun `rejects invalid currency input`() {
        assertNull(parseMoneyCents("рубль"))
    }
}
