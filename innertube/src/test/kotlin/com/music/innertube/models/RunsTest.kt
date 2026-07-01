package com.music.innertube.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunsTest {
    @Test
    fun extractCountTextReadsEnglishSplitRuns() {
        assertEquals("653K", runs("653K", " subscribers").extractCountText())
    }

    @Test
    fun extractCountTextReadsJapaneseLabelBeforeCount() {
        assertEquals("653K", runs(CHANNEL_SUBSCRIBER_COUNT, "653K").extractCountText())
    }

    @Test
    fun extractCountTextReadsJapaneseMonthlyLabelBeforeCount() {
        assertEquals("12.2M", runs("$MONTHLY_LISTENER_COUNT: ", "12.2M").extractCountText())
    }

    @Test
    fun extractCountTextReadsSingleRunWithLabel() {
        assertEquals("653K", runs("653K subscribers").extractCountText())
    }

    @Test
    fun extractCountTextKeepsJapaneseCompactUnit() {
        assertEquals("123\u4e07", runs("$MONTHLY_LISTENER_COUNT 123\u4e07").extractCountText())
    }

    @Test
    fun extractCountTextReturnsNullForLabelOnly() {
        assertNull(runs(CHANNEL_SUBSCRIBER_COUNT).extractCountText())
    }

    private fun runs(vararg texts: String) =
        Runs(texts.map { Run(text = it, navigationEndpoint = null) })

    private companion object {
        private const val CHANNEL_SUBSCRIBER_COUNT =
            "\u30c1\u30e3\u30f3\u30cd\u30eb\u767b\u9332\u8005\u6570"
        private const val MONTHLY_LISTENER_COUNT =
            "\u6708\u9593\u8996\u8074\u8005\u6570"
    }
}
