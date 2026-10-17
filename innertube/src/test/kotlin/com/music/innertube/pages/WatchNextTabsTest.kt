package com.music.innertube.pages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchNextTabsTest {
    @Test
    fun relatedTabIsFoundByPrefixWhenAllTabsArePresent() {
        val ids = listOf("VLup", "MPLYt_abc", "MPTRt_xyz")

        assertEquals(2, watchNextTabIndex(ids, "MPTR", fallbackIndex = 2))
    }

    @Test
    fun relatedTabIsFoundWhenLyricsTabIsMissing() {
        val ids = listOf("VLup", "MPTRt_xyz")

        assertEquals(1, watchNextTabIndex(ids, "MPTR", fallbackIndex = 2))
    }

    @Test
    fun lyricsTabIsNotMistakenForRelatedTab() {
        val ids = listOf("VLup", "MPTRt_xyz")

        assertNull(watchNextTabIndex(ids, "MPLY", fallbackIndex = 9))
    }

    @Test
    fun fallsBackToIndexWhenNoPrefixMatches() {
        val ids = listOf("VLup", "UNKNOWN", "OTHER")

        assertEquals(2, watchNextTabIndex(ids, "MPTR", fallbackIndex = 2))
    }

    @Test
    fun missingBrowseIdsAreSkipped() {
        val ids = listOf(null, "MPTRt_xyz")

        assertEquals(1, watchNextTabIndex(ids, "MPTR", fallbackIndex = 0))
    }

    @Test
    fun noTabsGiveNoIndex() {
        assertNull(watchNextTabIndex(emptyList(), "MPTR", fallbackIndex = 2))
    }

    @Test
    fun fallbackOutsideTheListGivesNoIndex() {
        assertNull(watchNextTabIndex(listOf("VLup"), "MPTR", fallbackIndex = 2))
    }
}
