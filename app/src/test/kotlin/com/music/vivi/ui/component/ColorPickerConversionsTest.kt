package com.music.vivi.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorPickerConversionsTest {

    private fun assertColorsEqual(expected: Color, actual: Color, tolerance: Float = 1f / 255f) {
        assertEquals("red", expected.red, actual.red, tolerance)
        assertEquals("green", expected.green, actual.green, tolerance)
        assertEquals("blue", expected.blue, actual.blue, tolerance)
    }

    @Test
    fun `hsvToColor produces primary colors at full saturation and value`() {
        assertColorsEqual(Color.Red, ColorPickerConversions.hsvToColor(0f, 1f, 1f))
        assertColorsEqual(Color.Green, ColorPickerConversions.hsvToColor(120f, 1f, 1f))
        assertColorsEqual(Color.Blue, ColorPickerConversions.hsvToColor(240f, 1f, 1f))
    }

    @Test
    fun `hsvToColor produces white at zero saturation and black at zero value`() {
        assertColorsEqual(Color.White, ColorPickerConversions.hsvToColor(180f, 0f, 1f))
        assertColorsEqual(Color.Black, ColorPickerConversions.hsvToColor(180f, 1f, 0f))
    }

    @Test
    fun `hsvToColor normalizes hue outside 0 to 360`() {
        assertColorsEqual(
            ColorPickerConversions.hsvToColor(30f, 1f, 1f),
            ColorPickerConversions.hsvToColor(390f, 1f, 1f),
        )
        assertColorsEqual(
            ColorPickerConversions.hsvToColor(330f, 1f, 1f),
            ColorPickerConversions.hsvToColor(-30f, 1f, 1f),
        )
    }

    @Test
    fun `colorToHsv extracts components of primary colors`() {
        val (redHue, redSat, redValue) = ColorPickerConversions.colorToHsv(Color.Red)
        assertEquals(0f, redHue, 0.5f)
        assertEquals(1f, redSat, 0.01f)
        assertEquals(1f, redValue, 0.01f)

        val (greenHue, _, _) = ColorPickerConversions.colorToHsv(Color.Green)
        assertEquals(120f, greenHue, 0.5f)
    }

    @Test
    fun `hsv roundtrip preserves colors`() {
        val samples = listOf(
            Color(0xFF123456),
            Color(0xFFAABBCC),
            Color(0xFFFF8800),
            Color(0xFF3F51B5),
            Color.White,
            Color.Black,
        )
        samples.forEach { color ->
            val (h, s, v) = ColorPickerConversions.colorToHsv(color)
            assertColorsEqual(color, ColorPickerConversions.hsvToColor(h, s, v))
        }
    }

    @Test
    fun `colorToHex formats without alpha`() {
        assertEquals("FFFFFF", ColorPickerConversions.colorToHex(Color.White))
        assertEquals("000000", ColorPickerConversions.colorToHex(Color.Black))
        assertEquals("123456", ColorPickerConversions.colorToHex(Color(0xFF123456)))
    }

    @Test
    fun `parseHexColor accepts hash prefix and mixed case`() {
        assertEquals(Color(0xFF123456).toArgb(), ColorPickerConversions.parseHexColor("#123456")!!.toArgb())
        assertEquals(Color(0xFFAABBCC).toArgb(), ColorPickerConversions.parseHexColor("aAbBcC")!!.toArgb())
    }

    @Test
    fun `parseHexColor returns opaque colors`() {
        assertEquals(1f, ColorPickerConversions.parseHexColor("123456")!!.alpha, 0.001f)
    }

    @Test
    fun `parseHexColor rejects invalid input`() {
        assertNull(ColorPickerConversions.parseHexColor(""))
        assertNull(ColorPickerConversions.parseHexColor("12345"))
        assertNull(ColorPickerConversions.parseHexColor("1234567"))
        assertNull(ColorPickerConversions.parseHexColor("GGGGGG"))
    }

    @Test
    fun `hex roundtrip preserves colors`() {
        val samples = listOf("123456", "AABBCC", "FF8800", "3F51B5")
        samples.forEach { hex ->
            val color = ColorPickerConversions.parseHexColor(hex)!!
            assertEquals(hex, ColorPickerConversions.colorToHex(color))
        }
    }
}
