/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearQrCodeGeneratorTest {

    @Test
    fun testGenerateBitmapDimensions() {
        val content = "http://192.168.1.150:8888/pair?pin=1234"
        val sizePx = 256
        val bitmap = WearQrCodeGenerator.generateBitmap(content, sizePx, sizePx)

        assertNotNull("Generated Bitmap must not be null", bitmap)
        assertEquals("Width must match requested size", sizePx, bitmap.width)
        assertEquals("Height must match requested size", sizePx, bitmap.height)
    }

    @Test
    fun testGenerateBitmapContainsValidBlackAndWhitePixels() {
        val content = "http://10.0.0.45:8080/pair?pin=9876"
        val sizePx = 160
        val bitmap = WearQrCodeGenerator.generateBitmap(content, sizePx, sizePx)

        var hasBlack = false
        var hasWhite = false

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel == Color.BLACK) hasBlack = true
                if (pixel == Color.WHITE) hasWhite = true
            }
        }

        assertTrue("QR code bitmap must contain black pixels", hasBlack)
        assertTrue("QR code bitmap must contain white pixels", hasWhite)
    }

    @Test
    fun testGenerateInvertedAmoledBitmap() {
        val content = "vivimusic://pair?host=192.168.1.1&port=8888&pin=0001"
        val sizePx = 128
        val bitmap = WearQrCodeGenerator.generateBitmap(content, sizePx, sizePx, inverted = true)

        assertNotNull(bitmap)
        assertEquals(sizePx, bitmap.width)
        assertEquals(sizePx, bitmap.height)

        var hasWhite = false
        var hasBlack = false
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel == Color.WHITE) hasWhite = true
                if (pixel == Color.BLACK) hasBlack = true
            }
        }
        assertTrue("Inverted QR code must contain white foreground modules", hasWhite)
        assertTrue("Inverted QR code must contain black background modules", hasBlack)
    }

    @Test
    fun testGenerateImageBitmap() {
        val content = "https://example.com"
        val sizePx = 200
        val imageBitmap = WearQrCodeGenerator.generate(content, sizePx)

        assertNotNull(imageBitmap)
        assertEquals(sizePx, imageBitmap.width)
        assertEquals(sizePx, imageBitmap.height)
    }
}
