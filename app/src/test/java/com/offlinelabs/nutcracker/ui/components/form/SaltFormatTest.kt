package com.offlinelabs.nutcracker.ui.components.form

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SaltFormatTest {
    private val en = Locale.US
    private val es = Locale.forLanguageTag("es-ES")
    private val pt = Locale.forLanguageTag("pt-BR")

    @Test
    fun formatSaltMg_groupsPerLocale() {
        assertEquals("1,500", formatSaltMg(1500.0, en))
        assertEquals("1.500", formatSaltMg(1500.0, pt))
        assertEquals("5.750", formatSaltMg(5750.0, es))
        assertEquals("123", formatSaltMg(123.0, en))
    }

    @Test
    fun formatSaltMg_roundsHalfUpToWholeNumber() {
        assertEquals("1,500", formatSaltMg(1499.5, en))
        assertEquals("1,499", formatSaltMg(1499.4, en))
        assertEquals("0", formatSaltMg(0.0, en))
        assertEquals("1", formatSaltMg(0.5, en))
    }

    @Test
    fun saltMgToInputText_roundTrips() {
        assertEquals("1500", saltMgToInputText(1500.0, en))
        assertEquals("1500", saltMgToInputText(1500.0, pt))
        assertEquals("12.5", saltMgToInputText(12.5, en))
        assertEquals("12,5", saltMgToInputText(12.5, pt))
        assertEquals(12.5, parseDecimal("12,5")!!, 0.0)
        assertEquals(12.5, parseDecimal(saltMgToInputText(12.5, pt))!!, 0.0)
        assertEquals(1500.0, parseDecimal(saltMgToInputText(1500.0, en))!!, 0.0)
    }
}
