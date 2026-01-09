package com.music.vivi.livemedia

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class QSStateListenerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val windows = windows
        var isQsOpen = false

        for (window in windows) {
            if (window.root == null) continue
            val pkgName = window.root?.packageName?.toString()
            if (pkgName == SYSTEM_UI_PACKAGE) {
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels

                val outBounds = android.graphics.Rect()
                window.getBoundsInScreen(outBounds)

                val windowHeight = outBounds.height()

                if (windowHeight > screenHeight / 2) {
                    isQsOpen = true
                }
                break
            }
        }

        if (isQsOpen) {
            Log.d(TAG, "🔽 Quick Settings or Notification Shade opened")
        } else {
            Log.d(TAG, "🔼 Quick Settings closed")
        }
        QSStateProvider.updateQsState(isQsOpen)
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "QSStateListenerService"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
