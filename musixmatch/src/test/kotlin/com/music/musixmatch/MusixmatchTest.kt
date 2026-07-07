/**
 * VIVI-LYRICS (C) 2026
 *
 * PROPRIETARY LICENSE:
 * This file is source-available for viewing. Copying, modification,
 * redistribution, or reuse in other applications is strictly prohibited.
 * Licensed exclusively for use in the official vivimusic application.
 */

package com.music.musixmatch

import com.music.musixmatch.models.RichSyncEntry
import com.music.musixmatch.models.WordEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class MusixmatchTest {

    @Test
    fun testConvertRichSyncToLrc() {
        val entries = listOf(
            RichSyncEntry(
                ts = 32.24,
                te = 34.033,
                l = listOf(
                    WordEntry("This", 0.0),
                    WordEntry(" ", 0.391),
                    WordEntry("is", 0.436),
                    WordEntry(" ", 0.559),
                    WordEntry("the", 0.627),
                    WordEntry("end", 1.287)
                ),
                x = "This is the end"
            )
        )

        val lrc = Musixmatch.convertRichSyncToLrc(entries)
        
        // Expected formatted LRC (inline RichSync):
        // [00:32.240]<00:32.240>This <00:32.676>is <00:32.867>the <00:33.527>end
        val expected = "[00:32.240]<00:32.240>This <00:32.676>is <00:32.867>the <00:33.527>end\n"
        assertEquals(expected, lrc)
    }
}
