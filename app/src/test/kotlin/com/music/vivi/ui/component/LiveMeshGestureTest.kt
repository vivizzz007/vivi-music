package com.music.vivi.ui.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMeshGestureTest {
    private val slop = 8f

    @Test
    fun stationaryQuickPressIsTap() {
        assertTrue(isTapGesture(distancePx = 0f, durationMs = 80L, touchSlopPx = slop))
    }

    @Test
    fun movementInsideSlopStillCountsAsTap() {
        assertTrue(isTapGesture(distancePx = slop - 0.5f, durationMs = 120L, touchSlopPx = slop))
    }

    @Test
    fun movementAtOrBeyondSlopIsNotTap() {
        assertFalse(isTapGesture(distancePx = slop, durationMs = 120L, touchSlopPx = slop))
        assertFalse(isTapGesture(distancePx = slop + 40f, durationMs = 120L, touchSlopPx = slop))
    }

    @Test
    fun pressLongerThanMaxDurationIsNotTap() {
        assertFalse(isTapGesture(distancePx = 0f, durationMs = TAP_MAX_DURATION_MS, touchSlopPx = slop))
        assertFalse(isTapGesture(distancePx = 0f, durationMs = TAP_MAX_DURATION_MS + 500L, touchSlopPx = slop))
    }

    @Test
    fun pressJustUnderMaxDurationIsTap() {
        assertTrue(isTapGesture(distancePx = 0f, durationMs = TAP_MAX_DURATION_MS - 1L, touchSlopPx = slop))
    }

    @Test
    fun zeroSlopRejectsEveryPress() {
        assertFalse(isTapGesture(distancePx = 0f, durationMs = 50L, touchSlopPx = 0f))
    }
}
