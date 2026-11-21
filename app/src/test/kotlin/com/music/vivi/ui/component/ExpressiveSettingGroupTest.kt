package com.music.vivi.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressiveSettingGroupTest {
    @Test
    fun allRowsVisibleKeepTheirPlainIndex() {
        val visibility = listOf(true, true, true)

        assertEquals(0, visibleIndexOf(visibility, 0))
        assertEquals(2, visibleIndexOf(visibility, 2))
    }

    @Test
    fun rowsAfterAHiddenRowMoveUpOne() {
        val visibility = listOf(true, false, true, true)

        assertEquals(1, visibleIndexOf(visibility, 2))
        assertEquals(2, visibleIndexOf(visibility, 3))
    }

    @Test
    fun hiddenRowReportsTheVisibleRowsBeforeIt() {
        val visibility = listOf(true, true, false)

        assertEquals(2, visibleIndexOf(visibility, 2))
    }

    @Test
    fun firstRowIsAlwaysSlotZero() {
        assertEquals(0, visibleIndexOf(listOf(false, true), 0))
    }

    @Test
    fun emptyListHasNoVisibleIndex() {
        assertEquals(0, visibleIndexOf(emptyList(), 0))
    }

    @Test
    fun indexBeyondListCountsEveryVisibleRow() {
        assertEquals(2, visibleIndexOf(listOf(true, false, true), 10))
    }
}
