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
        
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        for (window in windows) {
            val root = window.root
            if (root == null) continue
            
            val pkgName = root.packageName?.toString()
            if (pkgName == SYSTEM_UI_PACKAGE) {
                val outBounds = android.graphics.Rect()
                window.getBoundsInScreen(outBounds)

                val windowWidth = outBounds.width()
                val windowHeight = outBounds.height()
                val windowTop = outBounds.top

                // Improved heuristic for Notification Shade / QS:
                // 1. Must be full width
                // 2. Must start from the top of the screen (y=0)
                // 3. Must exceed half the screen height
                if (windowWidth >= screenWidth && windowTop == 0 && windowHeight > screenHeight / 2) {
                    isQsOpen = true
                    root.recycle()
                    break
                }
            }
            root.recycle()
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
