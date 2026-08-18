package com.music.vivi.desktop

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

/**
 * Installs a global uncaught-exception handler that shows a dialog with both a
 * "Copy error" action (copies the full message + stack trace to the clipboard)
 * and "OK", instead of the default AWT "Error" dialog that only offers OK.
 * Called once at startup, before the Compose window is created.
 */
fun installGlobalErrorDialog() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { showErrorDialog(throwable) }
        previous?.uncaughtException(thread, throwable)
    }
}

private fun showErrorDialog(throwable: Throwable) {
    val full = fullStack(throwable)
    val run = {
        val area = JTextArea(full)
        area.isEditable = false
        area.lineWrap = false
        val scroll = JScrollPane(area)
        scroll.preferredSize = Dimension(560, 220)

        val panel = JPanel(BorderLayout(0, 8))
        panel.add(JLabel(shortMessage(throwable)), BorderLayout.NORTH)
        panel.add(scroll, BorderLayout.CENTER)

        val options = arrayOf("Copy error", "OK")
        val choice = JOptionPane.showOptionDialog(
            null,
            panel,
            "Error",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            options,
            options[1],
        )
        if (choice == 0) {
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard
                    .setContents(StringSelection(full), null)
            }
        }
    }
    if (SwingUtilities.isEventDispatchThread()) run() else SwingUtilities.invokeLater(run)
}

private fun shortMessage(t: Throwable): String =
    t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName ?: "Error"

/** Full stack trace, capped so the dialog never becomes enormous. */
private fun fullStack(t: Throwable): String {
    val sw = StringWriter()
    t.printStackTrace(PrintWriter(sw))
    return sw.toString().take(8000)
}
