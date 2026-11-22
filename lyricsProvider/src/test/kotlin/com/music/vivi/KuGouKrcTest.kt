/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi

import com.music.kugou.KuGou
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KuGouKrcTest {

    @Test
    fun testKrcToWordSyncLrcConversion() {
        val sampleKrc = """
            [id:$00000000]
            [ar:Ariana Grande]
            [ti:Love Me Harder]
            [by:]
            [hash:b02e490ad60e33ac978b614afd7ea8c8]
            [al:My Everything]
            [sign:]
            [qq:]
            [total:236146]
            [offset:0]
            [100,500]<0,500,0>作词：Max Martin
            [3510,4530]<0,490,0>Tell <490,250,0>me <740,780,0>something <1520,200,0>I <1720,400,0>need <2120,400,0>to <2520,600,0>know
            [8040,4990]<0,300,0>Then <300,600,0>take <900,200,0>my <1100,700,0>breath <1800,1100,0>and <2900,300,0>never <3200,200,0>let <3400,200,0>it <3600,200,0>go
        """.trimIndent()

        val lrc = KuGou.krcToWordSyncLrc(sampleKrc)
        assertNotNull(lrc)
        // Check line timestamps
        assertTrue(lrc!!.contains("[00:03.51]Tell me something I need to know"))
        // Check word timestamps and range
        assertTrue(lrc.contains("<Tell :3.510:4.000|me :4.000:4.250|something :4.250:5.030|I :5.030:5.230|need :5.230:5.630|to :5.630:6.030|know:6.030:6.630>"))
        assertTrue(lrc.contains("[00:08.04]Then take my breath and never let it go"))
        // Check that credit line with BANNED_REGEX was filtered out
        assertTrue(!lrc.contains("作词"))
    }

    @Test
    fun testEmptyOrInvalidKrcReturnsNull() {
        assertNull(KuGou.krcToWordSyncLrc(""))
        assertNull(KuGou.krcToWordSyncLrc("   "))
        assertNull(KuGou.decodeKrc(""))
        assertNull(KuGou.decodeKrc("invalid_not_base64!"))
    }
}

