/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Generates ZXing QR code bitmaps for Wear Compose display and phone pairing.
 */
object WearQrCodeGenerator {

    /**
     * Generates a monochrome Android [Bitmap] containing the encoded QR code.
     *
     * @param content Text or URL to encode.
     * @param width Width in pixels.
     * @param height Height in pixels.
     * @param inverted If true, renders white QR modules on black background (AMOLED style).
     *                 If false, renders standard black QR modules on white background (maximum phone camera scan compatibility).
     * @return Monochrome Android [Bitmap].
     */
    fun generateBitmap(
        content: String,
        width: Int = 280,
        height: Int = width,
        inverted: Boolean = false,
    ): Bitmap {
        require(width > 0 && height > 0) { "Dimensions must be positive" }
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val onColor = if (inverted) Color.WHITE else Color.BLACK
        val offColor = if (inverted) Color.BLACK else Color.WHITE

        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (matrix[x, y]) onColor else offColor)
            }
        }
        return bmp
    }

    /**
     * Generates a Compose [ImageBitmap] ready to render directly inside Compose Image.
     *
     * @param content Text or URL to encode.
     * @param sizePx Dimensions in pixels (square).
     * @param inverted If true, renders white on black (AMOLED). Defaults to false (black on white)
     *                 for optimal optical scan performance from phone cameras.
     */
    fun generate(content: String, sizePx: Int = 280, inverted: Boolean = false): ImageBitmap {
        return generateBitmap(content, sizePx, sizePx, inverted).asImageBitmap()
    }
}
