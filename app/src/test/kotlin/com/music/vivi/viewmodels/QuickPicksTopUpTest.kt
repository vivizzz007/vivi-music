package com.music.vivi.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPicksTopUpTest {
    @Test
    fun emptyLocalPicksNeedTopUp() {
        assertTrue(needsQuickPicksTopUp(0))
    }

    @Test
    fun singleForgottenFavouriteStillNeedsTopUp() {
        assertTrue(needsQuickPicksTopUp(1))
    }

    @Test
    fun picksAtThresholdDoNotNeedTopUp() {
        assertFalse(needsQuickPicksTopUp(8))
        assertFalse(needsQuickPicksTopUp(20))
    }

    @Test
    fun mergeKeepsCurrentPicksFirst() {
        assertEquals(listOf("a", "b", "c"), mergeQuickPicks(listOf("a"), listOf("b", "c")) { it })
    }

    @Test
    fun mergeDropsDuplicatesByKey() {
        assertEquals(listOf("a", "b"), mergeQuickPicks(listOf("a", "b"), listOf("b", "a")) { it })
    }

    @Test
    fun mergeCapsResultAtTwentyPicks() {
        val merged = mergeQuickPicks((1..15).toList(), (16..30).toList()) { it }

        assertEquals(20, merged.size)
        assertEquals((1..20).toList(), merged)
    }

    @Test
    fun mergeWithEmptyExtraKeepsCurrent() {
        assertEquals(listOf("a"), mergeQuickPicks(listOf("a"), emptyList<String>()) { it })
    }
}
