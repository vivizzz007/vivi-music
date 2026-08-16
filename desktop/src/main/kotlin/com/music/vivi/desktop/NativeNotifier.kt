package com.music.vivi.desktop

import java.awt.Color
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * Best-effort native OS notification via `java.awt.SystemTray` (balloon/toast).
 * Works on Windows (action-center style balloon), most Linux desktops, and
 * macOS (Notification Center). Every call is guarded — on unsupported systems
 * it silently no-ops, and the in-app fallback stays available.
 */
object NativeNotifier {

    /** Shows a native system notification with [title] and [message]. */
    fun notify(title: String, message: String) {
        runCatching {
            if (!SystemTray.isSupported()) return
            val tray = SystemTray.getSystemTray()
            val icon = TrayIcon(trayImage(), "VIVI Music")
            icon.isImageAutoSize = true
            tray.add(icon)
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
            // Remove the temporary tray icon shortly after the balloon shows.
            Thread {
                Thread.sleep(10_000)
                runCatching { tray.remove(icon) }
            }.apply { isDaemon = true; name = "vivimusic-notify"; start() }
        }
    }

    /** Small square glyph used as the tray icon (no bundled resource needed). */
    private fun trayImage(): BufferedImage {
        val size = 32
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = Color(0xED, 0x55, 0x64) // VIVI accent
        g.fillRoundRect(0, 0, size, size, 10, 10)
        g.color = Color.WHITE
        g.font = g.font.deriveFont(18f).deriveFont(java.awt.Font.BOLD)
        g.drawString("♪", 8, 23)
        g.dispose()
        return img
    }
}
