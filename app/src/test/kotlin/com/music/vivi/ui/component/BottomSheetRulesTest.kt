package com.music.vivi.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomSheetRulesTest {
    @Test
    fun dismissedPlayerRevealsWhenATrackStarts() {
        assertEquals(true, shouldRevealPlayerForTrack(hasTrack = true, isDismissed = true))
    }

    @Test
    fun visiblePlayerIsNotRevealedAgain() {
        assertEquals(false, shouldRevealPlayerForTrack(hasTrack = true, isDismissed = false))
    }

    @Test
    fun emptyQueueNeverRevealsThePlayer() {
        assertEquals(false, shouldRevealPlayerForTrack(hasTrack = false, isDismissed = true))
    }

    @Test
    fun emptyQueueAndVisiblePlayerDoNothing() {
        assertEquals(false, shouldRevealPlayerForTrack(hasTrack = false, isDismissed = false))
    }
}
