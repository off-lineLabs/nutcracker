package com.offlinelabs.nutcracker.ui.components.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DecimalInputTest {

    private val en = Locale.US
    private val ptBR = Locale.forLanguageTag("pt-BR")
    private val esES = Locale.forLanguageTag("es-ES")

    private fun assertParses(expected: Double, text: String) {
        val actual = parseDecimal(text)
        assertEquals("parseDecimal(\"$text\")", expected, actual!!, 1e-9)
    }

    // ----- parseDecimal -----

    @Test
    fun parse_integers() {
        assertParses(0.0, "0")
        assertParses(12.0, "12")
        assertParses(250.0, "0250")
    }

    @Test
    fun parse_acceptsDotAndCommaAsDecimalSeparator() {
        assertParses(12.5, "12.5")
        assertParses(12.5, "12,5")
        assertParses(0.25, "0,25")
        assertParses(120.4, "120,4")
    }

    @Test
    fun parse_trimsWhitespace() {
        assertParses(12.5, "  12,5 ")
        assertParses(3.0, "\t3\n")
    }

    @Test
    fun parse_leadingOrTrailingSeparator() {
        assertParses(5.0, "5,")
        assertParses(5.0, "5.")
        assertParses(0.5, ",5")
        assertParses(0.5, ".5")
    }

    @Test
    fun parse_signs() {
        assertParses(-3.0, "-3")
        assertParses(-0.5, "-0,5")
        assertParses(3.0, "+3")
    }

    @Test
    fun parse_rejectsBlank() {
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("   "))
    }

    @Test
    fun parse_rejectsGarbage() {
        listOf(
            "abc", "12a", "a12", "1,2,3", "1.2.3", "1,234.5", "1 000", ",", ".", "-", "+", "-,",
            "--3", "3-", "1e3", "NaN", "Infinity", "-Infinity", "0x10", "12kg", "١٢"
        ).forEach { assertNull("expected null for \"$it\"", parseDecimal(it)) }
    }

    // ----- formatDecimal -----

    @Test
    fun format_dropsTrailingZeros() {
        assertEquals("12", formatDecimal(12.0, en))
        assertEquals("0", formatDecimal(0.0, en))
        assertEquals("12.5", formatDecimal(12.50, en))
        assertEquals("100", formatDecimal(100.0, ptBR))
    }

    @Test
    fun format_usesLocaleDecimalSeparator() {
        assertEquals("12.5", formatDecimal(12.5, en))
        assertEquals("12,5", formatDecimal(12.5, ptBR))
        assertEquals("12,5", formatDecimal(12.5, esES))
    }

    @Test
    fun format_neverUsesGroupingSeparators() {
        assertEquals("1234567.5", formatDecimal(1234567.5, en))
        assertEquals("1234567,5", formatDecimal(1234567.5, ptBR))
        assertEquals("12000", formatDecimal(12000.0, esES))
    }

    @Test
    fun format_roundsHalfUpToMaxFractionDigits() {
        assertEquals("0.13", formatDecimal(0.125, en))
        assertEquals("1.01", formatDecimal(1.005, en))
        assertEquals("33.33", formatDecimal(100.0 / 3, en))
        assertEquals("66,67", formatDecimal(200.0 / 3, ptBR))
        assertEquals("12.3", formatDecimal(12.345, en, maxFractionDigits = 1))
        assertEquals("12", formatDecimal(12.4, en, maxFractionDigits = 0))
    }

    @Test
    fun format_negativesAndNegativeZero() {
        assertEquals("-3.5", formatDecimal(-3.5, en))
        assertEquals("0", formatDecimal(-0.0, en))
        assertEquals("0", formatDecimal(-0.001, en))
    }

    @Test
    fun format_nonFiniteIsEmpty() {
        assertEquals("", formatDecimal(Double.NaN, en))
        assertEquals("", formatDecimal(Double.POSITIVE_INFINITY, en))
    }

    @Test
    fun format_thenParse_roundTrips() {
        for (locale in listOf(en, ptBR, esES)) {
            for (value in listOf(0.0, 1.0, 12.5, 0.05, 999.99, 1234567.25)) {
                assertParses(value, formatDecimal(value, locale))
            }
        }
    }

    @Test
    fun decimalSeparatorFor_locales() {
        assertEquals('.', decimalSeparatorFor(en))
        assertEquals(',', decimalSeparatorFor(ptBR))
        assertEquals(',', decimalSeparatorFor(esES))
    }

    // ----- isPartialDecimalInput -----

    @Test
    fun partialInput_allowsDigitsAndOneSeparator() {
        listOf("", "1", "12", "12,", "12.", ",5", "12,5", "0.25").forEach {
            assertTrue("expected \"$it\" to be allowed", isPartialDecimalInput(it))
        }
    }

    @Test
    fun partialInput_rejectsOtherCharacters() {
        listOf("-", "-1", "1,2,", "1.2.3", "a", "1 ", "1e3").forEach {
            assertFalse("expected \"$it\" to be rejected", isPartialDecimalInput(it))
        }
    }

    // ----- validateDecimal -----

    @Test
    fun validate_rules() {
        assertEquals(FieldError.Required, validateDecimal("", required = true))
        assertEquals(FieldError.Required, validateDecimal("  ", required = true))
        assertNull(validateDecimal("", required = false))
        assertEquals(FieldError.InvalidNumber, validateDecimal("abc", required = false))
        assertEquals(FieldError.InvalidNumber, validateDecimal("1,2,3", required = true))
        assertEquals(FieldError.Negative, validateDecimal("-3", required = false))
        assertNull(validateDecimal("0", required = true))
        assertEquals(FieldError.MustBePositive, validateDecimal("0", required = true, allowZero = false))
        assertEquals(FieldError.MustBePositive, validateDecimal("-1", required = true, allowZero = false))
        assertNull(validateDecimal("0,5", required = true, allowZero = false))
    }
}
